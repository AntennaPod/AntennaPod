package de.danoeh.antennapod.core.service;

import android.app.Notification;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import de.danoeh.antennapod.core.ClientConfigurator;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.LocalFeedUpdater;
import de.danoeh.antennapod.core.service.download.DefaultDownloaderFactory;
import de.danoeh.antennapod.core.service.download.DownloadRequestCreator;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.service.download.NewEpisodesNotification;
import de.danoeh.antennapod.core.service.download.handler.FeedSyncTask;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.download.FeedUpdateManager;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
        ClientConfigurator.initialize(getApplicationContext());
        newEpisodesNotification.loadCountersBeforeRefresh();

        List<Feed> toUpdate;
        long feedId = getInputData().getLong(FeedUpdateManager.EXTRA_FEED_ID, -1);
        boolean allAreLocal = true;
        boolean force = false;
        if (feedId == -1) { // Update all
            toUpdate = DBReader.getFeedList();
            Iterator<Feed> itr = toUpdate.iterator();
            while (itr.hasNext()) {
                Feed feed = itr.next();
                if (!feed.getPreferences().getKeepUpdated()) {
                    itr.remove();
                }
                if (!feed.isLocalFeed()) {
                    allAreLocal = false;
                }
            }
            Collections.shuffle(toUpdate); // If the worker gets cancelled early, every feed has a chance to be updated
        } else {
            Feed feed = DBReader.getFeed(feedId);
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

        if (!getInputData().getBoolean(FeedUpdateManager.EXTRA_EVEN_ON_MOBILE, false) && !allAreLocal) {
            if (!NetworkUtils.networkAvailable() || !NetworkUtils.isFeedRefreshAllowed()) {
                Log.d(TAG, "Blocking automatic update");
                return Result.retry();
            }
        }
        refreshFeeds(toUpdate,  force);

        notificationManager.cancel(R.id.notification_updating_feeds);
        DBTasks.autodownloadUndownloadedItems(getApplicationContext());
        return Result.success();
    }

    @NonNull
    private Notification createNotification(@Nullable List<Feed> toUpdate) {
        Context context = getApplicationContext();
        String contentText = "";
        String bigText = "";
        if (toUpdate != null) {
            contentText = context.getResources().getQuantityString(R.plurals.downloads_left,
                    toUpdate.size(), toUpdate.size());
            bigText = Stream.of(toUpdate).map(feed -> "• " + feed.getTitle()).collect(Collectors.joining("\n"));
        }
        return new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_DOWNLOADING)
                .setContentTitle(context.getString(R.string.download_notification_title_feeds))
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setSmallIcon(R.drawable.ic_notification_sync)
                .setOngoing(true)
                .addAction(R.drawable.ic_cancel, context.getString(R.string.cancel_label),
                        WorkManager.getInstance(context).createCancelPendingIntent(getId()))
                .build();
    }

    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        return Futures.immediateFuture(new ForegroundInfo(R.id.notification_updating_feeds, createNotification(null)));
    }

    private void refreshFeeds(List<Feed> toUpdate, boolean force) {
        while (!toUpdate.isEmpty()) {
            if (isStopped()) {
                return;
            }
            notificationManager.notify(R.id.notification_updating_feeds, createNotification(toUpdate));
            Feed feed = toUpdate.get(0);
            try {
                if (feed.isLocalFeed()) {
                    LocalFeedUpdater.updateFeed(feed, getApplicationContext(), null);
                } else {
                    refreshFeed(feed, force);
                }
            } catch (Exception e) {
                DBWriter.setFeedLastUpdateFailed(feed.getId(), true);
                DownloadResult status = new DownloadResult(feed, feed.getTitle(),
                        DownloadError.ERROR_IO_ERROR, false, e.getMessage());
                DBWriter.addDownloadStatus(status);
            }
            toUpdate.remove(0);
        }
    }

    void refreshFeed(Feed feed, boolean force) throws Exception {
        boolean nextPage = getInputData().getBoolean(FeedUpdateManager.EXTRA_NEXT_PAGE, false)
                && feed.getNextPageLink() != null;
        if (nextPage) {
            feed.setPageNr(feed.getPageNr() + 1);
        }
        DownloadRequest.Builder builder = DownloadRequestCreator.create(feed);
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
                return;
            }
            DBWriter.setFeedLastUpdateFailed(request.getFeedfileId(), true);
            DBWriter.addDownloadStatus(downloader.getResult());
            return;
        }

        FeedSyncTask feedSyncTask = new FeedSyncTask(getApplicationContext(), request);
        boolean success = feedSyncTask.run();

        if (!success) {
            DBWriter.setFeedLastUpdateFailed(request.getFeedfileId(), true);
            DBWriter.addDownloadStatus(feedSyncTask.getDownloadStatus());
            return;
        }

        if (request.getFeedfileId() == 0) {
            return; // No download logs for new subscriptions
        }
        // we create a 'successful' download log if the feed's last refresh failed
        List<DownloadResult> log = DBReader.getFeedDownloadLog(request.getFeedfileId());
        if (log.size() > 0 && !log.get(0).isSuccessful()) {
            DBWriter.addDownloadStatus(feedSyncTask.getDownloadStatus());
        }
        newEpisodesNotification.showIfNeeded(getApplicationContext(), feedSyncTask.getSavedFeed());
        if (downloader.permanentRedirectUrl != null) {
            DBWriter.updateFeedDownloadURL(request.getSource(), downloader.permanentRedirectUrl);
        } else if (feedSyncTask.getRedirectUrl() != null
                && !feedSyncTask.getRedirectUrl().equals(request.getSource())) {
            DBWriter.updateFeedDownloadURL(request.getSource(), feedSyncTask.getRedirectUrl());
        }
    }
}
