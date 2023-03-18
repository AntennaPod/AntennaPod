package de.danoeh.antennapod.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import java.util.concurrent.TimeUnit;

public class SleepTimerPreferences {

    private static final String TAG = "SleepTimerPreferences";

    public static final String PREF_NAME = "SleepTimerDialog";
    private static final String PREF_VALUE = "LastValue";

    private static final String PREF_VIBRATE = "Vibrate";
    private static final String PREF_SHAKE_TO_RESET = "ShakeToReset";
    private static final String PREF_AUTO_ENABLE = "AutoEnable";
    private static final String PREF_AUTO_ENABLE_TIME_BASED = "AutoEnableTimeBased";
    private static final String PREF_AUTO_ENABLE_TIME_BASED_FROM_MIN = "AutoEnableTimeBasedFromMin";
    private static final String PREF_AUTO_ENABLE_TIME_BASED_FROM_HOUR = "AutoEnableTimeBasedFromHour";
    private static final String PREF_AUTO_ENABLE_TIME_BASED_TO_MIN = "AutoEnableTimeBasedToMin";
    private static final String PREF_AUTO_ENABLE_TIME_BASED_TO_HOUR = "AutoEnableTimeBasedToHour";

    private static final String DEFAULT_VALUE = "15";

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
        return prefs.getString(PREF_VALUE, DEFAULT_VALUE);
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

    public static void setAutoEnableTimeBased(boolean autoEnableTimeBased) {
        prefs.edit().putBoolean(PREF_AUTO_ENABLE_TIME_BASED, autoEnableTimeBased).apply();
    }

    public static boolean autoEnableTimeBased() {
        return prefs.getBoolean(PREF_AUTO_ENABLE_TIME_BASED, false);
    }

    public static void setAutoEnableTimeFrom(int hourOfDay, int minute) {
        prefs.edit().putInt(PREF_AUTO_ENABLE_TIME_BASED_FROM_MIN, minute).apply();
        prefs.edit().putInt(PREF_AUTO_ENABLE_TIME_BASED_FROM_HOUR, hourOfDay).apply();
    }

    public static Pair<Integer, Integer> autoEnableTimeFrom() {
        Integer minute = prefs.getInt(PREF_AUTO_ENABLE_TIME_BASED_FROM_MIN, 0);
        Integer hourOfDay = prefs.getInt(PREF_AUTO_ENABLE_TIME_BASED_FROM_HOUR, 0);
        return new Pair<>(hourOfDay, minute);
    }

    public static void setAutoEnableTimeTo(int hourOfDay, int minute) {
        prefs.edit().putInt(PREF_AUTO_ENABLE_TIME_BASED_TO_MIN, minute).apply();
        prefs.edit().putInt(PREF_AUTO_ENABLE_TIME_BASED_TO_HOUR, hourOfDay).apply();
    }

    public static Pair<Integer, Integer> autoEnableTimeTo() {
        Integer minute = prefs.getInt(PREF_AUTO_ENABLE_TIME_BASED_TO_MIN, 0);
        Integer hourOfDay = prefs.getInt(PREF_AUTO_ENABLE_TIME_BASED_TO_HOUR, 0);
        return new Pair<>(hourOfDay, minute);
    }
}
