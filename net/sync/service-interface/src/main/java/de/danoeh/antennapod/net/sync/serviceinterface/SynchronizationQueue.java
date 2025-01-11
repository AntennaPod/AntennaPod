package de.danoeh.antennapod.net.sync.serviceinterface;

import de.danoeh.antennapod.model.feed.FeedMedia;

public abstract class SynchronizationQueue {
    private static SynchronizationQueue instance;

    public static SynchronizationQueue getInstance() {
        return instance;
    }

    public static void setInstance(SynchronizationQueue instance) {
        SynchronizationQueue.instance = instance;
    }

    /**
     * Sync bundled events after some delay to avoid spamming the sync server.
     */
    public abstract void sync();

    public abstract void syncImmediately();

    public abstract void fullSync();

    public abstract void syncIfNotSyncedRecently();

    public abstract void clear();

    public abstract void enqueueFeedAdded(String downloadUrl);

    public abstract void enqueueFeedRemoved(String downloadUrl);

    public abstract void enqueueEpisodeAction(EpisodeAction action);

    public abstract void enqueueEpisodePlayed(FeedMedia media, boolean completed);
}
