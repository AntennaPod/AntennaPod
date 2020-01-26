package de.danoeh.antennapod.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import de.danoeh.antennapod.core.event.FeedListUpdateEvent;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.FeedItemlistDescriptionAdapter;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.glide.FastBlurTransformation;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.service.download.HttpDownloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.syndication.handler.FeedHandler;
import de.danoeh.antennapod.core.syndication.handler.FeedHandlerResult;
import de.danoeh.antennapod.core.syndication.handler.UnsupportedFeedtypeException;
import de.danoeh.antennapod.core.util.DownloadError;
import de.danoeh.antennapod.core.util.FileNameGenerator;
import de.danoeh.antennapod.core.util.Optional;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.core.util.URLChecker;
import de.danoeh.antennapod.core.util.syndication.FeedDiscoverer;
import de.danoeh.antennapod.core.util.syndication.HtmlToPlainText;
import de.danoeh.antennapod.dialog.AuthenticationDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Downloads a feed from a feed URL and parses it. Subclasses can display the
 * feed object that was parsed. This activity MUST be started with a given URL
 * or an Exception will be thrown.
 * <p/>
 * If the feed cannot be downloaded or parsed, an error dialog will be displayed
 * and the activity will finish as soon as the error dialog is closed.
 */
public class OnlineFeedViewActivity extends AppCompatActivity {

    public static final String ARG_FEEDURL = "arg.feedurl";
    // Optional argument: specify a title for the actionbar.
    public static final String ARG_TITLE = "title";
    private static final int RESULT_ERROR = 2;
    private static final String TAG = "OnlineFeedViewActivity";
    private volatile List<Feed> feeds;
    private Feed feed;
    private String selectedDownloadUrl;
    private Downloader downloader;

    private boolean isPaused;
    private boolean didPressSubscribe = false;

    private Dialog dialog;
    private ListView listView;
    private Button subscribeButton;
    private ProgressBar progressBar;

