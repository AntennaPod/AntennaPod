package de.danoeh.antennapod.net.sync.service;

import android.content.Context;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import de.danoeh.antennapod.event.SyncServiceEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeAction;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

public class SynchronizationQueueImpl extends SynchronizationQueue {
    private static final String WORK_ID_SYNC = "SyncServiceWorkId";
    private final Context context;

    public SynchronizationQueueImpl(Context context) {
        this.context = context;
    }

    public void sync() {
        OneTimeWorkRequest workRequest = getWorkRequest().build();
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_ID_SYNC, ExistingWorkPolicy.REPLACE, workRequest);
    }

    public void syncIfNotSyncedRecently() {
        if (System.currentTimeMillis() - SynchronizationSettings.getLastSyncAttempt() > 1000 * 60 * 10) {
            sync();
        }
    }

    public void syncImmediately() {
        OneTimeWorkRequest workRequest = getWorkRequest()
                .setInitialDelay(0L, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_ID_SYNC, ExistingWorkPolicy.REPLACE, workRequest);
    }

    public void fullSync() {
        LockingAsyncExecutor.executeLockedAsync(() -> {
            SynchronizationSettings.resetTimestamps();
            syncImmediately();
        });
    }

    private static OneTimeWorkRequest.Builder getWorkRequest() {
        Constraints.Builder constraints = new Constraints.Builder();
        if (UserPreferences.isAllowMobileSync()) {
            constraints.setRequiredNetworkType(NetworkType.CONNECTED);
        } else {
            constraints.setRequiredNetworkType(NetworkType.UNMETERED);
        }

        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(SyncService.class)
                .setConstraints(constraints.build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES);

        if (SyncService.isCurrentlyActive()) {
            // Debounce: don't start sync again immediately after it was finished.
            builder.setInitialDelay(2L, TimeUnit.MINUTES);
        } else {
            // Give it some time, so other possible actions can be queued.
            builder.setInitialDelay(20L, TimeUnit.SECONDS);
            EventBus.getDefault().postSticky(new SyncServiceEvent(R.string.sync_status_started));
        }
        return builder;
    }

    public void clear() {
        LockingAsyncExecutor.executeLockedAsync(new SynchronizationQueueStorage(context)::clearQueue);
    }

    public void enqueueFeedAdded(String downloadUrl) {
        if (!SynchronizationSettings.isProviderConnected()) {
            return;
        }
        LockingAsyncExecutor.executeLockedAsync(() -> {
            new SynchronizationQueueStorage(context).enqueueFeedAdded(downloadUrl);
            sync();
        });
    }

    public void enqueueFeedRemoved(String downloadUrl) {
        if (!SynchronizationSettings.isProviderConnected()) {
            return;
        }
        LockingAsyncExecutor.executeLockedAsync(() -> {
            new SynchronizationQueueStorage(context).enqueueFeedRemoved(downloadUrl);
            sync();
        });
    }

    public void enqueueEpisodeAction(EpisodeAction action) {
        if (!SynchronizationSettings.isProviderConnected()) {
            return;
        }
        LockingAsyncExecutor.executeLockedAsync(() -> {
            new SynchronizationQueueStorage(context).enqueueEpisodeAction(action);
            sync();
        });
    }

    public void enqueueEpisodePlayed(FeedMedia media, boolean completed) {
        if (!SynchronizationSettings.isProviderConnected()) {
            return;
        }
        if (media.getItem() == null || media.getItem().getFeed().isLocalFeed()
                || media.getItem().getFeed().getState() != Feed.STATE_SUBSCRIBED) {
            return;
        }
        if (media.getStartPosition() < 0 || (!completed && media.getStartPosition() >= media.getPosition())) {
            return;
        }
        EpisodeAction action = new EpisodeAction.Builder(media.getItem(), EpisodeAction.PLAY)
                .currentTimestamp()
                .started(media.getStartPosition() / 1000)
                .position((completed ? media.getDuration() : media.getPosition()) / 1000)
                .total(media.getDuration() / 1000)
                .build();
        enqueueEpisodeAction(action);
    }
}
