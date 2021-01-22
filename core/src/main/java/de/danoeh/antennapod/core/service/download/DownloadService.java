package de.danoeh.antennapod.core.service.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.sync.SyncService;
import org.apache.commons.io.FileUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.handler.FailedDownloadHandler;
import de.danoeh.antennapod.core.service.download.handler.FeedSyncTask;
import de.danoeh.antennapod.core.service.download.handler.MediaDownloadedHandler;
import de.danoeh.antennapod.core.service.download.handler.PostDownloaderTask;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.DownloadError;

/**
 * Manages the download of feedfiles in the app. Downloads can be enqueued via the startService intent.
 * The argument of the intent is an instance of DownloadRequest in the EXTRA_REQUESTS field of
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
    public static final String EXTRA_REQUESTS = "downloadRequests";

    public static final String EXTRA_CLEANUP_MEDIA = "cleanupMedia";

    /**
     * Contains all completed downloads that have not been included in the report yet.
     */
    private final List<DownloadStatus> reportQueue;
    private final ExecutorService syncExecutor;
    private final CompletionService<Downloader> downloadExecutor;
    private final DownloadRequester requester;
    private DownloadServiceNotification notificationManager;
    private final NewEpisodesNotification newEpisodesNotification;

    /**
     * Currently running downloads.
     */
    private final List<Downloader> downloads;

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
    private ScheduledFuture<?> downloadPostFuture;
    private static final int SCHED_EX_POOL_SIZE = 1;
    private final ScheduledThreadPoolExecutor schedExecutor;
    private static DownloaderFactory downloaderFactory = new DefaultDownloaderFactory();

    private final IBinder mBinder = new LocalBinder();

    private class LocalBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    public DownloadService() {
        reportQueue = Collections.synchronizedList(new ArrayList<>());
        downloads = Collections.synchronizedList(new ArrayList<>());
        numberOfDownloads = new AtomicInteger(0);
        requester = DownloadRequester.getInstance();
        newEpisodesNotification = new NewEpisodesNotification();

        syncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SyncThread");
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
        // Must be the first runnable in syncExecutor
        syncExecutor.execute(newEpisodesNotification::loadCountersBeforeRefresh);

        Log.d(TAG, "parallel downloads: " + UserPreferences.getParallelDownloads());
        downloadExecutor = new ExecutorCompletionService<>(
                Executors.newFixedThreadPool(UserPreferences.getParallelDownloads(),
                        r -> {
                            Thread t = new Thread(r, "DownloadThread");
                            t.setPriority(Thread.MIN_PRIORITY);
                            return t;
                        }
                )
        );
        schedExecutor = new ScheduledThreadPoolExecutor(SCHED_EX_POOL_SIZE,
                r -> {
                    Thread t = new Thread(r, "DownloadSchedExecutorThread");
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                }, (r, executor) -> Log.w(TAG, "SchedEx rejected submission of new task")
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getParcelableArrayListExtra(EXTRA_REQUESTS) != null) {
            Notification notification = notificationManager.updateNotifications(
                    requester.getNumberOfDownloads(), downloads);
            startForeground(R.id.notification_downloading, notification);
            syncExecutor.execute(() -> onDownloadQueued(intent));
        } else if (numberOfDownloads.get() == 0) {
            stopForeground(true);
            stopSelf();
        } else {
            Log.d(TAG, "onStartCommand: Unknown intent");
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Service started");
        isRunning = true;
        handler = new Handler(Looper.getMainLooper());
        notificationManager = new DownloadServiceNotification(this);

        IntentFilter cancelDownloadReceiverFilter = new IntentFilter();
        cancelDownloadReceiverFilter.addAction(ACTION_CANCEL_ALL_DOWNLOADS);
        cancelDownloadReceiverFilter.addAction(ACTION_CANCEL_DOWNLOAD);
        registerReceiver(cancelDownloadReceiver, cancelDownloadReceiverFilter);

        downloadCompletionThread.start();

        Notification notification = notificationManager.updateNotifications(
                requester.getNumberOfDownloads(), downloads);
        startForeground(R.id.notification_downloading, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
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

        EventBus.getDefault().postSticky(DownloadEvent.refresh(Collections.emptyList()));

        downloadCompletionThread.interrupt();
        try {
            downloadCompletionThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        cancelNotificationUpdater();
        syncExecutor.shutdown();
        schedExecutor.shutdown();
        if (downloadPostFuture != null) {
            downloadPostFuture.cancel(true);
        }
        unregisterReceiver(cancelDownloadReceiver);

        stopForeground(true);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(R.id.notification_downloading);

        // if this was the initial gpodder sync, i.e. we just synced the feeds successfully,
        // it is now time to sync the episode actions
        SyncService.sync(this);

        // start auto download in case anything new has shown up
        DBTasks.autodownloadUndownloadedItems(getApplicationContext());
    }

    private final Thread downloadCompletionThread = new Thread("DownloadCompletionThread") {
        private static final String TAG = "downloadCompletionThd";

        @Override
        public void run() {
            Log.d(TAG, "downloadCompletionThread was started");
            while (!isInterrupted()) {
                try {
                    Downloader downloader = downloadExecutor.take().get();
                    Log.d(TAG, "Received 'Download Complete' - message.");

                    if (downloader.getResult().isSuccessful()) {
                        syncExecutor.execute(() -> {
                            handleSuccessfulDownload(downloader);
                            removeDownload(downloader);
                            numberOfDownloads.decrementAndGet();
                            queryDownloadsAsync();
                        });
                    } else {
                        handleFailedDownload(downloader);
                        removeDownload(downloader);
                        numberOfDownloads.decrementAndGet();
                        queryDownloadsAsync();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "DownloadCompletionThread was interrupted");
                    return;
                } catch (ExecutionException e) {
                    Log.e(TAG, "ExecutionException in DownloadCompletionThread: " + e.getMessage());
                    return;
                }
            }
            Log.d(TAG, "End of downloadCompletionThread");
        }
    };

    private void handleSuccessfulDownload(Downloader downloader) {
        DownloadRequest request = downloader.getDownloadRequest();
        DownloadStatus status = downloader.getResult();
        final int type = status.getFeedfileType();

        if (type == Feed.FEEDFILETYPE_FEED) {
            Log.d(TAG, "Handling completed Feed Download");
            FeedSyncTask task = new FeedSyncTask(DownloadService.this, request);
            boolean success = task.run();

            if (success) {
                // we create a 'successful' download log if the feed's last refresh failed
                List<DownloadStatus> log = DBReader.getFeedDownloadLog(request.getFeedfileId());
                if (log.size() > 0 && !log.get(0).isSuccessful()) {
                    saveDownloadStatus(task.getDownloadStatus());
                }
                if (request.getFeedfileId() != 0 && !request.isInitiatedByUser()) {
                    // Was stored in the database before and not initiated manually
                    newEpisodesNotification.showIfNeeded(DownloadService.this, task.getSavedFeed());
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
                DownloadRequester.getInstance().download(DownloadService.this, downloader.getDownloadRequest());
            } else {
                Log.e(TAG, "Download failed");
                saveDownloadStatus(status);
                syncExecutor.execute(new FailedDownloadHandler(downloader.getDownloadRequest()));

                if (type == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                    FeedItem item = getFeedItemFromId(status.getFeedfileId());
                    if (item == null) {
                        return;
                    }
                    boolean unknownHost = status.getReason() == DownloadError.ERROR_UNKNOWN_HOST;
                    boolean unsupportedType = status.getReason() == DownloadError.ERROR_UNSUPPORTED_TYPE;
                    boolean wrongSize = status.getReason() == DownloadError.ERROR_IO_WRONG_SIZE;

                    if (! (unknownHost || unsupportedType || wrongSize)) {
                        try {
                            DBWriter.saveFeedItemAutoDownloadFailed(item).get();
                        } catch (ExecutionException | InterruptedException e) {
                            Log.d(TAG, "Ignoring exception while setting item download status");
                            e.printStackTrace();
                        }
                    }
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
                    DownloadRequest request = d.getDownloadRequest();
                    DownloadRequester.getInstance().removeDownload(request);

                    FeedItem item = getFeedItemFromId(request.getFeedfileId());
                    if (item != null) {
                        // undo enqueue upon cancel
                        if (request.isMediaEnqueued()) {
                            Log.v(TAG, "Undoing enqueue upon cancelling download");
                            try {
                                DBWriter.removeQueueItem(getApplicationContext(), false, item).get();
                            } catch (Throwable t) {
                                Log.e(TAG, "Unexpected exception during undoing enqueue upon cancel", t);
                            }
                        }
                        EventBus.getDefault().post(FeedItemEvent.updated(item));
                    }
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
        List<DownloadRequest> requests = intent.getParcelableArrayListExtra(EXTRA_REQUESTS);
        if (requests == null) {
            throw new IllegalArgumentException(
                    "ACTION_ENQUEUE_DOWNLOAD intent needs request extra");
        }
        boolean cleanupMedia = intent.getBooleanExtra(EXTRA_CLEANUP_MEDIA, false);
        Log.d(TAG, "Received enqueue request. #requests=" + requests.size()
                + ", cleanupMedia=" + cleanupMedia);

        if (cleanupMedia) {
            UserPreferences.getEpisodeCleanupAlgorithm()
                    .makeRoomForEpisodes(getApplicationContext(), requests.size());
        }

        // #2448: First, add to-download items to the queue before actual download
        // so that the resulting queue order is the same as when download is clicked
        List<? extends FeedItem> itemsEnqueued;
        try {
            itemsEnqueued = enqueueFeedItems(requests);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception during enqueue before downloads. Abort download", e);
            return;
        }

        for (DownloadRequest request : requests) {
            onDownloadQueued(request, itemsEnqueued);
        }
    }

    private List<? extends FeedItem> enqueueFeedItems(@NonNull List<? extends DownloadRequest> requests)
        throws Exception {
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

        return DBTasks.enqueueFeedItemsToDownload(getApplicationContext(), feedItems);
    }

    private void onDownloadQueued(@NonNull DownloadRequest request,
                                  @NonNull List<? extends FeedItem> itemsEnqueued) {
        writeFileUrl(request);

        Downloader downloader = downloaderFactory.create(request);
        if (downloader != null) {
            numberOfDownloads.incrementAndGet();

            if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                    && isEnqueued(request, itemsEnqueued)) {
                request.setMediaEnqueued(true);
            }
            handler.post(() -> {
                downloads.add(downloader);
                downloadExecutor.submit(downloader);
                postDownloaders();
            });
        }
        handler.post(this::queryDownloads);
    }

    private static boolean isEnqueued(@NonNull DownloadRequest request,
                                      @NonNull List<? extends FeedItem> itemsEnqueued) {
        if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            final long mediaId = request.getFeedfileId();
            for (FeedItem item : itemsEnqueued) {
                if (item.getMedia() != null && item.getMedia().getId() == mediaId) {
                    return true;
                }
            }
        }
        return false;
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
     * Calls query downloads on the services main thread. This method should be used instead of queryDownloads if it is
     * used from a thread other than the main thread.
     */
    private void queryDownloadsAsync() {
        handler.post(DownloadService.this::queryDownloads);
    }

    /**
     * Check if there's something else to download, otherwise stop.
     */
    private void queryDownloads() {
        Log.d(TAG, numberOfDownloads.get() + " downloads left");

        if (numberOfDownloads.get() <= 0 && DownloadRequester.getInstance().hasNoDownloads()) {
            Log.d(TAG, "Number of downloads is " + numberOfDownloads.get() + ", attempting shutdown");
            stopForeground(true);
            stopSelf();
            if (notificationUpdater != null) {
                notificationUpdater.run();
            } else {
                Log.d(TAG, "Skipping notification update");
            }
        } else {
            setupNotificationUpdater();
            Notification notification = notificationManager.updateNotifications(
                    requester.getNumberOfDownloads(), downloads);
            startForeground(R.id.notification_downloading, notification);
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
    private void setupNotificationUpdater() {
        if (notificationUpdater == null) {
            Log.d(TAG, "Setting up notification updater");
            notificationUpdater = new NotificationUpdater();
            notificationUpdaterFuture = schedExecutor.scheduleAtFixedRate(notificationUpdater, 1, 1, TimeUnit.SECONDS);
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
            Notification n = notificationManager.updateNotifications(requester.getNumberOfDownloads(), downloads);
            if (n != null) {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(R.id.notification_downloading, n);
                Log.d(TAG, "Download progress notification was posted");
            }
        }
    }

    private void postDownloaders() {
        new PostDownloaderTask(downloads).run();

        if (downloadPostFuture == null) {
            downloadPostFuture = schedExecutor.scheduleAtFixedRate(
                    new PostDownloaderTask(downloads), 1, 1, TimeUnit.SECONDS);
        }
    }
}
