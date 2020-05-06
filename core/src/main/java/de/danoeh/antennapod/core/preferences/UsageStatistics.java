package de.danoeh.antennapod.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import java.util.Calendar;

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
    private static final long ASK_AGAIN_LATER_DELAY = 1000 * 3600 * 24 * 10; // 10 days
    private static final String SUFFIX_HIDDEN_UNTIL = "_hiddenUntil";
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
        final float movingAverage = prefs.getFloat(action.type, 0.5f);
        final long askAfter = prefs.getLong(action.type + SUFFIX_HIDDEN_UNTIL, 0);
        return Math.abs(action.value - movingAverage) < MOVING_AVERAGE_BIAS_THRESHOLD
                && Calendar.getInstance().getTimeInMillis() > askAfter;
    }

    public static void askAgainLater(StatsAction action) {
        prefs.edit().putLong(action.type + SUFFIX_HIDDEN_UNTIL,
                Calendar.getInstance().getTimeInMillis() + ASK_AGAIN_LATER_DELAY)
                .apply();
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
