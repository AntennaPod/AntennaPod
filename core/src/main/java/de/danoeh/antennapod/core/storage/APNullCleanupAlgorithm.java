package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

/**
 * A cleanup algorithm that never removes anything
 */
public class APNullCleanupAlgorithm implements EpisodeCleanupAlgorithm<Integer> {

    private static final String TAG = "APNullCleanupAlgorithm";

    @Override
    public int performCleanup(Context context, Integer parameter) {
        // never clean anything up
        Log.i(TAG, "performCleanup: Not removing anything");
        return 0;
    }

    @Override
    public Integer getDefaultCleanupParameter() {
        return 0;
    }
}
