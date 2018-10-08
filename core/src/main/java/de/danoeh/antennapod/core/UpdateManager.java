package de.danoeh.antennapod.core;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.antennapod.audio.MediaPlayer;

import de.danoeh.antennapod.core.preferences.UserPreferences;

/*
 * This class's job is do perform maintenance tasks whenever the app has been updated
 */
class UpdateManager {

    private static final String TAG = UpdateManager.class.getSimpleName();

    private static final String PREF_NAME = "app_version";
    private static final String KEY_VERSION_CODE = "version_code";

    private static int currentVersionCode;

    private static Context context;
    private static SharedPreferences prefs;

    public static void init(Context context) {
        UpdateManager.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            currentVersionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to obtain package info for package name: " + context.getPackageName(), e);
            currentVersionCode = 0;
            return;
        }
        final int oldVersionCode = getStoredVersionCode();
        Log.d(TAG, "old: " + oldVersionCode + ", current: " + currentVersionCode);
        if(oldVersionCode < currentVersionCode) {
            onUpgrade(oldVersionCode, currentVersionCode);
            setCurrentVersionCode();
        }
    }

    private static int getStoredVersionCode() {
        return prefs.getInt(KEY_VERSION_CODE, -1);
    }

    private static void setCurrentVersionCode() {
        prefs.edit().putInt(KEY_VERSION_CODE, currentVersionCode).apply();
    }

    private static void onUpgrade(final int oldVersionCode, final int newVersionCode) {
        if(oldVersionCode < 1050004) {
            if(MediaPlayer.isPrestoLibraryInstalled(context) && Build.VERSION.SDK_INT >= 16) {
                UserPreferences.enableSonic();
            }
        }
    }

}
