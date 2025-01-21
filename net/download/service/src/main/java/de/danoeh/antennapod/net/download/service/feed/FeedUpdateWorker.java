package de.danoeh.antennapod.net.download.service.feed;

import android.Manifest;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import de.danoeh.antennapod.net.download.service.R;
import de.danoeh.antennapod.net.download.service.feed.local.LocalFeedUpdater;
import de.danoeh.antennapod.net.download.service.feed.remote.DefaultDownloaderFactory;
import de.danoeh.antennapod.net.download.service.feed.remote.Downloader;
import de.danoeh.antennapod.net.download.service.feed.remote.FeedParserTask;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequestCreator;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.download.DownloadRequest;

import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequestBuilder;
import de.danoeh.antennapod.parser.feed.FeedHandlerResult;
import de.danoeh.antennapod.storage.database.NonSubscribedFeedsCleaner;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeedUpdateWorker extends Worker {
    private static final String TAG = "FeedUpdateWorker";

    private final NewEpisodesNotification newEpisodesNotification;
    private final NotificationManagerCompat notificationManager;

    public FeedUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        newEpisodesNotification = new NewEpisodesNotification();
        notificationManager = NotificationManagerCompat.from(context);
    }

    @Override
    @NonNull
    public Result doWork() {
        newEpisodesNotification.loadCountersBeforeRefresh();

        List<Feed> toUpdate;
        long feedId = getInputData().getLong(FeedUpdateManagerImpl.EXTRA_FEED_ID, -1);
        boolean allAreLocal = true;
        boolean force = false;
        if (feedId == -1) { // Update all
            toUpdate = DBReader.getFeedList();
            Iterator<Feed> itr = toUpdate.iterator();
            while (itr.hasNext()) {
                Feed feed = itr.next();
                if (!feed.getPreferences().getKeepUpdated() || feed.getState() != Feed.STATE_SUBSCRIBED) {
                    itr.remove();
                    continue;
                }
                if (!feed.isLocalFeed()) {
                    allAreLocal = false;
                }
            }
            Collections.shuffle(toUpdate); // If the worker gets cancelled early, every feed has a chance to be updated
        } else {
            Feed feed = DBReader.getFeed(feedId, false, 0, Integer.MAX_VALUE);
            if (feed == null) {
                return Result.success();
            }
            if (!feed.isLocalFeed()) {
                allAreLocal = false;
            }
            toUpdate = new ArrayList<>();
            toUpdate.add(feed); // Needs to be updatable, so no singletonList
            force = true;
        }

        if (!getInputData().getBoolean(FeedUpdateManagerImpl.EXTRA_EVEN_ON_MOBILE, false) && !allAreLocal) {
            if (!NetworkUtils.networkAvailable() || !NetworkUtils.isFeedRefreshAllowed()) {
                Log.d(TAG, "Blocking automatic update");
                return Result.retry();
            }
        }
        refreshFeeds(toUpdate,  force);

        NonSubscribedFeedsCleaner.deleteOldNonSubscribedFeeds(getApplicationContext());
        AutoDownloadManager.getInstance().autodownloadUndownloadedItems(getApplicationContext());
        notificationManager.cancel(R.id.notification_updating_feeds);
        SynchronizationQueue.getInstance().syncImmediately();
        return Result.success();
    }

    @NonNull
    private Notification createNotification(@Nullable List<Feed> toUpdate) {
        Context context = getApplicationContext();
        String contentText = "";
        StringBuilder bigText = new StringBuilder();
        if (toUpdate != null) {
            contentText = context.getResources().getQuantityString(R.plurals.downloads_left,
                    toUpdate.size(), toUpdate.size());
            for (int i = 0; i < toUpdate.size(); i++) {
                bigText.append("• ").append(toUpdate.get(i).getTitle());
                if (i != toUpdate.size() - 1) {
                    bigText.append("\n");
                }
            }
        }
        return new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_REFRESHING)
                .setContentTitle(context.getString(R.string.download_notification_title_feeds))
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setSmallIcon(R.drawable.ic_notification_sync)
                .setOngoing(true)
                .addAction(R.drawable.ic_notification_cancel, context.getString(R.string.cancel_label),
                        WorkManager.getInstance(context).createCancelPendingIntent(getId()))
                .build();
    }

    private void updateNotification(List<Feed> toUpdate) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(R.id.notification_updating_feeds, createNotification(toUpdate));
        }
    }

    @NonNull
    @Override
    public ListenableFuture getForegroundInfoAsync() {
        return Futures.immediateFuture(new ForegroundInfo(R.id.notification_updating_feeds, createNotification(null)));
    }

    private void refreshFeeds(List<Feed> toUpdate, boolean force) {
        List<Feed> notificationRemainingFeeds = new ArrayList<>(toUpdate);
        updateNotification(notificationRemainingFeeds);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (Feed feed : toUpdate) {
            executor.submit(() -> {
                if (isStopped()) {
                    return;
                }
                try {
                    Feed savedFeed;
                    if (feed.isLocalFeed()) {
                        savedFeed = LocalFeedUpdater.updateFeed(feed, getApplicationContext(), null);
                    } else {
                        savedFeed = refreshFeed(feed, force);
                    }
                    if (savedFeed != null) {
                        newEpisodesNotification.showIfNeeded(getApplicationContext(), savedFeed);
                    }
                } catch (Exception e) {
                    DBWriter.setFeedLastUpdateFailed(feed.getId(), true);
                    DownloadResult status = new DownloadResult(feed.getTitle(),
                            feed.getId(), Feed.FEEDFILETYPE_FEED, false,
                            DownloadError.ERROR_IO_ERROR, e.getMessage());
                    DBWriter.addDownloadStatus(status);
                }
                synchronized (notificationRemainingFeeds) {
                    notificationRemainingFeeds.remove(feed);
                    if (!notificationRemainingFeeds.isEmpty()) {
                        updateNotification(notificationRemainingFeeds);
                    }
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            //~300 years have elapsed
        }
    }

    Feed refreshFeed(Feed feed, boolean force) throws Exception {
        boolean nextPage = getInputData().getBoolean(FeedUpdateManagerImpl.EXTRA_NEXT_PAGE, false)
                && feed.getNextPageLink() != null;
        if (nextPage) {
            feed.setPageNr(feed.getPageNr() + 1);
        }
        DownloadRequestBuilder builder = DownloadRequestCreator.create(feed);
        builder.setForce(force || feed.hasLastUpdateFailed());
        if (nextPage) {
            builder.setSource(feed.getNextPageLink());
        }
        DownloadRequest request = builder.build();

        Downloader downloader = new DefaultDownloaderFactory().create(request);
        if (downloader == null) {
            throw new Exception("Unable to create downloader");
        }

        downloader.call();

        if (!downloader.getResult().isSuccessful()) {
            if (downloader.cancelled || downloader.getResult().getReason() == DownloadError.ERROR_DOWNLOAD_CANCELLED) {
                return null;
            }
            DBWriter.setFeedLastUpdateFailed(request.getFeedfileId(), true);
            DBWriter.addDownloadStatus(downloader.getResult());
            return null;
        }

        FeedParserTask parserTask = new FeedParserTask(request);
        FeedHandlerResult feedHandlerResult = parserTask.call();
        if (!parserTask.isSuccessful()) {
            DBWriter.setFeedLastUpdateFailed(request.getFeedfileId(), true);
            DBWriter.addDownloadStatus(parserTask.getDownloadStatus());
            return null;
        }
        feedHandlerResult.feed.setLastRefreshAttempt(System.currentTimeMillis());
        Feed savedFeed = FeedDatabaseWriter.updateFeed(getApplicationContext(), feedHandlerResult.feed, false);

        if (request.getFeedfileId() == 0) {
            return savedFeed; // No download logs for new subscriptions
        }
        // we create a 'successful' download log if the feed's last refresh failed
        List<DownloadResult> log = DBReader.getFeedDownloadLog(request.getFeedfileId());
        if (!log.isEmpty() && !log.get(0).isSuccessful()) {
            DBWriter.addDownloadStatus(parserTask.getDownloadStatus());
        }
        if (downloader.permanentRedirectUrl != null) {
            DBWriter.updateFeedDownloadURL(request.getSource(), downloader.permanentRedirectUrl);
        } else if (feedHandlerResult.redirectUrl != null
                && !feedHandlerResult.redirectUrl.equals(request.getSource())) {
            DBWriter.updateFeedDownloadURL(request.getSource(), feedHandlerResult.redirectUrl);
        }
        return savedFeed;
    }
}
