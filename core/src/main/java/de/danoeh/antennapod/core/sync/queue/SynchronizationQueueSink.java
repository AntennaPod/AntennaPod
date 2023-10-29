package de.danoeh.antennapod.core.sync.queue;

import android.content.Context;

import de.danoeh.antennapod.core.sync.LockingAsyncExecutor;
import de.danoeh.antennapod.core.sync.SynchronizationSettings;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.model.EpisodeAction;

public class SynchronizationQueueSink {
    // To avoid a dependency loop of every class to SyncService, and from SyncService back to every class.
    private static Runnable serviceStarterImpl = () -> { };

    public static void setServiceStarterImpl(Runnable serviceStarter) {
        serviceStarterImpl = serviceStarter;
    }

    public static void syncNow() {
        serviceStarterImpl.run();
    }

    public static void syncNowIfNotSyncedRecently() {
        if (System.currentTimeMillis() - SynchronizationSettings.getLastSyncAttempt() > 1000 * 60 * 10) {
            syncNow();
        }
    }

    public static void clearQueue(Context context) {
        LockingAsyncExecutor.executeLockedAsync(new SynchronizationQueueStorage(context)::clearQueue);
    }

    public static void enqueueFeedAddedIfSynchronizationIsActive(Context context, String downloadUrl) {
        if (!SynchronizationSettings.isProviderConnected()) {
            return;
        }
        LockingAsyncExecutor.executeLockedAsync(() -> {
            new SynchronizationQueueStorage(context).enqueueFeedAdded(downloadUrl);
            syncNow();
        });
    }

    public static void enqueueFeedRemovedIfSynchronizationIsActive(Context context, String downloadUrl) {
        if (!SynchronizationSettings.isProviderConnected()) {
            return;
        }
        LockingAsyncExecutor.executeLockedAsync(() -> {
            new SynchronizationQueueStorage(context).enqueueFeedRemoved(downloadUrl);
            syncNow();
        });
    }

    public static void enqueueEpisodeActionIfSynchronizationIsActive(Context context, EpisodeAction action) {
        if (!SynchronizationSettings.isProviderConnected()) {
            return;
        }
        LockingAsyncExecutor.executeLockedAsync(() -> {
            new SynchronizationQueueStorage(context).enqueueEpisodeAction(action);
            syncNow();
        });
    }

    public static void enqueueEpisodePlayedIfSynchronizationIsActive(Context context, FeedMedia media,
                                                                     boolean completed) {
        if (!SynchronizationSettings.isProviderConnected()) {
            return;
        }
        if (media.getItem() == null || media.getItem().getFeed().isLocalFeed()) {
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
        enqueueEpisodeActionIfSynchronizationIsActive(context, action);
    }

}
