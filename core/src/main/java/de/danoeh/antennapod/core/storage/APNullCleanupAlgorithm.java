package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

/**
 * A cleanup algorithm that never removes anything
 */
public class APNullCleanupAlgorithm extends EpisodeCleanupAlgorithm {

    private static final String TAG = "APNullCleanupAlgorithm";

    @Override
    public int performCleanup(Context context, int parameter) {
        // never clean anything up
        Log.i(TAG, "performCleanup: Not removing anything");
        return 0;
    }

    @Override
    public int getDefaultCleanupParameter() {
        return 0;
    }

    @Override
    public int getReclaimableItems() {
        return 0;
    }
}
