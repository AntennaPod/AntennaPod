package de.danoeh.antennapod.core.storage;

import android.content.Context;

/**
 * A cleanup algorithm that removes any item that isn't in the queue and isn't a favorite
 * but only if space is needed.
 */
public class APQueueCleanupAlgorithm implements EpisodeCleanupAlgorithm<Integer> {
    @Override
    public int performCleanup(Context context, Integer parameter) {
        return 0;
    }

    @Override
    public Integer getDefaultCleanupParameter() {
        return 0;
    }
}
