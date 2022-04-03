package de.danoeh.antennapod.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

/**
 * Collects statistics about the app usage. The statistics are used to allow on-demand configuration:
 * "Looks like you stream a lot. Do you want to toggle the 'Prefer streaming' setting?".
 * The data is only stored locally on the device. It is NOT used for analytics/tracking.
 * A private instance of this class must first be instantiated via
 * init() or otherwise every public method will throw an Exception
 * when called.
 */
public class UsageStatistics {
    private UsageStatistics() {

    }

    private static final String PREF_DB_NAME = "UsageStatistics";
    private static final float MOVING_AVERAGE_WEIGHT = 0.8f;
    private static final float MOVING_AVERAGE_BIAS_THRESHOLD = 0.1f;
    private static final String SUFFIX_HIDDEN = "_hidden";
    private static SharedPreferences prefs;

    public static final StatsAction ACTION_STREAM = new StatsAction("downloadVsStream", 0);
    public static final StatsAction ACTION_DOWNLOAD = new StatsAction("downloadVsStream", 1);

    /**
     * Sets up the UsageStatistics class.
     *
     * @throws IllegalArgumentException if context is null
     */
    public static void init(@NonNull Context context) {
        prefs = context.getSharedPreferences(PREF_DB_NAME, Context.MODE_PRIVATE);
    }

    public static void logAction(StatsAction action) {
        int numExecutions = prefs.getInt(action.type + action.value, 0);
        float movingAverage = prefs.getFloat(action.type, 0.5f);
        prefs.edit()
                .putInt(action.type + action.value, numExecutions + 1)
                .putFloat(action.type, MOVING_AVERAGE_WEIGHT * movingAverage
                        + (1 - MOVING_AVERAGE_WEIGHT) * action.value)
                .apply();
    }

    public static boolean hasSignificantBiasTo(StatsAction action) {
        if (prefs.getBoolean(action.type + SUFFIX_HIDDEN, false)) {
            return false;
        } else {
            final float movingAverage = prefs.getFloat(action.type, 0.5f);
            return Math.abs(action.value - movingAverage) < MOVING_AVERAGE_BIAS_THRESHOLD;
        }
    }

    public static void doNotAskAgain(StatsAction action) {
        prefs.edit().putBoolean(action.type + SUFFIX_HIDDEN, true).apply();
    }

    public static final class StatsAction {
        public final String type;
        public final int value;

        public StatsAction(String type, int value) {
            this.type = type;
            this.value = value;
        }
    }
}
