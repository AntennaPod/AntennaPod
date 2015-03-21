package de.danoeh.antennapod.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.dialog.AuthenticationDialog;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.service.download.HttpDownloader;
import de.danoeh.antennapod.core.syndication.handler.FeedHandler;
import de.danoeh.antennapod.core.syndication.handler.FeedHandlerResult;
import de.danoeh.antennapod.core.syndication.handler.UnsupportedFeedtypeException;
import de.danoeh.antennapod.core.util.DownloadError;
import de.danoeh.antennapod.core.util.FileNameGenerator;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.core.util.URLChecker;
import de.danoeh.antennapod.core.util.syndication.FeedDiscoverer;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Downloads a feed from a feed URL and parses it. Subclasses can display the
 * feed object that was parsed. This activity MUST be started with a given URL
 * or an Exception will be thrown.
 * <p/>
 * If the feed cannot be downloaded or parsed, an error dialog will be displayed
 * and the activity will finish as soon as the error dialog is closed.
 */
public abstract class OnlineFeedViewActivity extends ActionBarActivity {
    private static final String TAG = "OnlineFeedViewActivity";
    public static final String ARG_FEEDURL = "arg.feedurl";

    /**
     * Optional argument: specify a title for the actionbar.
     */
    public static final String ARG_TITLE = "title";

    public static final int RESULT_ERROR = 2;

    private Feed feed;
    private Map<String, String> alternateFeedUrls;
    private Downloader downloader;

