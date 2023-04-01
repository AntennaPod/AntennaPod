package de.danoeh.antennapod.core.service;

import android.app.Notification;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import de.danoeh.antennapod.core.ClientConfigurator;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.LocalFeedUpdater;
import de.danoeh.antennapod.core.service.download.DefaultDownloaderFactory;
import de.danoeh.antennapod.core.service.download.DownloadRequestCreator;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.service.download.NewEpisodesNotification;
import de.danoeh.antennapod.core.service.download.handler.FeedSyncTask;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.download.FeedUpdateManager;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadStatus;
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

        if (!NetworkUtils.networkAvailable() || !NetworkUtils.isFeedRefreshAllowed()) {
            Log.d(TAG, "Blocking automatic update: no wifi available / no mobile updates allowed");
            return Result.retry();
        }

        List<Feed> toUpdate;
        long feedId = getInputData().getLong(FeedUpdateManager.EXTRA_FEED_ID, -1);
        if (feedId == -1) { // Update all
            toUpdate = DBReader.getFeedList();
            Iterator<Feed> itr = toUpdate.iterator();
            while (itr.hasNext()) {
                Feed feed = itr.next();
                if (!feed.getPreferences().getKeepUpdated()) {
                    itr.remove();
                }
            }
            Collections.shuffle(toUpdate); // If the worker gets cancelled early, every feed has a chance to be updated
            refreshFeeds(toUpdate, false);
        } else {
            toUpdate = new ArrayList<>();
            Feed feed = DBReader.getFeed(feedId);
            if (feed == null) {
                return Result.success();
            }
            toUpdate.add(feed);
            refreshFeeds(toUpdate, true);
        }
        notificationManager.cancel(R.id.notification_updating_feeds);
        return Result.success();
    }

    @NonNull
    private Notification createNotification(List<Feed> toUpdate) {
        Context context = getApplicationContext();
        String contentText = context.getResources().getQuantityString(R.plurals.downloads_left,
                toUpdate.size(), toUpdate.size());
        String bigText = Stream.of(toUpdate).map(feed -> "• " + feed.getTitle()).collect(Collectors.joining("\n"));
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
                DownloadStatus status = new DownloadStatus(feed, feed.getTitle(),
                        DownloadError.ERROR_IO_ERROR, false, e.getMessage(), true);
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
            if (downloader.getResult().isCancelled()) {
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
        List<DownloadStatus> log = DBReader.getFeedDownloadLog(request.getFeedfileId());
        if (log.size() > 0 && !log.get(0).isSuccessful()) {
            DBWriter.addDownloadStatus(feedSyncTask.getDownloadStatus());
        }
        newEpisodesNotification.showIfNeeded(getApplicationContext(), feedSyncTask.getSavedFeed());
        if (downloader.permanentRedirectUrl != null) {
            DBWriter.updateFeedDownloadURL(request.getSource(), downloader.permanentRedirectUrl);
        } else if (feedSyncTask.getRedirectUrl() != null) {
            DBWriter.updateFeedDownloadURL(request.getSource(), feedSyncTask.getRedirectUrl());
        }
    }
}
