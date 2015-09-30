package de.danoeh.antennapod.core.storage;

import android.content.Context;

/**
 * A cleanup algorithm that never removes anything
 */
public class APNullCleanupAlgorithm implements EpisodeCleanupAlgorithm<Integer> {
    @Override
    public int performCleanup(Context context, Integer parameter) {
        // never clean anything up
        return 0;
    }

    @Override
    public Integer getDefaultCleanupParameter() {
        return 0;
    }
}
