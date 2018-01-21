package de.danoeh.antennapod.core.service.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.webkit.URLUtil;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeAction;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeAction.Action;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.GpodnetSyncService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.syndication.handler.FeedHandler;
import de.danoeh.antennapod.core.syndication.handler.FeedHandlerResult;
import de.danoeh.antennapod.core.syndication.handler.UnsupportedFeedtypeException;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.DownloadError;
import de.danoeh.antennapod.core.util.InvalidFeedException;
import de.greenrobot.event.EventBus;

/**
 * Manages the download of feedfiles in the app. Downloads can be enqueued via the startService intent.
 * The argument of the intent is an instance of DownloadRequest in the EXTRA_REQUEST field of
 * the intent.
 * After the downloads have finished, the downloaded object will be passed on to a specific handler, depending on the
 * type of the feedfile.
 */
public class DownloadService extends Service {
    private static final String TAG = "DownloadService";

    /**
     * Cancels one download. The intent MUST have an EXTRA_DOWNLOAD_URL extra that contains the download URL of the
     * object whose download should be cancelled.
     */
    public static final String ACTION_CANCEL_DOWNLOAD = "action.de.danoeh.antennapod.core.service.cancelDownload";

    /**
     * Cancels all running downloads.
     */
    public static final String ACTION_CANCEL_ALL_DOWNLOADS = "action.de.danoeh.antennapod.core.service.cancelAllDownloads";

    /**
     * Extra for ACTION_CANCEL_DOWNLOAD
     */
    public static final String EXTRA_DOWNLOAD_URL = "downloadUrl";

    /**
     * Extra for ACTION_ENQUEUE_DOWNLOAD intent.
     */
    public static final String EXTRA_REQUEST = "request";

    /**
     * Contains all completed downloads that have not been included in the report yet.
     */
    private List<DownloadStatus> reportQueue;

    private ExecutorService syncExecutor;
    private CompletionService<Downloader> downloadExecutor;
    private FeedSyncThread feedSyncThread;

    /**
     * Number of threads of downloadExecutor.
     */
    private static final int NUM_PARALLEL_DOWNLOADS = 6;

    private DownloadRequester requester;


    private NotificationCompat.Builder notificationCompatBuilder;
    private static final int NOTIFICATION_ID = 2;
    private static final int REPORT_ID = 3;

    /**
     * Currently running downloads.
     */
    private List<Downloader> downloads;

    /**
     * Number of running downloads.
     */
    private AtomicInteger numberOfDownloads;

    /**
     * True if service is running.
     */
    public static boolean isRunning = false;

    private Handler handler;

    private NotificationUpdater notificationUpdater;
    private ScheduledFuture<?> notificationUpdaterFuture;
    private static final int SCHED_EX_POOL_SIZE = 1;
    private ScheduledThreadPoolExecutor schedExecutor;

    private final Handler postHandler = new Handler();

    private final IBinder mBinder = new LocalBinder();

    private class LocalBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    private final Thread downloadCompletionThread = new Thread() {
        private static final String TAG = "downloadCompletionThd";

