package de.danoeh.antennapod.ui.screen.onlinefeedview;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.databinding.OnlinefeedviewActivityBinding;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadRequest;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.common.UrlChecker;
import de.danoeh.antennapod.net.discovery.CombinedSearcher;
import de.danoeh.antennapod.net.discovery.FeedUrlNotFoundException;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import de.danoeh.antennapod.net.discovery.PodcastSearcherRegistry;
import de.danoeh.antennapod.net.download.service.feed.remote.Downloader;
import de.danoeh.antennapod.net.download.service.feed.remote.HttpDownloader;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequestCreator;
import de.danoeh.antennapod.parser.feed.FeedHandler;
import de.danoeh.antennapod.parser.feed.FeedHandlerResult;
import de.danoeh.antennapod.parser.feed.UnsupportedFeedtypeException;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.common.ThemeSwitcher;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.preferences.screen.synchronization.AuthenticationDialog;
import de.danoeh.antennapod.ui.screen.download.DownloadErrorLabel;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter.ARG_FEEDURL;
import static de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter.ARG_STARTED_FROM_SEARCH;
import static de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter.ARG_WAS_MANUAL_URL;

/**
 * Downloads a feed from a feed URL and parses it. Subclasses can display the
 * feed object that was parsed. This activity MUST be started with a given URL
 * or an Exception will be thrown.
 * <p/>
 * If the feed cannot be downloaded or parsed, an error dialog will be displayed
 * and the activity will finish as soon as the error dialog is closed.
 */
