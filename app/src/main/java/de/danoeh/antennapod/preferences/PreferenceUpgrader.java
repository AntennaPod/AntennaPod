package de.danoeh.antennapod.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;

public class PreferenceUpgrader {
    private static final String PREF_CONFIGURED_VERSION = "configuredVersion";
    private static final String PREF_NAME = "PreferenceUpgrader";

    private static SharedPreferences prefs;

    public static void checkUpgrades(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences upgraderPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int oldVersion = upgraderPrefs.getInt(PREF_CONFIGURED_VERSION, 1070200);
        int newVersion = BuildConfig.VERSION_CODE;

        if (oldVersion != newVersion) {
            NotificationUtils.createChannels(context);

            upgraderPrefs.edit().putInt(PREF_CONFIGURED_VERSION, newVersion).apply();
            upgrade(oldVersion);
        }
    }

    private static void upgrade(int oldVersion) {
        if (oldVersion < 1070196) {
            // migrate episode cleanup value (unit changed from days to hours)
            int oldValueInDays = UserPreferences.getEpisodeCleanupValue();
            if (oldValueInDays > 0) {
                UserPreferences.setEpisodeCleanupValue(oldValueInDays * 24);
            } // else 0 or special negative values, no change needed
        }
        if (oldVersion < 1070197) {
            if (prefs.getBoolean("prefMobileUpdate", false)) {
                prefs.edit().putString(UserPreferences.PREF_MOBILE_UPDATE, "everything").apply();
            }
        }
        if (oldVersion < 1070300) {
            UserPreferences.restartUpdateAlarm();

            if (UserPreferences.getMediaPlayer().equals("builtin")) {
                prefs.edit().putString(UserPreferences.PREF_MEDIA_PLAYER,
                        UserPreferences.PREF_MEDIA_PLAYER_EXOPLAYER).apply();
            }
        }
    }
}