        @Override
        public void run() {
            Log.d(TAG, "downloadCompletionThread was started");
            while (!isInterrupted()) {
                try {
                    Downloader downloader = downloadExecutor.take().get();
                    Log.d(TAG, "Received 'Download Complete' - message.");
                    removeDownload(downloader);
                    DownloadStatus status = downloader.getResult();
                    boolean successful = status.isSuccessful();

                    final int type = status.getFeedfileType();
                    if (successful) {
                        if (type == Feed.FEEDFILETYPE_FEED) {
                            handleCompletedFeedDownload(downloader.getDownloadRequest());
                        } else if (type == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                            handleCompletedFeedMediaDownload(status, downloader.getDownloadRequest());
                        }
                    } else {
                        numberOfDownloads.decrementAndGet();
                        if (!status.isCancelled()) {
                            if (status.getReason() == DownloadError.ERROR_UNAUTHORIZED) {
                                postAuthenticationNotification(downloader.getDownloadRequest());
                            } else if (status.getReason() == DownloadError.ERROR_HTTP_DATA_ERROR
                                    && Integer.parseInt(status.getReasonDetailed()) == 416) {

                                Log.d(TAG, "Requested invalid range, restarting download from the beginning");
                                FileUtils.deleteQuietly(new File(downloader.getDownloadRequest().getDestination()));
                                DownloadRequester.getInstance().download(DownloadService.this, downloader.getDownloadRequest());
                            } else {
                                Log.e(TAG, "Download failed");
                                saveDownloadStatus(status);
                                handleFailedDownload(status, downloader.getDownloadRequest());

                                if (type == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                                    long id = status.getFeedfileId();
                                    FeedMedia media = DBReader.getFeedMedia(id);
                                    FeedItem item;
                                    if (media == null || (item = media.getItem()) == null) {
                                        return;
                                    }
                                    boolean httpNotFound = status.getReason() == DownloadError.ERROR_HTTP_DATA_ERROR
                                            && String.valueOf(HttpURLConnection.HTTP_NOT_FOUND).equals(status.getReasonDetailed());
                                    boolean forbidden = status.getReason() == DownloadError.ERROR_FORBIDDEN
                                            && String.valueOf(HttpURLConnection.HTTP_FORBIDDEN).equals(status.getReasonDetailed());
                                    boolean notEnoughSpace = status.getReason() == DownloadError.ERROR_NOT_ENOUGH_SPACE;
                                    boolean wrongFileType = status.getReason() == DownloadError.ERROR_FILE_TYPE;
                                    if (httpNotFound || forbidden || notEnoughSpace || wrongFileType) {
                                        DBWriter.saveFeedItemAutoDownloadFailed(item).get();
                                    }
                                    // to make lists reload the failed item, we fake an item update
                                    EventBus.getDefault().post(FeedItemEvent.updated(item));
                                }
                            }
                        } else {
                            // if FeedMedia download has been canceled, fake FeedItem update
                            // so that lists reload that it
                            if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                                FeedMedia media = DBReader.getFeedMedia(status.getFeedfileId());
                                FeedItem item;
                                if (media == null || (item = media.getItem()) == null) {
                                    return;
                                }
                                EventBus.getDefault().post(FeedItemEvent.updated(item));
                            }
                        }
                        queryDownloadsAsync();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "DownloadCompletionThread was interrupted");
                } catch (ExecutionException e) {
                    Log.e(TAG, "ExecutionException in DownloadCompletionThread: " + e.getMessage());
                    numberOfDownloads.decrementAndGet();
                }
            }
            Log.d(TAG, "End of downloadCompletionThread");
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getParcelableExtra(EXTRA_REQUEST) != null) {
            onDownloadQueued(intent);
        } else if (numberOfDownloads.get() == 0) {
            stopSelf();
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Service started");
        isRunning = true;
        handler = new Handler();
        reportQueue = Collections.synchronizedList(new ArrayList<>());
        downloads = Collections.synchronizedList(new ArrayList<>());
        numberOfDownloads = new AtomicInteger(0);

        IntentFilter cancelDownloadReceiverFilter = new IntentFilter();
        cancelDownloadReceiverFilter.addAction(ACTION_CANCEL_ALL_DOWNLOADS);
        cancelDownloadReceiverFilter.addAction(ACTION_CANCEL_DOWNLOAD);
        registerReceiver(cancelDownloadReceiver, cancelDownloadReceiverFilter);
        syncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        Log.d(TAG, "parallel downloads: " + UserPreferences.getParallelDownloads());
        downloadExecutor = new ExecutorCompletionService<>(
                Executors.newFixedThreadPool(UserPreferences.getParallelDownloads(),
                        r -> {
                            Thread t = new Thread(r);
                            t.setPriority(Thread.MIN_PRIORITY);
                            return t;
                        }
                )
        );
        schedExecutor = new ScheduledThreadPoolExecutor(SCHED_EX_POOL_SIZE,
                r -> {
                    Thread t = new Thread(r);
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                }, (r, executor) -> Log.w(TAG, "SchedEx rejected submission of new task")
        );
        downloadCompletionThread.start();
        feedSyncThread = new FeedSyncThread();
        feedSyncThread.start();

        setupNotificationBuilders();
        requester = DownloadRequester.getInstance();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service shutting down");
        isRunning = false;

        if (ClientConfig.downloadServiceCallbacks.shouldCreateReport() &&
                UserPreferences.showDownloadReport()) {
            updateReport();
        }

        postHandler.removeCallbacks(postDownloaderTask);
        EventBus.getDefault().postSticky(DownloadEvent.refresh(Collections.emptyList()));

        stopForeground(true);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);

        downloadCompletionThread.interrupt();
        syncExecutor.shutdown();
        schedExecutor.shutdown();
        feedSyncThread.shutdown();
        cancelNotificationUpdater();
        unregisterReceiver(cancelDownloadReceiver);

