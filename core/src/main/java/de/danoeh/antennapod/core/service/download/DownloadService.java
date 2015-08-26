package de.danoeh.antennapod.core.service.download;

import android.annotation.SuppressLint;
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
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;
import android.util.Log;
import android.webkit.URLUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpStatus;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.ParserConfigurationException;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeAction;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeAction.Action;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
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

/**
 * Manages the download of feedfiles in the app. Downloads can be enqueued viathe startService intent.
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
     * Sent by the DownloadService when the content of the downloads list
     * changes.
     */
    public static final String ACTION_DOWNLOADS_CONTENT_CHANGED = "action.de.danoeh.antennapod.core.service.downloadsContentChanged";

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
    private Notification.BigTextStyle notificationBuilder;
    private int NOTIFICATION_ID = 2;
    private int REPORT_ID = 3;

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
    private ScheduledFuture notificationUpdaterFuture;
    private static final int SCHED_EX_POOL_SIZE = 1;
    private ScheduledThreadPoolExecutor schedExecutor;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    private Thread downloadCompletionThread = new Thread() {
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
                            handleCompletedFeedDownload(downloader
                                    .getDownloadRequest());
                        } else if (type == FeedImage.FEEDFILETYPE_FEEDIMAGE) {
                            handleCompletedImageDownload(status, downloader.getDownloadRequest());
                        } else if (type == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                            handleCompletedFeedMediaDownload(status, downloader.getDownloadRequest());
                        }
                    } else {
                        numberOfDownloads.decrementAndGet();
                        if (!status.isCancelled()) {
                            if (status.getReason() == DownloadError.ERROR_UNAUTHORIZED) {
                                postAuthenticationNotification(downloader.getDownloadRequest());
                            } else if (status.getReason() == DownloadError.ERROR_HTTP_DATA_ERROR
                                    && Integer.valueOf(status.getReasonDetailed()) == HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE) {

                                Log.d(TAG, "Requested invalid range, restarting download from the beginning");
                                FileUtils.deleteQuietly(new File(downloader.getDownloadRequest().getDestination()));
                                DownloadRequester.getInstance().download(DownloadService.this, downloader.getDownloadRequest());
                            } else {
                                Log.e(TAG, "Download failed");
                                saveDownloadStatus(status);
                                handleFailedDownload(status, downloader.getDownloadRequest());
                            }
                        }
                        sendDownloadHandledIntent();
                        queryDownloadsAsync();
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "DownloadCompletionThread was interrupted");
                } catch (ExecutionException e) {
                    e.printStackTrace();
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

    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        Log.d(TAG, "Service started");
        isRunning = true;
        handler = new Handler();
        reportQueue = Collections.synchronizedList(new ArrayList<DownloadStatus>());
        downloads = Collections.synchronizedList(new ArrayList<Downloader>());
        numberOfDownloads = new AtomicInteger(0);

        IntentFilter cancelDownloadReceiverFilter = new IntentFilter();
        cancelDownloadReceiverFilter.addAction(ACTION_CANCEL_ALL_DOWNLOADS);
        cancelDownloadReceiverFilter.addAction(ACTION_CANCEL_DOWNLOAD);
        registerReceiver(cancelDownloadReceiver, cancelDownloadReceiverFilter);
        syncExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });
        Log.d(TAG, "parallel downloads: " + UserPreferences.getParallelDownloads());
        downloadExecutor = new ExecutorCompletionService<Downloader>(
                Executors.newFixedThreadPool(UserPreferences.getParallelDownloads(),
                        new ThreadFactory() {

                            @Override
                            public Thread newThread(Runnable r) {
                                Thread t = new Thread(r);
                                t.setPriority(Thread.MIN_PRIORITY);
                                return t;
                            }
                        }
                )
        );
        schedExecutor = new ScheduledThreadPoolExecutor(SCHED_EX_POOL_SIZE,
                new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setPriority(Thread.MIN_PRIORITY);
                        return t;
                    }
                }, new RejectedExecutionHandler() {

            @Override
            public void rejectedExecution(Runnable r,
                                          ThreadPoolExecutor executor) {
                Log.w(TAG, "SchedEx rejected submission of new task");
            }
        }
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

        stopForeground(true);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);

        downloadCompletionThread.interrupt();
        syncExecutor.shutdown();
        schedExecutor.shutdown();
        feedSyncThread.shutdown();
        cancelNotificationUpdater();
        unregisterReceiver(cancelDownloadReceiver);

        // start auto download in case anything new has shown up
        DBTasks.autodownloadUndownloadedItems(getApplicationContext());
    }

    @SuppressLint("NewApi")
    private void setupNotificationBuilders() {
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.stat_notify_sync);

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notificationBuilder = new Notification.BigTextStyle(
                    new Notification.Builder(this).setOngoing(true)
                            .setContentIntent(ClientConfig.downloadServiceCallbacks.getNotificationContentIntent(this)).setLargeIcon(icon)
                            .setSmallIcon(R.drawable.stat_notify_sync)
            );
        } else {
            notificationCompatBuilder = new NotificationCompat.Builder(this)
                    .setOngoing(true).setContentIntent(ClientConfig.downloadServiceCallbacks.getNotificationContentIntent(this))
                    .setLargeIcon(icon)
                    .setSmallIcon(R.drawable.stat_notify_sync);
        }
        Log.d(TAG, "Notification set up");
    }

    /**
     * Updates the contents of the service's notifications. Should be called
     * before setupNotificationBuilders.
     */
    @SuppressLint("NewApi")
    private Notification updateNotifications() {
        String contentTitle = getString(R.string.download_notification_title);
        int numDownloads = requester.getNumberOfDownloads();
        String downloadsLeft;
        if (numDownloads > 0) {
            downloadsLeft = requester.getNumberOfDownloads()
                    + getString(R.string.downloads_left);
        } else {
            downloadsLeft = getString(R.string.downloads_processing);
        }
        if (android.os.Build.VERSION.SDK_INT >= 16) {

            if (notificationBuilder != null) {

                StringBuilder bigText = new StringBuilder("");
                for (int i = 0; i < downloads.size(); i++) {
                    Downloader downloader = downloads.get(i);
                    final DownloadRequest request = downloader
                            .getDownloadRequest();
                    if (request.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
                        if (request.getTitle() != null) {
                            if (i > 0) {
                                bigText.append("\n");
                            }
                            bigText.append("\u2022 " + request.getTitle());
                        }
                    } else if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                        if (request.getTitle() != null) {
                            if (i > 0) {
                                bigText.append("\n");
                            }
                            bigText.append("\u2022 " + request.getTitle()
                                    + " (" + request.getProgressPercent()
                                    + "%)");
                        }
                    }

                }
                notificationBuilder.setSummaryText(downloadsLeft);
                notificationBuilder.setBigContentTitle(contentTitle);
                if (bigText != null) {
                    notificationBuilder.bigText(bigText.toString());
                }
                return notificationBuilder.build();
            }
        } else {
            if (notificationCompatBuilder != null) {
                notificationCompatBuilder.setContentTitle(contentTitle);
                notificationCompatBuilder.setContentText(downloadsLeft);
                return notificationCompatBuilder.build();
            }
        }
        return null;
    }

    private Downloader getDownloader(String downloadUrl) {
        for (Downloader downloader : downloads) {
            if (downloader.getDownloadRequest().getSource().equals(downloadUrl)) {
                return downloader;
            }
        }
        return null;
    }

    private BroadcastReceiver cancelDownloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (StringUtils.equals(intent.getAction(), ACTION_CANCEL_DOWNLOAD)) {
                String url = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
                Validate.notNull(url, "ACTION_CANCEL_DOWNLOAD intent needs download url extra");

                Log.d(TAG, "Cancelling download with url " + url);
                Downloader d = getDownloader(url);
                if (d != null) {
                    d.cancel();
                } else {
                    Log.e(TAG, "Could not cancel download with url " + url);
                }
                sendBroadcast(new Intent(ACTION_DOWNLOADS_CONTENT_CHANGED));

            } else if (StringUtils.equals(intent.getAction(), ACTION_CANCEL_ALL_DOWNLOADS)) {
                for (Downloader d : downloads) {
                    d.cancel();
                    Log.d(TAG, "Cancelled all downloads");
                }
                sendBroadcast(new Intent(ACTION_DOWNLOADS_CONTENT_CHANGED));

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

        Downloader downloader = getDownloader(request);
        if (downloader != null) {
            numberOfDownloads.incrementAndGet();
            // smaller rss feeds before bigger media files
            if(request.getFeedfileId() == Feed.FEEDFILETYPE_FEED) {
                downloads.add(0, downloader);
            } else {
                downloads.add(downloader);
            }
            downloadExecutor.submit(downloader);
            sendBroadcast(new Intent(ACTION_DOWNLOADS_CONTENT_CHANGED));
        }

        queryDownloads();
    }

    private Downloader getDownloader(DownloadRequest request) {
        if (URLUtil.isHttpUrl(request.getSource())
                || URLUtil.isHttpsUrl(request.getSource())) {
            return new HttpDownloader(request);
        }
        Log.e(TAG,
                "Could not find appropriate downloader for "
                        + request.getSource()
        );
        return null;
    }

    /**
     * Remove download from the DownloadRequester list and from the
     * DownloadService list.
     */
    private void removeDownload(final Downloader d) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Removing downloader: "
                        + d.getDownloadRequest().getSource());
                boolean rc = downloads.remove(d);
                Log.d(TAG, "Result of downloads.remove: " + rc);
                DownloadRequester.getInstance().removeDownload(d.getDownloadRequest());
                sendBroadcast(new Intent(ACTION_DOWNLOADS_CONTENT_CHANGED));
            }
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
        DBWriter.addDownloadStatus(this, status);
    }

    private void sendDownloadHandledIntent() {
        EventDistributor.getInstance().sendDownloadHandledBroadcast();
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
            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(
                            getString(R.string.download_report_title))
                    .setContentTitle(
                            getString(R.string.download_report_content_title))
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
                    .setAutoCancel(true).build();
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(REPORT_ID, notification);
        } else {
            Log.d(TAG, "No report is created");
        }
        reportQueue.clear();
    }

    /**
     * Calls query downloads on the services main thread. This method should be used instead of queryDownloads if it is
     * used from a thread other than the main thread.
     */
    void queryDownloadsAsync() {
        handler.post(new Runnable() {
            public void run() {
                queryDownloads();
                ;
            }
        });
    }

    /**
     * Check if there's something else to download, otherwise stop
     */
    void queryDownloads() {
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
        handler.post(new Runnable() {
            @Override
            public void run() {
                final String resourceTitle = (downloadRequest.getTitle() != null)
                        ? downloadRequest.getTitle() : downloadRequest.getSource();

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
                Notification n = builder.build();
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(downloadRequest.getSource().hashCode(), n);
            }
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
     * Is called whenever a Feed-Image is downloaded
     */
    private void handleCompletedImageDownload(DownloadStatus status, DownloadRequest request) {
        Log.d(TAG, "Handling completed Image Download");
        syncExecutor.execute(new ImageHandlerThread(status, request));
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
    class FeedSyncThread extends Thread {
        private static final String TAG = "FeedSyncThread";

        private BlockingQueue<DownloadRequest> completedRequests = new LinkedBlockingDeque<DownloadRequest>();
        private CompletionService<Pair<DownloadRequest, FeedHandlerResult>> parserService = new ExecutorCompletionService<Pair<DownloadRequest, FeedHandlerResult>>(Executors.newSingleThreadExecutor());
        private ExecutorService dbService = Executors.newSingleThreadExecutor();
        private Future<?> dbUpdateFuture;
        private volatile boolean isActive = true;
        private volatile boolean isCollectingRequests = false;

        private final long WAIT_TIMEOUT = 3000;


        /**
         * Waits for completed requests. Once the first request has been taken, the method will wait WAIT_TIMEOUT ms longer to
         * collect more completed requests.
         *
         * @return Collected feeds or null if the method has been interrupted during the first waiting period.
         */
        private List<Pair<DownloadRequest, FeedHandlerResult>> collectCompletedRequests() {
            List<Pair<DownloadRequest, FeedHandlerResult>> results = new LinkedList<Pair<DownloadRequest, FeedHandlerResult>>();
            DownloadRequester requester = DownloadRequester.getInstance();
            int tasks = 0;

            try {
                DownloadRequest request = completedRequests.take();
                parserService.submit(new FeedParserTask(request));
                tasks++;
            } catch (InterruptedException e) {
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
                    e.printStackTrace();

                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            return results;
        }

        private int pollCompletedDownloads() {
            int tasks = 0;
            for (int i = 0; i < completedRequests.size(); i++) {
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
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                dbUpdateFuture = dbService.submit(new Runnable() {
                    @Override
                    public void run() {
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

                        sendDownloadHandledIntent();

                        queryDownloadsAsync();
                    }
                });

            }

            if (dbUpdateFuture != null) {
                try {
                    dbUpdateFuture.get();
                } catch (InterruptedException e) {
                } catch (ExecutionException e) {
                    e.printStackTrace();
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

            private DownloadRequest request;

            private FeedParserTask(DownloadRequest request) {
                this.request = request;
            }

            @Override
            public Pair<DownloadRequest, FeedHandlerResult> call() throws Exception {
                return parseFeed(request);
            }
        }

        private Pair<DownloadRequest, FeedHandlerResult> parseFeed(DownloadRequest request) {
            Feed feed = new Feed(request.getSource(), new Date());
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
                if (checkFeedData(feed) == false) {
                    throw new InvalidFeedException();
                }

            } catch (SAXException e) {
                successful = false;
                e.printStackTrace();
                reason = DownloadError.ERROR_PARSER_EXCEPTION;
                reasonDetailed = e.getMessage();
            } catch (IOException e) {
                successful = false;
                e.printStackTrace();
                reason = DownloadError.ERROR_PARSER_EXCEPTION;
                reasonDetailed = e.getMessage();
            } catch (ParserConfigurationException e) {
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
            }

            // cleanup();


            if (successful) {
                // we create a 'successful' download log if the feed's last refresh failed
                List<DownloadStatus> log = DBReader.getFeedDownloadLog(DownloadService.this, feed);
                if(log.size() > 0 && log.get(0).isSuccessful() == false) {
                    saveDownloadStatus(new DownloadStatus(feed,
                            feed.getHumanReadableIdentifier(), DownloadError.SUCCESS, successful,
                            reasonDetailed));
                }
                return Pair.create(request, result);
            } else {
                numberOfDownloads.decrementAndGet();
                saveDownloadStatus(new DownloadStatus(feed,
                        feed.getHumanReadableIdentifier(), reason, successful,
                        reasonDetailed));
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

        /**
         * Checks if the FeedItems of this feed have images that point
         * to the same URL. If two FeedItems have an image that points to
         * the same URL, the reference of the second item is removed, so that every image
         * reference is unique.
         */
        private void removeDuplicateImages(Feed feed) {
            for (int x = 0; x < feed.getItems().size(); x++) {
                for (int y = x + 1; y < feed.getItems().size(); y++) {
                    FeedItem item1 = feed.getItems().get(x);
                    FeedItem item2 = feed.getItems().get(y);
                    if (item1.hasItemImage() && item2.hasItemImage()) {
                        if (StringUtils.equals(item1.getImage().getDownload_url(), item2.getImage().getDownload_url())) {
                            item2.setImage(null);
                        }
                    }
                }
            }
        }

        private boolean hasValidFeedItems(Feed feed) {
            for (FeedItem item : feed.getItems()) {
                if (item.getTitle() == null) {
                    Log.e(TAG, "Item has no title");
                    return false;
                }
                if (item.getPubDate() == null) {
                    Log.e(TAG,
                            "Item has no pubDate. Using current time as pubDate");
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

        public void submitCompletedDownload(DownloadRequest request) {
            completedRequests.offer(request);
            if (isCollectingRequests) {
                interrupt();
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
    class FailedDownloadHandler implements Runnable {

        private DownloadRequest request;
        private DownloadStatus status;

        FailedDownloadHandler(DownloadStatus status, DownloadRequest request) {
            this.request = request;
            this.status = status;
        }

        @Override
        public void run() {
            if(request.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
                DBWriter.setFeedLastUpdateFailed(DownloadService.this, request.getFeedfileId(), true);
            } else if (request.isDeleteOnFailure()) {
                Log.d(TAG, "Ignoring failed download, deleteOnFailure=true");
            } else  {
                File dest = new File(request.getDestination());
                if (dest.exists() && request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                    Log.d(TAG, "File has been partially downloaded. Writing file url");
                    FeedMedia media = DBReader.getFeedMedia(DownloadService.this, request.getFeedfileId());
                    media.setFile_url(request.getDestination());
                    try {
                        DBWriter.setFeedMedia(DownloadService.this, media).get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Handles a completed image download.
     */
    class ImageHandlerThread implements Runnable {

        private DownloadRequest request;
        private DownloadStatus status;

        public ImageHandlerThread(DownloadStatus status, DownloadRequest request) {
            Validate.notNull(status);
            Validate.notNull(request);

            this.status = status;
            this.request = request;
        }

        @Override
        public void run() {
            FeedImage image = DBReader.getFeedImage(DownloadService.this, request.getFeedfileId());
            if (image == null) {
                throw new IllegalStateException("Could not find downloaded image in database");
            }

            image.setFile_url(request.getDestination());
            image.setDownloaded(true);

            saveDownloadStatus(status);
            sendDownloadHandledIntent();
            DBWriter.setFeedImage(DownloadService.this, image);
            numberOfDownloads.decrementAndGet();
            queryDownloadsAsync();
        }
    }

    /**
     * Handles a completed media download.
     */
    class MediaHandlerThread implements Runnable {

        private DownloadRequest request;
        private DownloadStatus status;

        public MediaHandlerThread(DownloadStatus status, DownloadRequest request) {
            Validate.notNull(status);
            Validate.notNull(request);

            this.status = status;
            this.request = request;
        }

        @Override
        public void run() {
            FeedMedia media = DBReader.getFeedMedia(DownloadService.this,
                    request.getFeedfileId());
            if (media == null) {
                throw new IllegalStateException(
                        "Could not find downloaded media object in database");
            }
            boolean chaptersRead = false;
            media.setDownloaded(true);
            media.setFile_url(request.getDestination());
            media.setHasEmbeddedPicture(null);

            // Get duration
            MediaMetadataRetriever mmr = null;
            try {
                mmr = new MediaMetadataRetriever();
                mmr.setDataSource(media.getFile_url());
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                media.setDuration(Integer.parseInt(durationStr));
                Log.d(TAG, "Duration of file is " + media.getDuration());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                if (mmr != null) {
                    mmr.release();
                }
            }

            if (media.getItem().getChapters() == null) {
                ChapterUtils.loadChaptersFromFileUrl(media);
                if (media.getItem().getChapters() != null) {
                    chaptersRead = true;
                }
            }

            try {
                // we've received the media, we don't want to autodownload it again
                FeedItem item = media.getItem();
                item.setAutoDownload(false);

                // update the db
                DBWriter.setFeedItem(DownloadService.this, item).get();

                DBWriter.setFeedMedia(DownloadService.this, media).get();
                if (!DBTasks.isInQueue(DownloadService.this, item.getId())) {
                    DBWriter.addQueueItem(DownloadService.this, item.getId()).get();
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
                status = new DownloadStatus(media, media.getEpisodeTitle(), DownloadError.ERROR_DB_ACCESS_ERROR, false, e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                status = new DownloadStatus(media, media.getEpisodeTitle(), DownloadError.ERROR_DB_ACCESS_ERROR, false, e.getMessage());
            }

            saveDownloadStatus(status);
            sendDownloadHandledIntent();

            if(GpodnetPreferences.loggedIn()) {
                FeedItem item = media.getItem();
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
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Notification n = updateNotifications();
                    if (n != null) {
                        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        nm.notify(NOTIFICATION_ID, n);
                    }
                }
            });
        }
    }

    public List<Downloader> getDownloads() {
        if (downloads == null) {
            // this is unusual, but it should be OK, we'll return
            // an empty list to make it easy for people
            return new ArrayList<Downloader>();
        }

        // return a copy of downloads, but the copy doesn't need to be synchronized.
        synchronized (downloads) {
            return new ArrayList<Downloader>(downloads);
        }
    }

}
