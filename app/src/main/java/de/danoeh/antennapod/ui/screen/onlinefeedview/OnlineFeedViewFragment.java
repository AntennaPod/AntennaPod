package de.danoeh.antennapod.ui.screen.onlinefeedview;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.databinding.OnlinefeedviewFragmentBinding;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadRequest;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
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
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.cleaner.HtmlToPlainText;
import de.danoeh.antennapod.ui.glide.FastBlurTransformation;
import de.danoeh.antennapod.ui.preferences.screen.synchronization.AuthenticationDialog;
import de.danoeh.antennapod.ui.screen.download.DownloadErrorLabel;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter.ARG_FEEDURL;
import static de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter.ARG_STARTED_FROM_SEARCH;
import static de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter.ARG_WAS_MANUAL_URL;

public class OnlineFeedViewFragment extends Fragment {

    static final String TAG = "OnlineFeedViewActivity";
    private static final String PREFS = "OnlineFeedViewActivityPreferences";
    private static final String PREF_LAST_AUTO_DOWNLOAD = "lastAutoDownload";

    private volatile List<Feed> feeds;
    private String selectedDownloadUrl;
    private Downloader downloader;
    private String username = null;
    private String password = null;
    private boolean isFeedFoundBySearch = false;
    private Dialog dialog;
    private Disposable download;
    private Disposable parser;
    private OnlinefeedviewFragmentBinding viewBinding;

