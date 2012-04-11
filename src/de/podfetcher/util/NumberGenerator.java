package de.podfetcher.util;

import java.util.Random;
import android.util.Log;

/** Utility class for creating large random numbers */
public class NumberGenerator {
    private static final String TAG = "NumberGenerator";

    public static long generateLong(String strSeed) {
        long seed = (long) strSeed.hashCode();
        Log.d(TAG, "Taking " + seed + " as seed.");
        return new Random(seed).nextLong();
    }
}
