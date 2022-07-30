package de.danoeh.antennapod.core.service.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.ServiceCompat;

import androidx.core.content.ContextCompat;
import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.LocalFeedUpdater;
import de.danoeh.antennapod.core.storage.EpisodeCleanupAlgorithmFactory;
import de.danoeh.antennapod.model.download.DownloadStatus;
import org.apache.commons.io.FileUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.util.download.ConnectionStateMonitor;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.handler.FailedDownloadHandler;
import de.danoeh.antennapod.core.service.download.handler.FeedSyncTask;
import de.danoeh.antennapod.core.service.download.handler.MediaDownloadedHandler;
import de.danoeh.antennapod.core.service.download.handler.PostDownloaderTask;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.download.DownloadError;

/**
 * Manages the download of feedfiles in the app. Downloads can be enqueued via the startService intent.
 * The argument of the intent is an instance of DownloadRequest in the EXTRA_REQUESTS field of
 * the intent.
 * After the downloads have finished, the downloaded object will be passed on to a specific handler, depending on the
 * type of the feedfile.
 */
public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    private static final int SCHED_EX_POOL_SIZE = 1;
    public static final String ACTION_CANCEL_DOWNLOAD = "action.de.danoeh.antennapod.core.service.cancelDownload";
    public static final String ACTION_CANCEL_ALL_DOWNLOADS = "action.de.danoeh.antennapod.core.service.cancelAll";
    public static final String EXTRA_DOWNLOAD_URL = "downloadUrl";
    public static final String EXTRA_REQUESTS = "downloadRequests";
    public static final String EXTRA_REFRESH_ALL = "refreshAll";
    public static final String EXTRA_INITIATED_BY_USER = "initiatedByUser";
    public static final String EXTRA_CLEANUP_MEDIA = "cleanupMedia";

    public static boolean isRunning = false;

    // Can be modified from another thread while iterating. Both possible race conditions are not critical:
    // Remove while iterating: We think it is still downloading and don't start a new download with the same file.
    // Add while iterating: We think it is not downloading and might start a second download with the same file.
    static final List<Downloader> downloads = Collections.synchronizedList(new CopyOnWriteArrayList<>());
    private final ExecutorService downloadHandleExecutor;
    private final ExecutorService downloadEnqueueExecutor;

    private final List<DownloadStatus> reportQueue = new ArrayList<>();
    private DownloadServiceNotification notificationManager;
    private final NewEpisodesNotification newEpisodesNotification;
    private NotificationUpdater notificationUpdater;
    private ScheduledFuture<?> notificationUpdaterFuture;
    private ScheduledFuture<?> downloadPostFuture;
    private final ScheduledThreadPoolExecutor notificationUpdateExecutor;
    private static DownloaderFactory downloaderFactory = new DefaultDownloaderFactory();
    private ConnectionStateMonitor connectionMonitor;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public DownloadService() {
        newEpisodesNotification = new NewEpisodesNotification();

        downloadEnqueueExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "EnqueueThread");
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        // Must be the first runnable in syncExecutor
        downloadEnqueueExecutor.execute(newEpisodesNotification::loadCountersBeforeRefresh);

        Log.d(TAG, "parallel downloads: " + UserPreferences.getParallelDownloads());
        downloadHandleExecutor = Executors.newFixedThreadPool(UserPreferences.getParallelDownloads(),
            r -> {
                Thread t = new Thread(r, "DownloadThread");
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
        notificationUpdateExecutor = new ScheduledThreadPoolExecutor(SCHED_EX_POOL_SIZE,
                r -> {
                    Thread t = new Thread(r, "NotificationUpdateExecutor");
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                }, (r, executor) -> Log.w(TAG, "SchedEx rejected submission of new task")
        );
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Service started");
        isRunning = true;
        notificationManager = new DownloadServiceNotification(this);

        IntentFilter cancelDownloadReceiverFilter = new IntentFilter();
        cancelDownloadReceiverFilter.addAction(ACTION_CANCEL_ALL_DOWNLOADS);
        cancelDownloadReceiverFilter.addAction(ACTION_CANCEL_DOWNLOAD);
        registerReceiver(cancelDownloadReceiver, cancelDownloadReceiverFilter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectionMonitor = new ConnectionStateMonitor();
            connectionMonitor.enable(getApplicationContext());
        }
    }

    public static void download(Context context, boolean cleanupMedia, DownloadRequest... requests) {
        ArrayList<DownloadRequest> requestsToSend = new ArrayList<>();
        for (DownloadRequest request : requests) {
            if (!isDownloadingFile(request.getSource())) {
                requestsToSend.add(request);
            }
        }
        if (requestsToSend.isEmpty()) {
            return;
        } else if (requestsToSend.size() > 100) {
            if (BuildConfig.DEBUG) {
                throw new IllegalArgumentException("Android silently drops intent payloads that are too large");
            } else {
                Log.d(TAG, "Too many download requests. Dropping some to avoid Android dropping all.");
                requestsToSend = new ArrayList<>(requestsToSend.subList(0, 100));
            }
        }

        Intent launchIntent = new Intent(context, DownloadService.class);
        launchIntent.putParcelableArrayListExtra(DownloadService.EXTRA_REQUESTS, requestsToSend);
        if (cleanupMedia) {
            launchIntent.putExtra(DownloadService.EXTRA_CLEANUP_MEDIA, true);
        }
        ContextCompat.startForegroundService(context, launchIntent);
    }

    public static void refreshAllFeeds(Context context, boolean initiatedByUser) {
        Intent launchIntent = new Intent(context, DownloadService.class);
        launchIntent.putExtra(DownloadService.EXTRA_REFRESH_ALL, true);
        launchIntent.putExtra(DownloadService.EXTRA_INITIATED_BY_USER, initiatedByUser);
        ContextCompat.startForegroundService(context, launchIntent);
    }

    public static void cancel(Context context, String url) {
        if (!isRunning) {
            return;
        }
        Intent cancelIntent = new Intent(DownloadService.ACTION_CANCEL_DOWNLOAD);
        cancelIntent.putExtra(DownloadService.EXTRA_DOWNLOAD_URL, url);
        cancelIntent.setPackage(context.getPackageName());
        context.sendBroadcast(cancelIntent);
    }

    public static void cancelAll(Context context) {
        if (!isRunning) {
            return;
        }
        Intent cancelIntent = new Intent(DownloadService.ACTION_CANCEL_ALL_DOWNLOADS);
        cancelIntent.setPackage(context.getPackageName());
        context.sendBroadcast(cancelIntent);
    }

    public static boolean isDownloadingFeeds() {
        if (!isRunning) {
            return false;
        }
        for (Downloader downloader : downloads) {
            if (downloader.request.getFeedfileType() == Feed.FEEDFILETYPE_FEED && !downloader.cancelled) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDownloadingFile(String downloadUrl) {
        if (!isRunning) {
            return false;
        }
        for (Downloader downloader : downloads) {
            if (downloader.request.getSource().equals(downloadUrl) && !downloader.cancelled) {
                return true;
            }
        }
        return false;
    }

    public static DownloadRequest findRequest(String downloadUrl) {
        for (Downloader downloader : downloads) {
            if (downloader.request.getSource().equals(downloadUrl)) {
                return downloader.request;
            }
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_REQUESTS)) {
            Notification notification = notificationManager.updateNotifications(downloads);
            startForeground(R.id.notification_downloading, notification);
            setupNotificationUpdaterIfNecessary();
            downloadEnqueueExecutor.execute(() -> onDownloadQueued(intent));
        } else if (intent != null && intent.getBooleanExtra(EXTRA_REFRESH_ALL, false)) {
            Notification notification = notificationManager.updateNotifications(downloads);
            startForeground(R.id.notification_downloading, notification);
            setupNotificationUpdaterIfNecessary();
            downloadEnqueueExecutor.execute(() -> enqueueAll(intent));
        } else if (downloads.size() == 0) {
            shutdown();
        } else {
            Log.d(TAG, "onStartCommand: Unknown intent");
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service shutting down");
        isRunning = false;

        boolean showAutoDownloadReport = UserPreferences.showAutoDownloadReport();
        if (UserPreferences.showDownloadReport() || showAutoDownloadReport) {
            notificationManager.updateReport(reportQueue, showAutoDownloadReport);
            reportQueue.clear();
        }

        unregisterReceiver(cancelDownloadReceiver);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectionMonitor.disable(getApplicationContext());
        }

        EventBus.getDefault().postSticky(DownloadEvent.refresh(Collections.emptyList()));
        cancelNotificationUpdater();
        downloadEnqueueExecutor.shutdownNow();
        downloadHandleExecutor.shutdownNow();
        notificationUpdateExecutor.shutdownNow();
        if (downloadPostFuture != null) {
            downloadPostFuture.cancel(true);
        }
        downloads.clear();

        // start auto download in case anything new has shown up
        DBTasks.autodownloadUndownloadedItems(getApplicationContext());
    }

    /**
     * This method MUST NOT, in any case, throw an exception.
     * Otherwise, it hangs up the refresh thread pool.
     */
    private void performDownload(Downloader downloader) {
        try {
            downloader.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (downloader.getResult().isSuccessful()) {
                handleSuccessfulDownload(downloader);
            } else {
                handleFailedDownload(downloader);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        downloadEnqueueExecutor.submit(() -> {
            downloads.remove(downloader);
            stopServiceIfEverythingDone();
        });
    }

    /**
     * This method MUST NOT, in any case, throw an exception.
     * Otherwise, it hangs up the refresh thread pool.
     */
    private void performLocalFeedRefresh(Downloader downloader, DownloadRequest request) {
        try {
            Feed feed = DBReader.getFeed(request.getFeedfileId());
            LocalFeedUpdater.updateFeed(feed, DownloadService.this, (scanned, totalFiles) -> {
                request.setSize(totalFiles);
                request.setSoFar(scanned);
                request.setProgressPercent((int) (100.0 * scanned / totalFiles));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        downloadEnqueueExecutor.submit(() -> {
            downloads.remove(downloader);
            stopServiceIfEverythingDone();
        });
    }


    private void handleSuccessfulDownload(Downloader downloader) {
        DownloadRequest request = downloader.getDownloadRequest();
        DownloadStatus status = downloader.getResult();
        final int type = status.getFeedfileType();

        if (type == Feed.FEEDFILETYPE_FEED) {
            Log.d(TAG, "Handling completed Feed Download");
            FeedSyncTask task = new FeedSyncTask(DownloadService.this, request);
            boolean success = task.run();

            if (success) {
                if (request.getFeedfileId() == 0) {
                    return; // No download logs for new subscriptions
                }
                // we create a 'successful' download log if the feed's last refresh failed
                List<DownloadStatus> log = DBReader.getFeedDownloadLog(request.getFeedfileId());
                if (log.size() > 0 && !log.get(0).isSuccessful()) {
                    saveDownloadStatus(task.getDownloadStatus());
                }
                if (!request.isInitiatedByUser()) {
                    // Was stored in the database before and not initiated manually
                    newEpisodesNotification.showIfNeeded(DownloadService.this, task.getSavedFeed());
                }
                if (downloader.permanentRedirectUrl != null) {
                    DBWriter.updateFeedDownloadURL(request.getSource(), downloader.permanentRedirectUrl);
                }
            } else {
                DBWriter.setFeedLastUpdateFailed(request.getFeedfileId(), true);
                saveDownloadStatus(task.getDownloadStatus());
            }
        } else if (type == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            Log.d(TAG, "Handling completed FeedMedia Download");
            MediaDownloadedHandler handler = new MediaDownloadedHandler(DownloadService.this, status, request);
            handler.run();
            saveDownloadStatus(handler.getUpdatedStatus());
        }
    }

    private void handleFailedDownload(Downloader downloader) {
        DownloadStatus status = downloader.getResult();
        final int type = status.getFeedfileType();

        if (!status.isCancelled()) {
            if (status.getReason() == DownloadError.ERROR_UNAUTHORIZED) {
                notificationManager.postAuthenticationNotification(downloader.getDownloadRequest());
            } else if (status.getReason() == DownloadError.ERROR_HTTP_DATA_ERROR
                    && Integer.parseInt(status.getReasonDetailed()) == 416) {

                Log.d(TAG, "Requested invalid range, restarting download from the beginning");
                FileUtils.deleteQuietly(new File(downloader.getDownloadRequest().getDestination()));
                download(this, false, downloader.getDownloadRequest());
            } else {
                Log.e(TAG, "Download failed");
                saveDownloadStatus(status);
                new FailedDownloadHandler(downloader.getDownloadRequest()).run();

                if (type == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                    FeedItem item = getFeedItemFromId(status.getFeedfileId());
                    if (item == null) {
                        return;
                    }
                    item.increaseFailedAutoDownloadAttempts(System.currentTimeMillis());
                    DBWriter.setFeedItem(item);
                    // to make lists reload the failed item, we fake an item update
                    EventBus.getDefault().post(FeedItemEvent.updated(item));
                }
            }
        } else {
            // if FeedMedia download has been canceled, fake FeedItem update
            // so that lists reload that it
            if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                FeedItem item = getFeedItemFromId(status.getFeedfileId());
                if (item == null) {
                    return;
                }
                EventBus.getDefault().post(FeedItemEvent.updated(item));
            }
        }
    }

    private final BroadcastReceiver cancelDownloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "cancelDownloadReceiver: " + intent.getAction());
            if (!isRunning) {
                return;
            }
            if (TextUtils.equals(intent.getAction(), ACTION_CANCEL_DOWNLOAD)) {
                String url = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
                if (url == null) {
                    throw new IllegalArgumentException("ACTION_CANCEL_DOWNLOAD intent needs download url extra");
                }
                downloadEnqueueExecutor.execute(() -> {
                    doCancel(url);
                    postDownloaders();
                    stopServiceIfEverythingDone();
                });
            } else if (TextUtils.equals(intent.getAction(), ACTION_CANCEL_ALL_DOWNLOADS)) {
                downloadEnqueueExecutor.execute(() -> {
                    for (Downloader d : downloads) {
                        d.cancel();
                    }
                    Log.d(TAG, "Cancelled all downloads");
                    postDownloaders();
                    stopServiceIfEverythingDone();
                });
            }
        }
    };

    private void doCancel(String url) {
        Log.d(TAG, "Cancelling download with url " + url);
        for (Downloader downloader : downloads) {
            if (downloader.cancelled || !downloader.getDownloadRequest().getSource().equals(url)) {
                continue;
            }
            downloader.cancel();
            DownloadRequest request = downloader.getDownloadRequest();
            FeedItem item = getFeedItemFromId(request.getFeedfileId());
            if (item != null) {
                EventBus.getDefault().post(FeedItemEvent.updated(item));
                // undo enqueue upon cancel
                if (request.isMediaEnqueued()) {
                    Log.v(TAG, "Undoing enqueue upon cancelling download");
                    DBWriter.removeQueueItem(getApplicationContext(), false, item);
                }
            }
        }
    }

    private void onDownloadQueued(Intent intent) {
        List<DownloadRequest> requests = intent.getParcelableArrayListExtra(EXTRA_REQUESTS);
        if (requests == null) {
            throw new IllegalArgumentException("ACTION_ENQUEUE_DOWNLOAD intent needs request extra");
        }
        Log.d(TAG, "Received enqueue request. #requests=" + requests.size());

        if (intent.getBooleanExtra(EXTRA_CLEANUP_MEDIA, false)) {
            EpisodeCleanupAlgorithmFactory.build().makeRoomForEpisodes(getApplicationContext(), requests.size());
        }

        for (DownloadRequest request : requests) {
            addNewRequest(request);
        }
        postDownloaders();
        stopServiceIfEverythingDone();

        // Add to-download items to the queue before actual download completed
        // so that the resulting queue order is the same as when download is clicked
        enqueueFeedItems(requests);
    }

    private void enqueueFeedItems(@NonNull List<DownloadRequest> requests) {
        List<FeedItem> feedItems = new ArrayList<>();
        for (DownloadRequest request : requests) {
            if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                long mediaId = request.getFeedfileId();
                FeedMedia media = DBReader.getFeedMedia(mediaId);
                if (media == null) {
                    Log.w(TAG, "enqueueFeedItems() : FeedFile Id " + mediaId + " is not found. ignore it.");
                    continue;
                }
                feedItems.add(media.getItem());
            }
        }
        List<FeedItem> actuallyEnqueued = Collections.emptyList();
        try {
            actuallyEnqueued = DBTasks.enqueueFeedItemsToDownload(getApplicationContext(), feedItems);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        for (DownloadRequest request : requests) {
            if (request.getFeedfileType() != FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                continue;
            }
            final long mediaId = request.getFeedfileId();
            for (FeedItem item : actuallyEnqueued) {
                if (item.getMedia() != null && item.getMedia().getId() == mediaId) {
                    request.setMediaEnqueued(true);
                }
            }
        }
    }

    private void enqueueAll(Intent intent) {
        boolean initiatedByUser = intent.getBooleanExtra(EXTRA_INITIATED_BY_USER, false);
        List<Feed> feeds = DBReader.getFeedList();
        for (Feed feed : feeds) {
            if (feed.getPreferences().getKeepUpdated()) {
                DownloadRequest.Builder builder = DownloadRequestCreator.create(feed);
                builder.withInitiatedByUser(initiatedByUser);
                addNewRequest(builder.build());
            }
        }
        postDownloaders();
        stopServiceIfEverythingDone();
    }

    private void addNewRequest(@NonNull DownloadRequest request) {
        if (isDownloadingFile(request.getSource())) {
            Log.d(TAG, "Skipped enqueueing request. Already running.");
            return;
        } else if (downloadHandleExecutor.isShutdown()) {
            Log.d(TAG, "Skipped enqueueing request. Service is already shutting down.");
            return;
        }
        Log.d(TAG, "Add new request: " + request.getSource());
        if (request.getSource().startsWith(Feed.PREFIX_LOCAL_FOLDER)) {
            Downloader downloader = new LocalFeedStubDownloader(request);
            downloads.add(downloader);
            downloadHandleExecutor.submit(() -> performLocalFeedRefresh(downloader, request));
        } else {
            writeFileUrl(request);
            Downloader downloader = downloaderFactory.create(request);
            if (downloader != null) {
                downloads.add(downloader);
                downloadHandleExecutor.submit(() -> performDownload(downloader));
            }
        }
    }

    @VisibleForTesting
    public static DownloaderFactory getDownloaderFactory() {
        return downloaderFactory;
    }

    // public scope rather than package private,
    // because androidTest put classes in the non-standard de.test.antennapod hierarchy
    @VisibleForTesting
    public static void setDownloaderFactory(DownloaderFactory downloaderFactory) {
        DownloadService.downloaderFactory = downloaderFactory;
    }

    /**
     * Adds a new DownloadStatus object to the list of completed downloads and
     * saves it in the database
     *
     * @param status the download that is going to be saved
     */
    private void saveDownloadStatus(@NonNull DownloadStatus status) {
        reportQueue.add(status);
        DBWriter.addDownloadStatus(status);
    }

    /**
     * Check if there's something else to download, otherwise stop.
     */
    private void stopServiceIfEverythingDone() {
        Log.d(TAG, downloads.size() + " downloads left");
        if (downloads.size() <= 0) {
            Log.d(TAG, "Attempting shutdown");
            shutdown();
        }
    }

    @Nullable
    private FeedItem getFeedItemFromId(long id) {
        FeedMedia media = DBReader.getFeedMedia(id);
        if (media != null) {
            return media.getItem();
        } else {
            return null;
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
     * Schedules the notification updater task if it hasn't been scheduled yet.
     */
    private void setupNotificationUpdaterIfNecessary() {
        if (notificationUpdater == null) {
            Log.d(TAG, "Setting up notification updater");
            notificationUpdater = new NotificationUpdater();
            notificationUpdaterFuture = notificationUpdateExecutor
                    .scheduleAtFixedRate(notificationUpdater, 1, 1, TimeUnit.SECONDS);
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
            Notification n = notificationManager.updateNotifications(downloads);
            if (n != null) {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(R.id.notification_downloading, n);
            }
        }
    }

    private void postDownloaders() {
        new PostDownloaderTask(downloads).run();

        if (downloadPostFuture == null) {
            downloadPostFuture = notificationUpdateExecutor.scheduleAtFixedRate(
                    new PostDownloaderTask(downloads), 1, 1, TimeUnit.SECONDS);
        }
    }

    private void shutdown() {
        // If the service was run for a very short time, the system may delay closing
        // the notification. Set the notification text now so that a misleading message
        // is not left on the notification.
        if (notificationUpdater != null) {
            notificationUpdater.run();
        }
        cancelNotificationUpdater();
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }
}