    private Disposable download;
    private Disposable parser;
    private Disposable updater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTranslucentTheme());
        super.onCreate(savedInstanceState);
        StorageUtils.checkStorageAvailability(this);
        setContentView(R.layout.onlinefeedview_activity);
        listView = findViewById(R.id.listview);
        progressBar = findViewById(R.id.progressBar);

        String feedUrl = null;
        if (getIntent().hasExtra(ARG_FEEDURL)) {
            feedUrl = getIntent().getStringExtra(ARG_FEEDURL);
        } else if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_SEND)
                || TextUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            feedUrl = TextUtils.equals(getIntent().getAction(), Intent.ACTION_SEND)
                    ? getIntent().getStringExtra(Intent.EXTRA_TEXT) : getIntent().getDataString();
        }

        if (feedUrl == null) {
            Log.e(TAG, "feedUrl is null.");
            new AlertDialog.Builder(OnlineFeedViewActivity.this).
                    setNeutralButton(android.R.string.ok,
                    (dialog, which) -> finish()).
                    setTitle(R.string.error_label).
                    setMessage(R.string.null_value_podcast_error).create().show();
        } else {
            Log.d(TAG, "Activity was started with url " + feedUrl);
            setLoadingLayout();
            // Remove subscribeonandroid.com from feed URL in order to subscribe to the actual feed URL
            if (feedUrl.contains("subscribeonandroid.com")) {
                feedUrl = feedUrl.replaceFirst("((www.)?(subscribeonandroid.com/))", "");
            }
            if (savedInstanceState == null) {
                startFeedDownload(feedUrl, null, null);
            } else {
                startFeedDownload(feedUrl, savedInstanceState.getString("username"), savedInstanceState.getString("password"));
            }
        }
    }

    /**
     * Displays a progress indicator.
     */
    private void setLoadingLayout() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.feedDisplay).setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        isPaused = false;
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isPaused = true;
        EventBus.getDefault().unregister(this);
        if (downloader != null && !downloader.isFinished()) {
            downloader.cancel();
        }
        if(dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(updater != null) {
            updater.dispose();
        }
        if(download != null) {
            download.dispose();
        }
        if(parser != null) {
            parser.dispose();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (feed != null && feed.getPreferences() != null) {
            outState.putString("username", feed.getPreferences().getUsername());
            outState.putString("password", feed.getPreferences().getPassword());
        }
    }

    private void resetIntent(String url, String title) {
        Intent intent = new Intent();
        intent.putExtra(ARG_FEEDURL, url);
        intent.putExtra(ARG_TITLE, title);
        setIntent(intent);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent destIntent = new Intent(this, MainActivity.class);
                if (NavUtils.shouldUpRecreateTask(this, destIntent)) {
                    startActivity(destIntent);
                } else {
                    NavUtils.navigateUpFromSameTask(this);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startFeedDownload(String url, String username, String password) {
        Log.d(TAG, "Starting feed download");
        url = URLChecker.prepareURL(url);
        feed = new Feed(url, null);
        if (username != null && password != null) {
            feed.setPreferences(new FeedPreferences(0, false, FeedPreferences.AutoDeleteAction.GLOBAL, VolumeAdaptionSetting.OFF, username, password));
        }
        String fileUrl = new File(getExternalCacheDir(),
                FileNameGenerator.generateFileName(feed.getDownload_url())).toString();
        feed.setFile_url(fileUrl);
        final DownloadRequest request = new DownloadRequest(feed.getFile_url(),
                feed.getDownload_url(), "OnlineFeed", 0, Feed.FEEDFILETYPE_FEED, username, password,
                true, null);

        download = Observable.fromCallable(() -> {
                    feeds = DBReader.getFeedList();
                    downloader = new HttpDownloader(request);
                    downloader.call();
                    return downloader.getResult();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::checkDownloadResult,
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void checkDownloadResult(@NonNull DownloadStatus status) {
        if (status.isCancelled()) {
            return;
        }
        if (status.isSuccessful()) {
            parseFeed();
        } else if (status.getReason() == DownloadError.ERROR_UNAUTHORIZED) {
            if (!isFinishing() && !isPaused) {
                dialog = new FeedViewAuthenticationDialog(OnlineFeedViewActivity.this,
                        R.string.authentication_notification_title,
                        downloader.getDownloadRequest().getSource()).create();
                dialog.show();
            }
        } else {
            String errorMsg = status.getReason().getErrorString(OnlineFeedViewActivity.this);
            if (status.getReasonDetailed() != null) {
                errorMsg += " (" + status.getReasonDetailed() + ")";
            }
            showErrorDialog(errorMsg);
        }
    }

    @Subscribe
    public void onFeedListChanged(FeedListUpdateEvent event) {
        updater = Observable.fromCallable(DBReader::getFeedList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        feeds -> {
                            OnlineFeedViewActivity.this.feeds = feeds;
                            handleUpdatedFeedStatus(feed);
                        }, error -> Log.e(TAG, Log.getStackTraceString(error))
                );
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        handleUpdatedFeedStatus(feed);
    }

    private void parseFeed() {
        if (feed == null || (feed.getFile_url() == null && feed.isDownloaded())) {
            throw new IllegalStateException("feed must be non-null and downloaded when parseFeed is called");
        }
        Log.d(TAG, "Parsing feed");

        parser = Observable.fromCallable(this::doParseFeed)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(optionalResult -> {
                    if(optionalResult.isPresent()) {
                        FeedHandlerResult result = optionalResult.get();
                        beforeShowFeedInformation(result.feed);
                        showFeedInformation(result.feed, result.alternateFeedUrls);
                    }
                }, error -> {
                    String errorMsg = DownloadError.ERROR_PARSER_EXCEPTION.getErrorString(
                            OnlineFeedViewActivity.this) + " (" + error.getMessage() + ")";
                    showErrorDialog(errorMsg);
                    Log.d(TAG, "Feed parser exception: " + Log.getStackTraceString(error));
                });
    }

    @NonNull
    private Optional<FeedHandlerResult> doParseFeed() throws Exception {
        FeedHandler handler = new FeedHandler();
        try {
            return Optional.of(handler.parseFeed(feed));
        } catch (UnsupportedFeedtypeException e) {
            Log.d(TAG, "Unsupported feed type detected");
            if ("html".equalsIgnoreCase(e.getRootElement())) {
                boolean dialogShown = showFeedDiscoveryDialog(new File(feed.getFile_url()), feed.getDownload_url());
                if (dialogShown) {
                    return Optional.empty();
                } else {
                    Log.d(TAG, "Supplied feed is an HTML web page that has no references to any feed");
                    throw e;
                }
            } else {
                throw e;
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
        } finally {
            boolean rc = new File(feed.getFile_url()).delete();
            Log.d(TAG, "Deleted feed source file. Result: " + rc);
        }
    }

    /**
     * Called after the feed has been downloaded and parsed and before showFeedInformation is called.
     * This method is executed on a background thread
     */
    private void beforeShowFeedInformation(Feed feed) {
        Log.d(TAG, "Removing HTML from feed description");

        feed.setDescription(HtmlToPlainText.getPlainText(feed.getDescription()));

        Log.d(TAG, "Removing HTML from shownotes");
        if (feed.getItems() != null) {
            for (FeedItem item : feed.getItems()) {
                item.setDescription(HtmlToPlainText.getPlainText(item.getDescription()));
            }
        }
    }

    /**
     * Called when feed parsed successfully.
     * This method is executed on the GUI thread.
     */
    private void showFeedInformation(final Feed feed, Map<String, String> alternateFeedUrls) {
        progressBar.setVisibility(View.GONE);
        findViewById(R.id.feedDisplay).setVisibility(View.VISIBLE);
        this.feed = feed;
        this.selectedDownloadUrl = feed.getDownload_url();
        listView.setSelector(android.R.color.transparent);
        listView.setAdapter(new FeedItemlistDescriptionAdapter(this, 0, feed.getItems()));

        ImageView cover = findViewById(R.id.imgvCover);
        ImageView headerBackground = findViewById(R.id.imgvBackground);
        findViewById(R.id.butShowInfo).setVisibility(View.INVISIBLE);
        findViewById(R.id.butShowSettings).setVisibility(View.INVISIBLE);
        headerBackground.setColorFilter(new LightingColorFilter(0xff828282, 0x000000));
        TextView title = findViewById(R.id.txtvTitle);
        TextView author = findViewById(R.id.txtvAuthor);
        Spinner spAlternateUrls = findViewById(R.id.spinnerAlternateUrls);

        View header = View.inflate(this, R.layout.onlinefeedview_header, null);
        listView.addHeaderView(header);
        TextView description = header.findViewById(R.id.txtvDescription);

        subscribeButton = findViewById(R.id.butSubscribe);

        if (StringUtils.isNotBlank(feed.getImageUrl())) {
            Glide.with(this)
                    .load(feed.getImageUrl())
                    .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate())
                    .into(cover);
            Glide.with(this)
                    .load(feed.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.color.image_readability_tint)
                            .error(R.color.image_readability_tint)
                            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                            .transform(new FastBlurTransformation())
                            .dontAnimate())
                    .into(headerBackground);
        }

        title.setText(feed.getTitle());
        author.setText(feed.getAuthor());
        description.setText(feed.getDescription());

        subscribeButton.setOnClickListener(v -> {
            if (feedInFeedlist(feed)) {
                openFeed();
            } else {
                Feed f = new Feed(selectedDownloadUrl, null, feed.getTitle());
                f.setPreferences(feed.getPreferences());
                this.feed = f;
                try {
                    DownloadRequester.getInstance().downloadFeed(this, f);
                } catch (DownloadRequestException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                    DownloadRequestErrorDialogCreator.newRequestErrorDialog(this, e.getMessage());
                }
                didPressSubscribe = true;
                handleUpdatedFeedStatus(feed);
            }
        });

        final int MAX_LINES_COLLAPSED = 10;
        description.setMaxLines(MAX_LINES_COLLAPSED);
        description.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                    && description.getMaxLines() > MAX_LINES_COLLAPSED) {
                description.setMaxLines(MAX_LINES_COLLAPSED);
            } else {
                description.setMaxLines(2000);
            }
        });

        if (alternateFeedUrls.isEmpty()) {
            spAlternateUrls.setVisibility(View.GONE);
        } else {
            spAlternateUrls.setVisibility(View.VISIBLE);

            final List<String> alternateUrlsList = new ArrayList<>();
            final List<String> alternateUrlsTitleList = new ArrayList<>();

            alternateUrlsList.add(feed.getDownload_url());
            alternateUrlsTitleList.add(feed.getTitle());


            alternateUrlsList.addAll(alternateFeedUrls.keySet());
            for (String url : alternateFeedUrls.keySet()) {
                alternateUrlsTitleList.add(alternateFeedUrls.get(url));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, alternateUrlsTitleList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spAlternateUrls.setAdapter(adapter);
            spAlternateUrls.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedDownloadUrl = alternateUrlsList.get(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
        handleUpdatedFeedStatus(feed);
    }

    private void openFeed() {
        // feed.getId() is always 0, we have to retrieve the id from the feed list from
        // the database
        Intent intent = MainActivity.getIntentToOpenFeed(this, getFeedId(feed));
        finish();
        startActivity(intent);
    }

    private void handleUpdatedFeedStatus(Feed feed) {
        if (subscribeButton != null && feed != null) {
            if (DownloadRequester.getInstance().isDownloadingFile(feed.getDownload_url())) {
                subscribeButton.setEnabled(false);
                subscribeButton.setText(R.string.subscribing_label);
            } else if (feedInFeedlist(feed)) {
                subscribeButton.setEnabled(true);
                subscribeButton.setText(R.string.open_podcast);
                if (didPressSubscribe) {
                    openFeed();
                }
            } else {
                subscribeButton.setEnabled(true);
                subscribeButton.setText(R.string.subscribe_label);
            }
        }
    }

    private boolean feedInFeedlist(Feed feed) {
        if (feeds == null || feed == null) {
            return false;
        }
        for (Feed f : feeds) {
            if (f.getIdentifyingValue().equals(feed.getIdentifyingValue())) {
                return true;
            }
        }
        return false;
    }

    private long getFeedId(Feed feed) {
        if (feeds == null || feed == null) {
            return 0;
        }
        for (Feed f : feeds) {
            if (f.getIdentifyingValue().equals(feed.getIdentifyingValue())) {
                return f.getId();
            }
        }
        return 0;
    }

    @UiThread
    private void showErrorDialog(String errorMsg) {
        if (!isFinishing() && !isPaused) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.error_label);
            if (errorMsg != null) {
                builder.setMessage(getString(R.string.error_msg_prefix) + errorMsg);
            } else {
                builder.setMessage(R.string.error_msg_prefix);
            }
            builder.setNeutralButton(android.R.string.ok,
                    (dialog, which) -> dialog.cancel()
            );
            builder.setOnCancelListener(dialog -> {
                setResult(RESULT_ERROR);
                finish();
            });
            if(dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog = builder.show();
        }
    }

    /**
     *
     * @return true if a FeedDiscoveryDialog is shown, false otherwise (e.g., due to no feed found).
     */
    private boolean showFeedDiscoveryDialog(File feedFile, String baseUrl) {
        FeedDiscoverer fd = new FeedDiscoverer();
        final Map<String, String> urlsMap;
        try {
            urlsMap = fd.findLinks(feedFile, baseUrl);
            if (urlsMap == null || urlsMap.isEmpty()) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (isPaused || isFinishing()) {
            return false;
        }

        final List<String> titles = new ArrayList<>();
        final List<String> urls = new ArrayList<>();

        urls.addAll(urlsMap.keySet());
        for (String url : urls) {
            titles.add(urlsMap.get(url));
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(OnlineFeedViewActivity.this, R.layout.ellipsize_start_listitem, R.id.txtvTitle, titles);
        DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
            String selectedUrl = urls.get(which);
            dialog.dismiss();
            resetIntent(selectedUrl, titles.get(which));
            FeedPreferences prefs = feed.getPreferences();
            if(prefs != null) {
                startFeedDownload(selectedUrl, prefs.getUsername(), prefs.getPassword());
            } else {
                startFeedDownload(selectedUrl, null, null);
            }
        };

        AlertDialog.Builder ab = new AlertDialog.Builder(OnlineFeedViewActivity.this)
                .setTitle(R.string.feeds_label)
                .setCancelable(true)
                .setOnCancelListener(dialog -> finish())
                .setAdapter(adapter, onClickListener);

        runOnUiThread(() -> {
            if(dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog = ab.show();
        });
        return true;
    }

    private class FeedViewAuthenticationDialog extends AuthenticationDialog {

        private final String feedUrl;

        FeedViewAuthenticationDialog(Context context, int titleRes, String feedUrl) {
            super(context, titleRes, true, false, null, null);
            this.feedUrl = feedUrl;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            finish();
        }

        @Override
        protected void onConfirmed(String username, String password, boolean saveUsernamePassword) {
            startFeedDownload(feedUrl, username, password);
        }
    }

}