    public static OnlineFeedViewFragment newInstance(String url, boolean startedFromSearch, boolean wasManualUrl) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_FEEDURL, url);
        arguments.putBoolean(ARG_STARTED_FROM_SEARCH, startedFromSearch);
        arguments.putBoolean(ARG_WAS_MANUAL_URL, wasManualUrl);
        OnlineFeedViewFragment fragment = new OnlineFeedViewFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewBinding = OnlinefeedviewFragmentBinding.inflate(getLayoutInflater());
        viewBinding.closeButton.setOnClickListener(view -> getActivity().finish());
        setLoadingLayout();
        if (savedInstanceState != null) {
            username = savedInstanceState.getString("username");
            password = savedInstanceState.getString("password");
        }
        lookupUrlAndDownload(getArguments().getString(ARG_FEEDURL));
        return viewBinding.getRoot();
    }

    private void showNoPodcastFoundError() {
        getActivity().runOnUiThread(() -> new MaterialAlertDialogBuilder(getContext())
                .setNeutralButton(android.R.string.ok, (dialog, which) -> getActivity().finish())
                .setTitle(R.string.error_label)
                .setMessage(R.string.null_value_podcast_error)
                .show());
    }

    /**
     * Displays a progress indicator.
     */
    private void setLoadingLayout() {
        viewBinding.progressBar.setVisibility(View.VISIBLE);
        viewBinding.feedDisplayContainer.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (downloader != null && !downloader.isFinished()) {
            downloader.cancel();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (download != null) {
            download.dispose();
        }
        if (parser != null) {
            parser.dispose();
        }
        viewBinding = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("username", username);
        outState.putString("password", password);
    }

    private void lookupUrlAndDownload(String url) {
        download = PodcastSearcherRegistry.lookupUrl(url)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::startFeedDownload,
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
            startFeedDownload(url);
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

    private void startFeedDownload(String url) {
        Log.d(TAG, "Starting feed download");
        selectedDownloadUrl = UrlChecker.prepareUrl(url);
        DownloadRequest request = DownloadRequestCreator.create(new Feed(selectedDownloadUrl, null))
                .withAuthentication(username, password)
                .withInitiatedByUser(true)
                .build();

        download = Observable.fromCallable(() -> {
            long start = System.currentTimeMillis();
            feeds = DBReader.getFeedList();
            boolean isInDatabase = findFeedInSubscriptions() != null;
            if (isInDatabase) {
                viewBinding.stateDeleteNotSubscribedLabel.post(() ->
                        viewBinding.stateDeleteNotSubscribedLabel.setVisibility(View.VISIBLE));
            }
            downloader = new HttpDownloader(request);
            downloader.call();
            long end = System.currentTimeMillis();
            if (isInDatabase && end < start + 4000) {
                Thread.sleep(4000 - (end - start)); // To nudge people into subscribing
            }
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
            if (username != null && password != null) {
                Toast.makeText(getContext(), R.string.download_error_unauthorized, Toast.LENGTH_LONG).show();
            }
            dialog = new FeedViewAuthenticationDialog(getContext(),
                    R.string.authentication_notification_title,
                    downloader.getDownloadRequest().getSource()).create();
            dialog.show();
        } else {
            showErrorDialog(getString(DownloadErrorLabel.from(status.getReason())), status.getReasonDetailed());
        }
    }

    private void parseFeed(String destination) {
        Log.d(TAG, "Parsing feed");
        parser = Maybe.fromCallable(() -> doParseFeed(destination))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableMaybeObserver<FeedHandlerResult>() {
                    @Override
                    public void onSuccess(@NonNull FeedHandlerResult result) {
                        showFeedInformation(result.feed, result.alternateFeedUrls);
                    }

                    @Override
                    public void onComplete() {
                        // Ignore null result: We showed the discovery dialog.
                    }

                    @Override
                    public void onError(@NonNull Throwable error) {
                        showErrorDialog(error.getMessage(), "");
                        Log.d(TAG, "Feed parser exception: " + Log.getStackTraceString(error));
                    }
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
                    return null; // Should not display an error message
                } else {
                    throw new UnsupportedFeedtypeException(getString(R.string.download_error_unsupported_type_html));
                }
            } else {
                throw e;
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw e;
        } finally {
            boolean rc = destinationFile.delete();
            Log.d(TAG, "Deleted feed source file. Result: " + rc);
        }
    }

    /**
     * Called when feed parsed successfully.
     * This method is executed on the GUI thread.
     */
    private void showFeedInformation(final Feed feed, Map<String, String> alternateFeedUrls) {
        viewBinding.progressBar.setVisibility(View.GONE);
        viewBinding.stateDeleteNotSubscribedLabel.setVisibility(View.GONE);
        viewBinding.feedDisplayContainer.setVisibility(View.VISIBLE);
        if (isFeedFoundBySearch) {
            int resId = R.string.no_feed_url_podcast_found_by_search;
            Snackbar.make(viewBinding.getRoot(), resId, Snackbar.LENGTH_LONG).show();
        }

        viewBinding.backgroundImage.setColorFilter(new LightingColorFilter(0xff828282, 0x000000));

        if (StringUtils.isNotBlank(feed.getImageUrl())) {
            Glide.with(this)
                    .load(feed.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.color.light_gray)
                            .error(R.color.light_gray)
                            .fitCenter()
                            .dontAnimate())
                    .into(viewBinding.coverImage);
            Glide.with(this)
                    .load(feed.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.color.image_readability_tint)
                            .error(R.color.image_readability_tint)
                            .transform(new FastBlurTransformation())
                            .dontAnimate())
                    .into(viewBinding.backgroundImage);
        }

        viewBinding.titleLabel.setText(feed.getTitle());
        viewBinding.authorLabel.setText(feed.getAuthor());
        viewBinding.txtvDescription.setText(HtmlToPlainText.getPlainText(feed.getDescription()));

        Feed feedInSubscriptions = findFeedInSubscriptions();
        if (feedInSubscriptions != null) {
            if (feedInSubscriptions.getState() == Feed.STATE_SUBSCRIBED) {
                viewBinding.subscribeButton.setVisibility(View.GONE);
                viewBinding.previewEpisodesButton.setVisibility(View.GONE);
                viewBinding.openPodcastButton.setVisibility(View.VISIBLE);
                viewBinding.openPodcastButton.setOnClickListener(view -> openFeed(feedInSubscriptions.getId()));
            } else {
                viewBinding.openPodcastButton.setVisibility(View.GONE);
                viewBinding.subscribeButton.setVisibility(View.VISIBLE);
                viewBinding.subscribeButton.setOnClickListener(view -> {
                    setLoadingLayout();
                    DBWriter.setFeedState(getContext(), feedInSubscriptions, Feed.STATE_SUBSCRIBED);
                    openFeed(feedInSubscriptions.getId());
                });
                viewBinding.previewEpisodesButton.setVisibility(View.VISIBLE);
                viewBinding.previewEpisodesButton.setOnClickListener(view -> {
                    setLoadingLayout();
                    feed.setId(feedInSubscriptions.getId());
                    FeedDatabaseWriter.updateFeed(getContext(), feed, false);
                    previewFeed(feedInSubscriptions.getId());
                });
            }
        } else {
            viewBinding.openPodcastButton.setVisibility(View.GONE);
            viewBinding.subscribeButton.setVisibility(View.VISIBLE);
            viewBinding.previewEpisodesButton.setVisibility(View.VISIBLE);

            viewBinding.previewEpisodesButton.setOnClickListener(v -> {
                setLoadingLayout();
                feed.setState(Feed.STATE_NOT_SUBSCRIBED);
                FeedDatabaseWriter.updateFeed(getContext(), feed, false);
                Feed feedFromDb = DBReader.getFeed(feed.getId(), false, 0, Integer.MAX_VALUE);
                feedFromDb.getPreferences().setKeepUpdated(false);
                DBWriter.setFeedPreferences(feedFromDb.getPreferences());
                previewFeed(feedFromDb.getId());
            });
            viewBinding.subscribeButton.setOnClickListener(v -> {
                setLoadingLayout();
                Feed feedFromDb = FeedDatabaseWriter.updateFeed(getContext(), feed, false);
                setAutodownloadPreferenceAndOpen(feedFromDb);
            });
            if (UserPreferences.isEnableAutodownload()) {
                viewBinding.autoDownloadCheckBox.setVisibility(View.VISIBLE);
            }
        }

        if (UserPreferences.isEnableAutodownload()) {
            SharedPreferences preferences = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            viewBinding.autoDownloadCheckBox.setChecked(preferences.getBoolean(PREF_LAST_AUTO_DOWNLOAD, true));
        }
        setupAlternateUrls(feed, alternateFeedUrls);
    }

    void setupAlternateUrls(Feed feed, Map<String, String> alternateFeedUrls) {
        if (alternateFeedUrls.isEmpty()) {
            viewBinding.alternateUrlsSpinner.setVisibility(View.GONE);
            return;
        }
        viewBinding.alternateUrlsSpinner.setVisibility(View.VISIBLE);

        final List<String> alternateUrlsList = new ArrayList<>();
        final List<String> alternateUrlsTitleList = new ArrayList<>();

        alternateUrlsList.add(feed.getDownloadUrl());
        alternateUrlsTitleList.add(feed.getTitle());

        alternateUrlsList.addAll(alternateFeedUrls.keySet());
        for (String url : alternateFeedUrls.keySet()) {
            alternateUrlsTitleList.add(alternateFeedUrls.get(url));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                R.layout.alternate_urls_item, alternateUrlsTitleList) {
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                // reusing the old view causes a visual bug on Android <= 10
                return super.getDropDownView(position, null, parent);
            }
        };

        adapter.setDropDownViewResource(R.layout.alternate_urls_dropdown_item);
        viewBinding.alternateUrlsSpinner.setAdapter(adapter);
        viewBinding.alternateUrlsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDownloadUrl = alternateUrlsList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void previewFeed(long id) {
        FeedItemlistFragment fragment = FeedItemlistFragment.newInstance(id);
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment, OnlineFeedViewFragment.TAG)
                .addToBackStack(TAG)
                .commitAllowingStateLoss();
    }

    private void openFeed(long id) {
        MainActivityStarter mainActivityStarter = new MainActivityStarter(getContext());
        mainActivityStarter.withOpenFeed(id);
        if (getArguments().getBoolean(ARG_STARTED_FROM_SEARCH, false)) {
            mainActivityStarter.withAddToBackStack();
        }
        getActivity().finish();
        startActivity(mainActivityStarter.getIntent());
    }

    private void setAutodownloadPreferenceAndOpen(Feed feed) {
        FeedPreferences feedPreferences = feed.getPreferences();
        if (UserPreferences.isEnableAutodownload()) {
            boolean autoDownload = viewBinding.autoDownloadCheckBox.isChecked();
            feedPreferences.setAutoDownload(autoDownload);

            SharedPreferences preferences = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREF_LAST_AUTO_DOWNLOAD, autoDownload);
            editor.apply();
        }
        if (username != null) {
            feedPreferences.setUsername(username);
            feedPreferences.setPassword(password);
        }
        DBWriter.setFeedPreferences(feedPreferences);
        openFeed(feed.getId());
    }

    private Feed findFeedInSubscriptions() {
        if (feeds == null) {
            return null;
        }
        for (Feed f : feeds) {
            if (f.getDownloadUrl().equals(selectedDownloadUrl)) {
                return f;
            }
        }
        return null;
    }

    @UiThread
    private void showErrorDialog(String errorMsg, String details) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.error_label);
        if (errorMsg != null) {
            String total = errorMsg + "\n\n" + details;
            SpannableString errorMessage = new SpannableString(total);
            errorMessage.setSpan(new ForegroundColorSpan(0x88888888),
                    errorMsg.length(), total.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setMessage(errorMessage);
        } else {
            builder.setMessage(R.string.download_error_error_unknown);
        }
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.cancel());
        if (getArguments().getBoolean(ARG_WAS_MANUAL_URL, false)) {
            builder.setNeutralButton(R.string.edit_url_menu, (dialog, which) -> editUrl());
        }
        builder.setOnCancelListener(dialog -> getActivity().finish());
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = builder.show();
    }

    private void editUrl() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.edit_url_menu);
        final EditTextDialogBinding dialogBinding = EditTextDialogBinding.inflate(getLayoutInflater());
        if (downloader != null) {
            dialogBinding.urlEditText.setText(downloader.getDownloadRequest().getSource());
        }
        builder.setView(dialogBinding.getRoot());
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            setLoadingLayout();
            lookupUrlAndDownload(dialogBinding.urlEditText.getText().toString());
        });
        builder.setNegativeButton(R.string.cancel_label, (dialog1, which) -> dialog1.cancel());
        builder.setOnCancelListener(dialog1 -> {
            getActivity().finish();
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

        final List<String> titles = new ArrayList<>();

        final List<String> urls = new ArrayList<>(urlsMap.keySet());
        for (String url : urls) {
            titles.add(urlsMap.get(url));
        }

        if (urls.size() == 1) {
            // Skip dialog and display the item directly
            getArguments().putString(ARG_FEEDURL, urls.get(0));
            startFeedDownload(urls.get(0));
            return true;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.ellipsize_start_listitem, R.id.txtvTitle, titles);
        DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
            String selectedUrl = urls.get(which);
            dialog.dismiss();
            getArguments().putString(ARG_FEEDURL, selectedUrl);
            startFeedDownload(selectedUrl);
        };

        MaterialAlertDialogBuilder ab = new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.feeds_label)
                .setCancelable(true)
                .setOnCancelListener(dialog -> getActivity().finish())
                .setAdapter(adapter, onClickListener);

        getActivity().runOnUiThread(() -> {
            if (dialog != null && dialog.isShowing()) {
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
            getActivity().finish();
        }

        @Override
        protected void onConfirmed(String username, String password) {
            OnlineFeedViewFragment.this.username = username;
            OnlineFeedViewFragment.this.password = password;
            startFeedDownload(feedUrl);
        }
    }

}
