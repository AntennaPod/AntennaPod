package de.danoeh.antennapod.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

public class SleepTimerPreferences {

    private static final String TAG = "SleepTimerPreferences";

    public static final String PREF_NAME = "SleepTimerDialog";
    private static final String PREF_VALUE = "LastValue";

    private static final String PREF_VIBRATE = "Vibrate";
    private static final String PREF_SHAKE_TO_RESET = "ShakeToReset";
    private static final String PREF_AUTO_ENABLE = "AutoEnable";
    private static final String PREF_AUTO_ENABLE_FROM = "AutoEnableFrom";
    private static final String PREF_AUTO_ENABLE_TO = "AutoEnableTo";

    private static final String DEFAULT_LAST_TIMER = "15";
    private static final int DEFAULT_AUTO_ENABLE_FROM = 22;
    private static final int DEFAULT_AUTO_ENABLE_TO = 6;

    private static SharedPreferences prefs;

    /**
     * Sets up the UserPreferences class.
     *
     * @throws IllegalArgumentException if context is null
     */
    public static void init(@NonNull Context context) {
        Log.d(TAG, "Creating new instance of SleepTimerPreferences");
        SleepTimerPreferences.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void setLastTimer(String value) {
        prefs.edit().putString(PREF_VALUE, value).apply();
    }

    public static String lastTimerValue() {
        return prefs.getString(PREF_VALUE, DEFAULT_LAST_TIMER);
    }

    public static long timerMillis() {
        long value = Long.parseLong(lastTimerValue());
        return TimeUnit.MINUTES.toMillis(value);
    }

    public static void setVibrate(boolean vibrate) {
        prefs.edit().putBoolean(PREF_VIBRATE, vibrate).apply();
    }

    public static boolean vibrate() {
        return prefs.getBoolean(PREF_VIBRATE, false);
    }

    public static void setShakeToReset(boolean shakeToReset) {
        prefs.edit().putBoolean(PREF_SHAKE_TO_RESET, shakeToReset).apply();
    }

    public static boolean shakeToReset() {
        return prefs.getBoolean(PREF_SHAKE_TO_RESET, true);
    }

    public static void setAutoEnable(boolean autoEnable) {
        prefs.edit().putBoolean(PREF_AUTO_ENABLE, autoEnable).apply();
    }

    public static boolean autoEnable() {
        return prefs.getBoolean(PREF_AUTO_ENABLE, false);
    }

    public static void setAutoEnableFrom(int hourOfDay) {
        prefs.edit().putInt(PREF_AUTO_ENABLE_FROM, hourOfDay).apply();
    }

    public static int autoEnableFrom() {
        return prefs.getInt(PREF_AUTO_ENABLE_FROM, DEFAULT_AUTO_ENABLE_FROM);
    }

    public static void setAutoEnableTo(int hourOfDay) {
        prefs.edit().putInt(PREF_AUTO_ENABLE_TO, hourOfDay).apply();
    }

    public static int autoEnableTo() {
        return prefs.getInt(PREF_AUTO_ENABLE_TO, DEFAULT_AUTO_ENABLE_TO);
    }

    public static boolean isInTimeRange(int from, int to, int current) {
        // Range covers one day
        if (from < to) {
            return from <= current && current < to;
        }

        // Range covers two days
        if (from <= current) {
            return true;
        }

        return current < to;
    }
}
