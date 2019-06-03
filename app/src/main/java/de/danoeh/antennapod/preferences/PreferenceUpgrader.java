package de.danoeh.antennapod.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.core.preferences.UserPreferences;

public class PreferenceUpgrader {
    private static final String PREF_CONFIGURED_VERSION = "configuredVersion";
    private static final String PREF_NAME = "PreferenceUpgrader";


    public static void checkUpgrades(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int oldVersion = prefs.getInt(PREF_CONFIGURED_VERSION, 1070200);
        int newVersion = BuildConfig.VERSION_CODE;

        if (oldVersion != newVersion) {
            prefs.edit().putInt(PREF_CONFIGURED_VERSION, newVersion).apply();
            upgrade(oldVersion);
        }
    }

    private static void upgrade(int oldVersion) {
        if (oldVersion < 1070300) {
            UserPreferences.restartUpdateAlarm();
        }
    }
}