        // if this was the initial gpodder sync, i.e. we just synced the feeds successfully,
        // it is now time to sync the episode actions
        if (GpodnetPreferences.loggedIn() &&
                GpodnetPreferences.getLastSubscriptionSyncTimestamp() > 0 &&
                GpodnetPreferences.getLastEpisodeActionsSyncTimestamp() == 0) {
            GpodnetSyncService.sendSyncActionsIntent(this);
        }

        // start auto download in case anything new has shown up
        DBTasks.autodownloadUndownloadedItems(getApplicationContext());
    }

    private void setupNotificationBuilders() {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.stat_notify_sync);

        notificationCompatBuilder = new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setContentIntent(ClientConfig.downloadServiceCallbacks.getNotificationContentIntent(this))
                .setLargeIcon(icon)
                .setSmallIcon(R.drawable.stat_notify_sync);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationCompatBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        Log.d(TAG, "Notification set up");
    }

    /**
     * Updates the contents of the service's notifications. Should be called
     * before setupNotificationBuilders.
     */
    private Notification updateNotifications() {
        if (notificationCompatBuilder == null) {
            return null;
        }

        String contentTitle = getString(R.string.download_notification_title);
        int numDownloads = requester.getNumberOfDownloads();
        String downloadsLeft = (numDownloads > 0) ?
                getResources().getQuantityString(R.plurals.downloads_left, numDownloads, numDownloads) :
                getString(R.string.downloads_processing);
        String bigText = compileNotificationString(downloads);

        notificationCompatBuilder.setContentTitle(contentTitle);
        notificationCompatBuilder.setContentText(downloadsLeft);
        notificationCompatBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        return notificationCompatBuilder.build();
    }

    private Downloader getDownloader(String downloadUrl) {
        for (Downloader downloader : downloads) {
            if (downloader.getDownloadRequest().getSource().equals(downloadUrl)) {
                return downloader;
            }
        }
        return null;
    }

    private final BroadcastReceiver cancelDownloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), ACTION_CANCEL_DOWNLOAD)) {
                String url = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
                if (url == null) {
                    throw new IllegalArgumentException("ACTION_CANCEL_DOWNLOAD intent needs download url extra");
                }

                Log.d(TAG, "Cancelling download with url " + url);
                Downloader d = getDownloader(url);
                if (d != null) {
                    d.cancel();
                } else {
                    Log.e(TAG, "Could not cancel download with url " + url);
                }
                postDownloaders();

            } else if (TextUtils.equals(intent.getAction(), ACTION_CANCEL_ALL_DOWNLOADS)) {
                for (Downloader d : downloads) {
                    d.cancel();
                    Log.d(TAG, "Cancelled all downloads");
                }
                postDownloaders();
            }
            queryDownloads();
        }

    };

    private void onDownloadQueued(Intent intent) {
        Log.d(TAG, "Received enqueue request");
        DownloadRequest request = intent.getParcelableExtra(EXTRA_REQUEST);
        if (request == null) {
            throw new IllegalArgumentException(
                    "ACTION_ENQUEUE_DOWNLOAD intent needs request extra");
        }

        writeFileUrl(request);

        Downloader downloader = getDownloader(request);
        if (downloader != null) {
            numberOfDownloads.incrementAndGet();
            // smaller rss feeds before bigger media files
            if (request.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
                downloads.add(0, downloader);
            } else {
                downloads.add(downloader);
            }
            downloadExecutor.submit(downloader);

            postDownloaders();
        }

        queryDownloads();
    }

    private Downloader getDownloader(DownloadRequest request) {
        if (!URLUtil.isHttpUrl(request.getSource()) && !URLUtil.isHttpsUrl(request.getSource())) {
            Log.e(TAG, "Could not find appropriate downloader for " + request.getSource());
            return null;
        }
        return new HttpDownloader(request);
    }

    /**
     * Remove download from the DownloadRequester list and from the
     * DownloadService list.
     */
    private void removeDownload(final Downloader d) {
        handler.post(() -> {
            Log.d(TAG, "Removing downloader: " + d.getDownloadRequest().getSource());
            boolean rc = downloads.remove(d);
            Log.d(TAG, "Result of downloads.remove: " + rc);
            DownloadRequester.getInstance().removeDownload(d.getDownloadRequest());
            postDownloaders();
        });
    }

    /**
     * Adds a new DownloadStatus object to the list of completed downloads and
     * saves it in the database
     *
     * @param status the download that is going to be saved
     */
    private void saveDownloadStatus(DownloadStatus status) {
        reportQueue.add(status);
        DBWriter.addDownloadStatus(status);
    }

    /**
     * Creates a notification at the end of the service lifecycle to notify the
     * user about the number of completed downloads. A report will only be
     * created if there is at least one failed download excluding images
     */
    private void updateReport() {
        // check if report should be created
        boolean createReport = false;
        int successfulDownloads = 0;
        int failedDownloads = 0;

        // a download report is created if at least one download has failed
        // (excluding failed image downloads)
        for (DownloadStatus status : reportQueue) {
            if (status.isSuccessful()) {
                successfulDownloads++;
            } else if (!status.isCancelled()) {
                if (status.getFeedfileType() != FeedImage.FEEDFILETYPE_FEEDIMAGE) {
                    createReport = true;
                }
                failedDownloads++;
            }
        }

        if (createReport) {
            Log.d(TAG, "Creating report");
            // create notification object
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setTicker(getString(R.string.download_report_title))
                    .setContentTitle(getString(R.string.download_report_content_title))
                    .setContentText(
                            String.format(
                                    getString(R.string.download_report_content),
                                    successfulDownloads, failedDownloads)
                    )
                    .setSmallIcon(R.drawable.stat_notify_sync_error)
                    .setLargeIcon(
                            BitmapFactory.decodeResource(getResources(),
                                    R.drawable.stat_notify_sync_error)
                    )
                    .setContentIntent(
                            ClientConfig.downloadServiceCallbacks.getReportNotificationContentIntent(this)
                    )
                    .setAutoCancel(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(REPORT_ID, builder.build());
        } else {
            Log.d(TAG, "No report is created");
        }
        reportQueue.clear();
    }

    /**
     * Calls query downloads on the services main thread. This method should be used instead of queryDownloads if it is
     * used from a thread other than the main thread.
     */
    private void queryDownloadsAsync() {
        handler.post(DownloadService.this::queryDownloads);
    }

    /**
     * Check if there's something else to download, otherwise stop
     */
    private void queryDownloads() {
        Log.d(TAG, numberOfDownloads.get() + " downloads left");

        if (numberOfDownloads.get() <= 0 && DownloadRequester.getInstance().hasNoDownloads()) {
            Log.d(TAG, "Number of downloads is " + numberOfDownloads.get() + ", attempting shutdown");
            stopSelf();
        } else {
            setupNotificationUpdater();
            startForeground(NOTIFICATION_ID, updateNotifications());
        }
    }

    private void postAuthenticationNotification(final DownloadRequest downloadRequest) {
        handler.post(() -> {
            final String resourceTitle = (downloadRequest.getTitle() != null) ?
                    downloadRequest.getTitle() : downloadRequest.getSource();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(DownloadService.this);
            builder.setTicker(getText(R.string.authentication_notification_title))
                    .setContentTitle(getText(R.string.authentication_notification_title))
                    .setContentText(getText(R.string.authentication_notification_msg))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(getText(R.string.authentication_notification_msg)
                            + ": " + resourceTitle))
                    .setSmallIcon(R.drawable.ic_stat_authentication)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_stat_authentication))
                    .setAutoCancel(true)
                    .setContentIntent(ClientConfig.downloadServiceCallbacks.getAuthentificationNotificationContentIntent(DownloadService.this, downloadRequest));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(downloadRequest.getSource().hashCode(), builder.build());
        });
    }

    /**
     * Is called whenever a Feed is downloaded
     */
    private void handleCompletedFeedDownload(DownloadRequest request) {
        Log.d(TAG, "Handling completed Feed Download");
        feedSyncThread.submitCompletedDownload(request);
    }

    /**
     * Is called whenever a FeedMedia is downloaded.
     */
    private void handleCompletedFeedMediaDownload(DownloadStatus status, DownloadRequest request) {
        Log.d(TAG, "Handling completed FeedMedia Download");
        syncExecutor.execute(new MediaHandlerThread(status, request));
    }

    private void handleFailedDownload(DownloadStatus status, DownloadRequest request) {
        Log.d(TAG, "Handling failed download");
        syncExecutor.execute(new FailedDownloadHandler(status, request));
    }

    /**
     * Takes a single Feed, parses the corresponding file and refreshes
     * information in the manager
     */
    private class FeedSyncThread extends Thread {
        private static final String TAG = "FeedSyncThread";

        private final BlockingQueue<DownloadRequest> completedRequests = new LinkedBlockingDeque<>();
        private final CompletionService<Pair<DownloadRequest, FeedHandlerResult>> parserService = new ExecutorCompletionService<>(Executors.newSingleThreadExecutor());
        private final ExecutorService dbService = Executors.newSingleThreadExecutor();
        private Future<?> dbUpdateFuture;
        private volatile boolean isActive = true;
        private volatile boolean isCollectingRequests = false;

        private static final long WAIT_TIMEOUT = 3000;


        /**
         * Waits for completed requests. Once the first request has been taken, the method will wait WAIT_TIMEOUT ms longer to
         * collect more completed requests.
         *
         * @return Collected feeds or null if the method has been interrupted during the first waiting period.
         */
        private List<Pair<DownloadRequest, FeedHandlerResult>> collectCompletedRequests() {
            List<Pair<DownloadRequest, FeedHandlerResult>> results = new LinkedList<>();
            DownloadRequester requester = DownloadRequester.getInstance();
            int tasks = 0;

            try {
                DownloadRequest request = completedRequests.take();
                parserService.submit(new FeedParserTask(request));
                tasks++;
            } catch (InterruptedException e) {
                Log.e(TAG, "FeedSyncThread was interrupted");
                return null;
            }

            tasks += pollCompletedDownloads();

            isCollectingRequests = true;

            if (requester.isDownloadingFeeds()) {
                // wait for completion of more downloads
                long startTime = System.currentTimeMillis();
                long currentTime = startTime;
                while (requester.isDownloadingFeeds() && (currentTime - startTime) < WAIT_TIMEOUT) {
                    try {
                        Log.d(TAG, "Waiting for " + (startTime + WAIT_TIMEOUT - currentTime) + " ms");
                        sleep(startTime + WAIT_TIMEOUT - currentTime);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "interrupted while waiting for more downloads");
                        tasks += pollCompletedDownloads();
                    } finally {
                        currentTime = System.currentTimeMillis();
                    }
                }

                tasks += pollCompletedDownloads();

            }

            isCollectingRequests = false;

            for (int i = 0; i < tasks; i++) {
                try {
                    Pair<DownloadRequest, FeedHandlerResult> result = parserService.take().get();
                    if (result != null) {
                        results.add(result);
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "FeedSyncThread was interrupted");
                } catch (ExecutionException e) {
                    Log.e(TAG, "ExecutionException in FeedSyncThread: " + e.getMessage());
                }
            }

            return results;
        }

        private int pollCompletedDownloads() {
            int tasks = 0;
            while (!completedRequests.isEmpty()) {
                parserService.submit(new FeedParserTask(completedRequests.poll()));
                tasks++;
            }
            return tasks;
        }

        @Override
        public void run() {
            while (isActive) {
                final List<Pair<DownloadRequest, FeedHandlerResult>> results = collectCompletedRequests();

                if (results == null) {
                    continue;
                }

                Log.d(TAG, "Bundling " + results.size() + " feeds");

                for (Pair<DownloadRequest, FeedHandlerResult> result : results) {
                    removeDuplicateImages(result.second.feed); // duplicate images have to removed because the DownloadRequester does not accept two downloads with the same download URL yet.
                }

                // Save information of feed in DB
                if (dbUpdateFuture != null) {
                    try {
                        dbUpdateFuture.get();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "FeedSyncThread was interrupted");
                    } catch (ExecutionException e) {
                        Log.e(TAG, "ExecutionException in FeedSyncThread: " + e.getMessage());
                    }
                }

                dbUpdateFuture = dbService.submit(() -> {
                    Feed[] savedFeeds = DBTasks.updateFeed(DownloadService.this, getFeeds(results));

                    for (int i = 0; i < savedFeeds.length; i++) {
                        Feed savedFeed = savedFeeds[i];

                        // If loadAllPages=true, check if another page is available and queue it for download
                        final boolean loadAllPages = results.get(i).first.getArguments().getBoolean(DownloadRequester.REQUEST_ARG_LOAD_ALL_PAGES);
                        final Feed feed = results.get(i).second.feed;
                        if (loadAllPages && feed.getNextPageLink() != null) {
                            try {
                                feed.setId(savedFeed.getId());
                                DBTasks.loadNextPageOfFeed(DownloadService.this, savedFeed, true);
                            } catch (DownloadRequestException e) {
                                Log.e(TAG, "Error trying to load next page", e);
                            }
                        }

                        ClientConfig.downloadServiceCallbacks.onFeedParsed(DownloadService.this,
                                savedFeed);

                        numberOfDownloads.decrementAndGet();
                    }

                    queryDownloadsAsync();
                });

            }

            if (dbUpdateFuture != null) {
                try {
                    dbUpdateFuture.get();
                } catch (InterruptedException e) {
                    Log.e(TAG, "interrupted while updating the db");
                } catch (ExecutionException e) {
                    Log.e(TAG, "ExecutionException while updating the db: " + e.getMessage());
                }
            }

            Log.d(TAG, "Shutting down");
        }

        /**
         * Helper method
         */
        private Feed[] getFeeds(List<Pair<DownloadRequest, FeedHandlerResult>> results) {
            Feed[] feeds = new Feed[results.size()];
            for (int i = 0; i < results.size(); i++) {
                feeds[i] = results.get(i).second.feed;
            }
            return feeds;
        }

        private class FeedParserTask implements Callable<Pair<DownloadRequest, FeedHandlerResult>> {

            private final DownloadRequest request;

            private FeedParserTask(DownloadRequest request) {
                this.request = request;
            }

            @Override
            public Pair<DownloadRequest, FeedHandlerResult> call() throws Exception {
                return parseFeed(request);
            }
        }

        private Pair<DownloadRequest, FeedHandlerResult> parseFeed(DownloadRequest request) {
            Feed feed = new Feed(request.getSource(), request.getLastModified());
            feed.setFile_url(request.getDestination());
            feed.setId(request.getFeedfileId());
            feed.setDownloaded(true);
            feed.setPreferences(new FeedPreferences(0, true, FeedPreferences.AutoDeleteAction.GLOBAL,
                    request.getUsername(), request.getPassword()));
            feed.setPageNr(request.getArguments().getInt(DownloadRequester.REQUEST_ARG_PAGE_NR, 0));

            DownloadError reason = null;
            String reasonDetailed = null;
            boolean successful = true;
            FeedHandler feedHandler = new FeedHandler();

            FeedHandlerResult result = null;
            try {
                result = feedHandler.parseFeed(feed);
                Log.d(TAG, feed.getTitle() + " parsed");
                if (!checkFeedData(feed)) {
                    throw new InvalidFeedException();
                }

            } catch (SAXException | IOException | ParserConfigurationException e) {
                successful = false;
                e.printStackTrace();
                reason = DownloadError.ERROR_PARSER_EXCEPTION;
                reasonDetailed = e.getMessage();
            } catch (UnsupportedFeedtypeException e) {
                e.printStackTrace();
                successful = false;
                reason = DownloadError.ERROR_UNSUPPORTED_TYPE;
                reasonDetailed = e.getMessage();
            } catch (InvalidFeedException e) {
                e.printStackTrace();
                successful = false;
                reason = DownloadError.ERROR_PARSER_EXCEPTION;
                reasonDetailed = e.getMessage();
            } finally {
                File feedFile = new File(request.getDestination());
                if (feedFile.exists()) {
                    boolean deleted = feedFile.delete();
                    Log.d(TAG, "Deletion of file '" + feedFile.getAbsolutePath() + "' " + (deleted ? "successful" : "FAILED"));
                }
            }

            if (successful) {
                // we create a 'successful' download log if the feed's last refresh failed
                List<DownloadStatus> log = DBReader.getFeedDownloadLog(feed);
                if (log.size() > 0 && !log.get(0).isSuccessful()) {
                    saveDownloadStatus(
                            new DownloadStatus(feed, feed.getHumanReadableIdentifier(),
                                    DownloadError.SUCCESS, successful, reasonDetailed));
                }
                return Pair.create(request, result);
            } else {
                numberOfDownloads.decrementAndGet();
                saveDownloadStatus(
                        new DownloadStatus(feed, feed.getHumanReadableIdentifier(), reason,
                                successful, reasonDetailed));
                return null;
            }
        }


        /**
         * Checks if the feed was parsed correctly.
         */
        private boolean checkFeedData(Feed feed) {
            if (feed.getTitle() == null) {
                Log.e(TAG, "Feed has no title.");
                return false;
            }
            if (!hasValidFeedItems(feed)) {
                Log.e(TAG, "Feed has invalid items");
                return false;
            }
            return true;
        }

        private boolean hasValidFeedItems(Feed feed) {
            for (FeedItem item : feed.getItems()) {
                if (item.getTitle() == null) {
                    Log.e(TAG, "Item has no title");
                    return false;
                }
                if (item.getPubDate() == null) {
                    Log.e(TAG, "Item has no pubDate. Using current time as pubDate");
                    if (item.getTitle() != null) {
                        Log.e(TAG, "Title of invalid item: " + item.getTitle());
                    }
                    item.setPubDate(new Date());
                }
            }
            return true;
        }

        /**
         * Delete files that aren't needed anymore
         */
        private void cleanup(Feed feed) {
            if (feed.getFile_url() != null) {
                if (new File(feed.getFile_url()).delete()) {
                    Log.d(TAG, "Successfully deleted cache file.");
                } else {
                    Log.e(TAG, "Failed to delete cache file.");
                }
                feed.setFile_url(null);
            } else {
                Log.d(TAG, "Didn't delete cache file: File url is not set.");
            }
        }

        public void shutdown() {
            isActive = false;
            if (isCollectingRequests) {
                interrupt();
            }
        }

        void submitCompletedDownload(DownloadRequest request) {
            completedRequests.offer(request);
            if (isCollectingRequests) {
                interrupt();
            }
        }

    }

    /**
     * Creates the destination file and writes FeedMedia File_url directly after starting download
     * to make it possible to resume download after the service was killed by the system.
     */
    private void writeFileUrl(DownloadRequest request) {
        if (request.getFeedfileType() != FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            return;
        }

        File dest = new File(request.getDestination());
        if (!dest.exists()) {
            try {
                dest.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "Unable to create file");
            }
        }

        if (dest.exists()) {
            Log.d(TAG, "Writing file url");
            FeedMedia media = DBReader.getFeedMedia(request.getFeedfileId());
            if (media == null) {
                Log.d(TAG, "No media");
                return;
            }
            media.setFile_url(request.getDestination());
            try {
                DBWriter.setFeedMedia(media).get();
            } catch (InterruptedException e) {
                Log.e(TAG, "writeFileUrl was interrupted");
            } catch (ExecutionException e) {
                Log.e(TAG, "ExecutionException in writeFileUrl: " + e.getMessage());
            }
        }
    }

    /**
     * Handles failed downloads.
     * <p/>
     * If the file has been partially downloaded, this handler will set the file_url of the FeedFile to the location
     * of the downloaded file.
     * <p/>
     * Currently, this handler only handles FeedMedia objects, because Feeds and FeedImages are deleted if the download fails.
     */
    private static class FailedDownloadHandler implements Runnable {

        private final DownloadRequest request;
        private final DownloadStatus status;

        FailedDownloadHandler(DownloadStatus status, DownloadRequest request) {
            this.request = request;
            this.status = status;
        }

        @Override
        public void run() {
            if (request.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
                DBWriter.setFeedLastUpdateFailed(request.getFeedfileId(), true);
            } else if (request.isDeleteOnFailure()) {
                Log.d(TAG, "Ignoring failed download, deleteOnFailure=true");
            }
        }
    }

    /**
     * Handles a completed media download.
     */
    private class MediaHandlerThread implements Runnable {

        private final DownloadRequest request;
        private DownloadStatus status;

        MediaHandlerThread(@NonNull DownloadStatus status,
                           @NonNull DownloadRequest request) {
            this.status = status;
            this.request = request;
        }

        @Override
        public void run() {
            FeedMedia media = DBReader.getFeedMedia(request.getFeedfileId());
            if (media == null) {
                Log.e(TAG, "Could not find downloaded media object in database");
                return;
            }
            media.setDownloaded(true);
            media.setFile_url(request.getDestination());
            media.checkEmbeddedPicture(); // enforce check

            // check if file has chapters
            if(media.getItem() != null && !media.getItem().hasChapters()) {
                ChapterUtils.loadChaptersFromFileUrl(media);
            }

            // Get duration
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            String durationStr = null;
            try {
                mmr.setDataSource(media.getFile_url());
                durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                media.setDuration(Integer.parseInt(durationStr));
                Log.d(TAG, "Duration of file is " + media.getDuration());
            } catch (NumberFormatException e) {
                Log.d(TAG, "Invalid file duration: " + durationStr);
            } catch (Exception e) {
                Log.e(TAG, "Get duration failed", e);
            } finally {
                mmr.release();
            }

            final FeedItem item = media.getItem();

            try {
                // we've received the media, we don't want to autodownload it again
                if (item != null) {
                    item.setAutoDownload(false);
                    DBWriter.setFeedItem(item).get();
                }

                DBWriter.setFeedMedia(media).get();

                if (item != null && UserPreferences.enqueueDownloadedEpisodes() &&
                        !DBTasks.isInQueue(DownloadService.this, item.getId())) {
                    DBWriter.addQueueItem(DownloadService.this, item).get();
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "MediaHandlerThread was interrupted");
            } catch (ExecutionException e) {
                Log.e(TAG, "ExecutionException in MediaHandlerThread: " + e.getMessage());
                status = new DownloadStatus(media, media.getEpisodeTitle(), DownloadError.ERROR_DB_ACCESS_ERROR, false, e.getMessage());
            }

            saveDownloadStatus(status);

            if (GpodnetPreferences.loggedIn() && item != null) {
                GpodnetEpisodeAction action = new GpodnetEpisodeAction.Builder(item, Action.DOWNLOAD)
                        .currentDeviceId()
                        .currentTimestamp()
                        .build();
                GpodnetPreferences.enqueueEpisodeAction(action);
            }

            numberOfDownloads.decrementAndGet();
            queryDownloadsAsync();
        }
    }

    /**
     * Schedules the notification updater task if it hasn't been scheduled yet.
     */
    private void setupNotificationUpdater() {
        Log.d(TAG, "Setting up notification updater");
        if (notificationUpdater == null) {
            notificationUpdater = new NotificationUpdater();
            notificationUpdaterFuture = schedExecutor.scheduleAtFixedRate(
                    notificationUpdater, 5L, 5L, TimeUnit.SECONDS);
        }
    }

    private void cancelNotificationUpdater() {
        boolean result = false;
        if (notificationUpdaterFuture != null) {
            result = notificationUpdaterFuture.cancel(true);
        }
        notificationUpdater = null;
        notificationUpdaterFuture = null;
        Log.d(TAG, "NotificationUpdater cancelled. Result: " + result);
    }

    private class NotificationUpdater implements Runnable {
        public void run() {
            handler.post(() -> {
                Notification n = updateNotifications();
                if (n != null) {
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(NOTIFICATION_ID, n);
                }
            });
        }

    }


    private long lastPost = 0;

    private final Runnable postDownloaderTask = new Runnable() {
        @Override
        public void run() {
            List<Downloader> list = Collections.unmodifiableList(downloads);
            EventBus.getDefault().postSticky(DownloadEvent.refresh(list));
            postHandler.postDelayed(postDownloaderTask, 1500);
        }
    };

    private void postDownloaders() {
        long now = System.currentTimeMillis();
        if (now - lastPost >= 250) {
            postHandler.removeCallbacks(postDownloaderTask);
            postDownloaderTask.run();
            lastPost = now;
        }
    }

    /**
     * Checks if the FeedItems of this feed have images that point to the same URL. If two FeedItems
     * have an image that points to the same URL, the reference of the second item is removed, so
     * that every image reference is unique.
     */
    @VisibleForTesting
    public static void removeDuplicateImages(Feed feed) {
        Set<String> known = new HashSet<>();
        for (FeedItem item : feed.getItems()) {
            String url = item.hasItemImage() ? item.getImage().getDownload_url() : null;
            if (url != null) {
                if (known.contains(url)) {
                    item.setImage(null);
                } else {
                    known.add(url);
                }
            }
        }
    }

    private static String compileNotificationString(List<Downloader> downloads) {
        List<String> lines = new ArrayList<>(downloads.size());
        for (Downloader downloader : downloads) {
            StringBuilder line = new StringBuilder("\u2022 ");
            DownloadRequest request = downloader.getDownloadRequest();
            switch (request.getFeedfileType()) {
                case Feed.FEEDFILETYPE_FEED:
                    if (request.getTitle() != null) {
                        line.append(request.getTitle());
                    }
                    break;
                case FeedMedia.FEEDFILETYPE_FEEDMEDIA:
                    if (request.getTitle() != null) {
                        line.append(request.getTitle())
                                .append(" (")
                                .append(request.getProgressPercent())
                                .append("%)");
                    }
                    break;
                default:
                    line.append("Unknown: ").append(request.getFeedfileType());
            }
            lines.add(line.toString());
        }
        return TextUtils.join("\n", lines);
    }
}
