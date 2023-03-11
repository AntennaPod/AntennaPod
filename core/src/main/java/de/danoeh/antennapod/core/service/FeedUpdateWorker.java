package de.danoeh.antennapod.core.service;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.core.ClientConfigurator;
import de.danoeh.antennapod.core.feed.LocalFeedUpdater;
import de.danoeh.antennapod.core.service.download.DefaultDownloaderFactory;
import de.danoeh.antennapod.core.service.download.DownloadRequestCreator;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.service.download.NewEpisodesNotification;
import de.danoeh.antennapod.core.service.download.handler.FeedSyncTask;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;

import java.util.Iterator;
import java.util.List;

public class FeedUpdateWorker extends Worker {
    private static final String TAG = "FeedUpdateWorker";

    private final NewEpisodesNotification newEpisodesNotification;

    public FeedUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        newEpisodesNotification = new NewEpisodesNotification();
    }

    @Override
    @NonNull
    public Result doWork() {
        ClientConfigurator.initialize(getApplicationContext());
        newEpisodesNotification.loadCountersBeforeRefresh();

        if (NetworkUtils.networkAvailable() && NetworkUtils.isFeedRefreshAllowed()) {
            refreshFeeds();
            return Result.success();
        } else {
            Log.d(TAG, "Blocking automatic update: no wifi available / no mobile updates allowed");
            return Result.retry();
        }
    }

    private void refreshFeeds() {
        List<Feed> toUpdate = DBReader.getFeedList();
        Iterator<Feed> itr = toUpdate.iterator();
        while (itr.hasNext()) {
            Feed feed = itr.next();
            if (!feed.getPreferences().getKeepUpdated()) {
                itr.remove();
            }
        }

        for (Feed feed : toUpdate) {
            if (isStopped()) {
                return;
            }
            try {
                if (feed.isLocalFeed()) {
                    LocalFeedUpdater.updateFeed(feed, getApplicationContext(), null);
                } else {
                    refreshFeed(feed);
                }
            } catch (Exception e) {
                DBWriter.setFeedLastUpdateFailed(feed.getId(), true);
                DownloadStatus status = new DownloadStatus(feed, feed.getTitle(),
                        DownloadError.ERROR_IO_ERROR, false, e.getMessage(), true);
                DBWriter.addDownloadStatus(status);
            }
        }
    }

    void refreshFeed(Feed feed) throws Exception {
        DownloadRequest.Builder builder = DownloadRequestCreator.create(feed);
        builder.setForce(feed.hasLastUpdateFailed());
        DownloadRequest request = builder.build();

        Downloader downloader = new DefaultDownloaderFactory().create(request);
        if (downloader == null) {
            throw new Exception("Unable to create downloader");
        }

        downloader.call();

        if (!downloader.getResult().isSuccessful()) {
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
        if (!request.isInitiatedByUser()) {
            // Was stored in the database before and not initiated manually
            newEpisodesNotification.showIfNeeded(getApplicationContext(), feedSyncTask.getSavedFeed());
        }
        if (downloader.permanentRedirectUrl != null) {
            DBWriter.updateFeedDownloadURL(request.getSource(), downloader.permanentRedirectUrl);
        } else if (feedSyncTask.getRedirectUrl() != null) {
            DBWriter.updateFeedDownloadURL(request.getSource(), feedSyncTask.getRedirectUrl());
        }
    }
}
