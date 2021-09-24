package de.danoeh.antennapod.core.sync;

import android.content.Context;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.sync.model.EpisodeAction;

public class LockingQueueWriter {

    public static void clearQueue(Context context) {
        LockingAsyncExecutor.executeLockedAsync(new SynchronizationQueue(context)::clearQueue);
    }

    public static void enqueueFeedAdded(Context context, String downloadUrl) {
        LockingAsyncExecutor.executeLockedAsync(() -> {
            new SynchronizationQueue(context).enqueueFeedAdded(downloadUrl);
            SyncService.sync(context);
        });
    }

    public static void enqueueFeedRemoved(Context context, String downloadUrl) {
        LockingAsyncExecutor.executeLockedAsync(() -> {
            new SynchronizationQueue(context).enqueueFeedRemoved(downloadUrl);
            SyncService.sync(context);
        });
    }

    public static void enqueueEpisodeAction(Context context, EpisodeAction action) {
        LockingAsyncExecutor.executeLockedAsync(() -> {
            new SynchronizationQueue(context).enqueueEpisodeAction(action);
            SyncService.sync(context);
        });
    }

    public static void enqueueEpisodePlayed(Context context, FeedMedia media, boolean completed) {
        if (media.getItem() == null) {
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
        enqueueEpisodeAction(context, action);
    }

}