public class OnlineFeedViewActivity extends AppCompatActivity {
    private static final String TAG = "OnlineFeedViewActivity";
    private String selectedDownloadUrl;
    private Downloader downloader;
    private String username = null;
    private String password = null;
    private boolean isPaused;
    private boolean isFeedFoundBySearch = false;
    private Dialog dialog;
    private Disposable download;
    private Disposable parser;
    private OnlinefeedviewActivityBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeSwitcher.getTranslucentTheme(this));
        super.onCreate(savedInstanceState);

        viewBinding = OnlinefeedviewActivityBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        viewBinding.transparentBackground.setOnClickListener(v -> finish());
        viewBinding.card.setOnClickListener(null);
        viewBinding.card.setCardBackgroundColor(ThemeUtils.getColorFromAttr(this, R.attr.colorSurface));

        String feedUrl = null;
        if (getIntent().hasExtra(ARG_FEEDURL)) {
            feedUrl = getIntent().getStringExtra(ARG_FEEDURL);
        } else if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_SEND)) {
            feedUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        } else if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            feedUrl = getIntent().getDataString();
        }

        if (feedUrl == null || UrlChecker.isDeeplinkWithoutUrl(feedUrl)) {
            Log.e(TAG, "feedUrl is null.");
            showNoPodcastFoundError();
        } else {
            Log.d(TAG, "Activity was started with url " + feedUrl);
            if (savedInstanceState != null) {
                username = savedInstanceState.getString("username");
                password = savedInstanceState.getString("password");
            }
            lookupUrlAndDownload(UrlChecker.prepareUrl(feedUrl));
        }
    }

    private void showNoPodcastFoundError() {
        runOnUiThread(() -> new MaterialAlertDialogBuilder(OnlineFeedViewActivity.this)
                .setNeutralButton(android.R.string.ok, (dialog, which) -> finish())
                .setTitle(R.string.error_label)
                .setMessage(R.string.null_value_podcast_error)
                .setOnDismissListener(dialog1 -> finish())
                .show());
    }

    @Override
    protected void onStart() {
        super.onStart();
        isPaused = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isPaused = true;
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
        outState.putString("username", username);
        outState.putString("password", password);
    }

    private void resetIntent(String url) {
        Intent intent = new Intent();
        intent.putExtra(ARG_FEEDURL, url);
        setIntent(intent);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void lookupUrlAndDownload(String url) {
        download = PodcastSearcherRegistry.lookupUrl(url)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::downloadIfNotAlreadySubscribed,
                        error -> {
                            if (error instanceof FeedUrlNotFoundException) {
                                tryToRetrieveFeedUrlBySearch((FeedUrlNotFoundException) error);
                            } else {
                                showNoPodcastFoundError();
                                Log.e(TAG, Log.getStackTraceString(error));
                            }
                        });
    }

    private void tryToRetrieveFeedUrlBySearch(FeedUrlNotFoundException error) {
        Log.d(TAG, "Unable to retrieve feed url, trying to retrieve feed url from search");
        String url = searchFeedUrlByTrackName(error.getTrackName(), error.getArtistName());
        if (url != null) {
            Log.d(TAG, "Successfully retrieve feed url");
            isFeedFoundBySearch = true;
            downloadIfNotAlreadySubscribed(url);
        } else {
            showNoPodcastFoundError();
            Log.d(TAG, "Failed to retrieve feed url");
        }
    }

    private String searchFeedUrlByTrackName(String trackName, String artistName) {
        CombinedSearcher searcher = new CombinedSearcher();
        String query = trackName + " " + artistName;
        List<PodcastSearchResult> results = searcher.search(query).blockingGet();
        for (PodcastSearchResult result : results) {
            if (result.feedUrl != null && result.author != null
                    && result.author.equalsIgnoreCase(artistName) && result.title.equalsIgnoreCase(trackName)) {
                return result.feedUrl;

            }
        }
        return null;
    }

    private Feed downloadIfNotAlreadySubscribed(String url) {
        download = Maybe.fromCallable(() -> {
            List<Feed> feeds = DBReader.getFeedList();
            for (Feed f : feeds) {
                if (f.getDownloadUrl().equals(url)) {
                    return f;
                }
            }
            return null;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(subscribedFeed -> {
            if (subscribedFeed.getState() == Feed.STATE_SUBSCRIBED) {
                openFeed(subscribedFeed.getId());
            } else {
                showFeedFragment(subscribedFeed.getId());
            }
        }, error -> Log.e(TAG, Log.getStackTraceString(error)), () -> startFeedDownload(url));
        return null;
    }

    private void startFeedDownload(String url) {
        Log.d(TAG, "Starting feed download");
        selectedDownloadUrl = UrlChecker.prepareUrl(url);
        DownloadRequest request = DownloadRequestCreator.create(new Feed(selectedDownloadUrl, null))
                .withAuthentication(username, password)
                .withInitiatedByUser(true)
                .build();

        download = Observable.fromCallable(() -> {
            downloader = new HttpDownloader(request);
            downloader.call();
            return downloader.getResult();
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(status -> checkDownloadResult(status, request.getDestination()),
                error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void checkDownloadResult(@NonNull DownloadResult status, String destination) {
        if (status.isSuccessful()) {
            parseFeed(destination);
        } else if (status.getReason() == DownloadError.ERROR_UNAUTHORIZED) {
            if (!isFinishing() && !isPaused) {
                if (username != null && password != null) {
                    Toast.makeText(this, R.string.download_error_unauthorized, Toast.LENGTH_LONG).show();
                }
                dialog = new FeedViewAuthenticationDialog(OnlineFeedViewActivity.this,
                        R.string.authentication_notification_title,
                        downloader.getDownloadRequest().getSource()).create();
                dialog.show();
            }
        } else {
            showErrorDialog(getString(DownloadErrorLabel.from(status.getReason())), status.getReasonDetailed());
        }
    }

    private void parseFeed(String destination) {
        Log.d(TAG, "Parsing feed");
        parser = Maybe.<Long>create(emitter -> {
            FeedHandlerResult handlerResult = doParseFeed(destination);
            if (handlerResult == null) { // Started another attempt with another url
                emitter.onComplete();
                return;
            }
            Feed feed = handlerResult.feed;
            feed.setState(Feed.STATE_NOT_SUBSCRIBED);
            feed.setLastRefreshAttempt(System.currentTimeMillis());
            FeedDatabaseWriter.updateFeed(this, feed, false);
            Feed feedFromDb = DBReader.getFeed(feed.getId(), false, 0, Integer.MAX_VALUE);
            feedFromDb.getPreferences().setKeepUpdated(false);
            DBWriter.setFeedPreferences(feedFromDb.getPreferences());
            emitter.onSuccess(feed.getId());
        })
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::showFeedFragment, error -> {
            error.printStackTrace();
            if (error instanceof UnsupportedFeedtypeException
                    && "html".equalsIgnoreCase(((UnsupportedFeedtypeException) error).getRootElement())) {
                if (getIntent().getBooleanExtra(ARG_WAS_MANUAL_URL, false)) {
                    showErrorDialog(getString(R.string.download_error_unsupported_type_html_manual),
                            error.getMessage());
                } else {
                    showErrorDialog(getString(R.string.download_error_unsupported_type_html), error.getMessage());
                }
                return;
            }
            showErrorDialog(getString(R.string.download_error_parser_exception), error.getMessage());
        });
    }

    /**
     * Try to parse the feed.
     * @return  The FeedHandlerResult if successful.
     *          Null if unsuccessful but we started another attempt.
     * @throws Exception If unsuccessful but we do not know a resolution.
     */
    @Nullable
    private FeedHandlerResult doParseFeed(String destination) throws Exception {
        FeedHandler handler = new FeedHandler();
        Feed feed = new Feed(selectedDownloadUrl, null);
        feed.setLocalFileUrl(destination);
        File destinationFile = new File(destination);
        try {
            return handler.parseFeed(feed);
        } catch (UnsupportedFeedtypeException e) {
            Log.d(TAG, "Unsupported feed type detected");
            if ("html".equalsIgnoreCase(e.getRootElement())) {
                boolean dialogShown = showFeedDiscoveryDialog(destinationFile, selectedDownloadUrl);
                if (dialogShown) {
                    return null; // We handled the problem
                }
            }
            throw e;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
        } finally {
            boolean rc = destinationFile.delete();
            Log.d(TAG, "Deleted feed source file. Result: " + rc);
        }
    }

    private void showFeedFragment(long id) {
        if (isFeedFoundBySearch) {
            Toast.makeText(this, R.string.no_feed_url_podcast_found_by_search, Toast.LENGTH_LONG).show();
        }

        viewBinding.progressBar.setVisibility(View.GONE);
        FeedItemlistFragment fragment = FeedItemlistFragment.newInstance(id);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment, FeedItemlistFragment.TAG)
                .commitAllowingStateLoss();
    }

    private void openFeed(long feedId) {
        // feed.getId() is always 0, we have to retrieve the id from the feed list from the database
        MainActivityStarter mainActivityStarter = new MainActivityStarter(this);
        mainActivityStarter.withOpenFeed(feedId);
        if (getIntent().getBooleanExtra(ARG_STARTED_FROM_SEARCH, false)) {
            mainActivityStarter.withAddToBackStack();
        }
        finish();
        startActivity(mainActivityStarter.getIntent());
    }

    @UiThread
    private void showErrorDialog(String errorMsg, String details) {
        if (!isFinishing() && !isPaused) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.error_label);
            if (errorMsg != null) {
                SpannableString errorMessage = new SpannableString(getString(
                        R.string.download_log_details_message, errorMsg, details, selectedDownloadUrl));
                errorMessage.setSpan(new ForegroundColorSpan(0x88888888),
                        errorMsg.length(), errorMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setMessage(errorMessage);
            } else {
                builder.setMessage(R.string.download_error_error_unknown);
            }
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.cancel());
            if (getIntent().getBooleanExtra(ARG_WAS_MANUAL_URL, false)) {
                builder.setNeutralButton(R.string.edit_url_menu, (dialog, which) -> editUrl());
            }
            builder.setOnCancelListener(dialog -> {
                finish();
            });
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog = builder.show();
            ((TextView) dialog.findViewById(android.R.id.message)).setTextIsSelectable(true);
        }
    }

    private void editUrl() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.edit_url_menu);
        final EditTextDialogBinding dialogBinding = EditTextDialogBinding.inflate(getLayoutInflater());
        if (downloader != null) {
            dialogBinding.textInput.setText(downloader.getDownloadRequest().getSource());
            dialogBinding.textInput.setHint(R.string.rss_address);
        }
        builder.setView(dialogBinding.getRoot());
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            lookupUrlAndDownload(dialogBinding.textInput.getText().toString());
        });
        builder.setNegativeButton(R.string.cancel_label, (dialog1, which) -> dialog1.cancel());
        builder.setOnCancelListener(dialog1 -> {
            finish();
        });
        builder.show();
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

        final List<String> urls = new ArrayList<>(urlsMap.keySet());
        for (String url : urls) {
            titles.add(urlsMap.get(url));
        }

        if (urls.size() == 1) {
            // Skip dialog and display the item directly
            resetIntent(urls.get(0));
            downloadIfNotAlreadySubscribed(urls.get(0));
            return true;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(OnlineFeedViewActivity.this,
                R.layout.ellipsize_start_listitem, R.id.txtvTitle, titles);
        DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
            String selectedUrl = urls.get(which);
            dialog.dismiss();
            resetIntent(selectedUrl);
            downloadIfNotAlreadySubscribed(selectedUrl);
        };

        MaterialAlertDialogBuilder ab = new MaterialAlertDialogBuilder(OnlineFeedViewActivity.this)
                .setTitle(R.string.subscriptions_label)
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
            super(context, titleRes, true, username, password);
            this.feedUrl = feedUrl;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            finish();
        }

        @Override
        protected void onConfirmed(String username, String password) {
            OnlineFeedViewActivity.this.username = username;
            OnlineFeedViewActivity.this.password = password;
            downloadIfNotAlreadySubscribed(feedUrl);
        }
    }

}