    private boolean isPaused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().hasExtra(ARG_TITLE)) {
            getSupportActionBar().setTitle(getIntent().getStringExtra(ARG_TITLE));
        }

        StorageUtils.checkStorageAvailability(this);

        final String feedUrl;
        if (getIntent().hasExtra(ARG_FEEDURL)) {
            feedUrl = getIntent().getStringExtra(ARG_FEEDURL);
        } else if (StringUtils.equals(getIntent().getAction(), Intent.ACTION_SEND)
                || StringUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            feedUrl = (StringUtils.equals(getIntent().getAction(), Intent.ACTION_SEND))
                    ? getIntent().getStringExtra(Intent.EXTRA_TEXT) : getIntent().getDataString();

            getSupportActionBar().setTitle(R.string.add_new_feed_label);
        } else {
            throw new IllegalArgumentException(
                    "Activity must be started with feedurl argument!");
        }

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Activity was started with url " + feedUrl);
        setLoadingLayout();
        if (savedInstanceState == null) {
            startFeedDownload(feedUrl, null, null);
        } else {
            startFeedDownload(feedUrl, savedInstanceState.getString("username"), savedInstanceState.getString("password"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (feed != null && feed.getPreferences() != null) {
            outState.putString("username", feed.getPreferences().getUsername());
            outState.putString("password", feed.getPreferences().getPassword());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (downloader != null && !downloader.isFinished()) {
            downloader.cancel();
        }
    }

    private void resetIntent(String url, String title) {
        Intent intent = new Intent();
        intent.putExtra(ARG_FEEDURL, url);
        intent.putExtra(ARG_TITLE, title);
        setIntent(intent);
    }


    private void onDownloadCompleted(final Downloader downloader) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (BuildConfig.DEBUG) Log.d(TAG, "Download was completed");
                DownloadStatus status = downloader.getResult();
                if (status != null) {
                    if (!status.isCancelled()) {
                        if (status.isSuccessful()) {
                            parseFeed();
                        } else if (status.getReason() == DownloadError.ERROR_UNAUTHORIZED) {
                            if (!isFinishing() && !isPaused) {
                                Dialog dialog = new FeedViewAuthenticationDialog(OnlineFeedViewActivity.this,
                                        R.string.authentication_notification_title, downloader.getDownloadRequest().getSource());
                                dialog.show();
                            }
                        } else {
                            String errorMsg = status.getReason().getErrorString(
                                    OnlineFeedViewActivity.this);
                            if (errorMsg != null
                                    && status.getReasonDetailed() != null) {
                                errorMsg += " ("
                                        + status.getReasonDetailed() + ")";
                            }
                            showErrorDialog(errorMsg);
                        }
                    }
                } else {
                    Log.wtf(TAG,
                            "DownloadStatus returned by Downloader was null");
                    finish();
                }
            }
        });

    }

    private void startFeedDownload(String url, String username, String password) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Starting feed download");
        url = URLChecker.prepareURL(url);
        feed = new Feed(url, new Date());
        if (username != null && password != null) {
            feed.setPreferences(new FeedPreferences(0, false, username, password));
        }
        String fileUrl = new File(getExternalCacheDir(),
                FileNameGenerator.generateFileName(feed.getDownload_url()))
                .toString();
        feed.setFile_url(fileUrl);
        final DownloadRequest request = new DownloadRequest(feed.getFile_url(),
                feed.getDownload_url(), "OnlineFeed", 0, Feed.FEEDFILETYPE_FEED, username, password, true, null);
        downloader = new HttpDownloader(
                request);
        new Thread() {
            @Override
            public void run() {
                loadData();
                downloader.call();
                onDownloadCompleted(downloader);
            }
        }.start();


    }

    /**
     * Displays a progress indicator.
     */
    private void setLoadingLayout() {
        RelativeLayout rl = new RelativeLayout(this);
        RelativeLayout.LayoutParams rlLayoutParams = new RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        ProgressBar pb = new ProgressBar(this);
        pb.setIndeterminate(true);
        RelativeLayout.LayoutParams pbLayoutParams = new RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pbLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        rl.addView(pb, pbLayoutParams);
        addContentView(rl, rlLayoutParams);
    }

    private void parseFeed() {
        if (feed == null || feed.getFile_url() == null && feed.isDownloaded()) {
            throw new IllegalStateException(
                    "feed must be non-null and downloaded when parseFeed is called");
        }

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Parsing feed");

        Thread thread = new Thread() {

            @Override
            public void run() {
                String reasonDetailed = "";
                boolean successful = false;
                FeedHandler handler = new FeedHandler();
                try {
                    FeedHandlerResult result = handler.parseFeed(feed);
                    feed = result.feed;
                    alternateFeedUrls = result.alternateFeedUrls;
                    successful = true;
                } catch (SAXException e) {
                    e.printStackTrace();
                    reasonDetailed = e.getMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                    reasonDetailed = e.getMessage();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                    reasonDetailed = e.getMessage();
                } catch (UnsupportedFeedtypeException e) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Unsupported feed type detected");
                    if (StringUtils.equalsIgnoreCase("html", e.getRootElement())) {
                        if (showFeedDiscoveryDialog(new File(feed.getFile_url()), feed.getDownload_url())) {
                            return;
                        }
                    } else {
                        e.printStackTrace();
                        reasonDetailed = e.getMessage();
                    }
                } finally {
                    boolean rc = new File(feed.getFile_url()).delete();
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Deleted feed source file. Result: " + rc);
                }

                if (successful) {
                    beforeShowFeedInformation(feed, alternateFeedUrls);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showFeedInformation(feed, alternateFeedUrls);
                        }
                    });
                } else {
                    final String errorMsg =
                            DownloadError.ERROR_PARSER_EXCEPTION.getErrorString(
                                    OnlineFeedViewActivity.this)
                                    + " (" + reasonDetailed + ")";
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            showErrorDialog(errorMsg);
                        }
                    });
                }
            }
        };
        thread.start();
    }

    /**
     * Can be used to load data asynchronously.
     */
    protected void loadData() {

    }

    /**
     * Called after the feed has been downloaded and parsed and before showFeedInformation is called.
     * This method is executed on a background thread
     */
    protected void beforeShowFeedInformation(Feed feed, Map<String, String> alternateFeedUrls) {

    }

    /**
     * Called when feed parsed successfully.
     * This method is executed on the GUI thread.
     */
    protected void showFeedInformation(Feed feed, Map<String, String> alternateFeedUrls) {

    }

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
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }
            );
            builder.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    setResult(RESULT_ERROR);
                    finish();
                }
            });
            builder.show();
        }
    }

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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isPaused || isFinishing()) {
                    return;
                }

                final List<String> titles = new ArrayList<String>();
                final List<String> urls = new ArrayList<String>();

                urls.addAll(urlsMap.keySet());
                for (String url : urls) {
                    titles.add(urlsMap.get(url));
                }

                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(OnlineFeedViewActivity.this, R.layout.ellipsize_start_listitem, R.id.txtvTitle, titles);
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedUrl = urls.get(which);
                        dialog.dismiss();
                        resetIntent(selectedUrl, titles.get(which));
                        startFeedDownload(selectedUrl, null, null);
                    }
                };

                AlertDialog.Builder ab = new AlertDialog.Builder(OnlineFeedViewActivity.this)
                        .setTitle(R.string.feeds_label)
                        .setCancelable(true)
                        .setOnCancelListener(new OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        })
                        .setAdapter(adapter, onClickListener);
                ab.show();
            }
        });


        return true;
    }

    private class FeedViewAuthenticationDialog extends AuthenticationDialog {

        private String feedUrl;

        public FeedViewAuthenticationDialog(Context context, int titleRes, String feedUrl) {
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
