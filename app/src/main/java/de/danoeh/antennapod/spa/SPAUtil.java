package de.danoeh.antennapod.spa;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.util.Log;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.receiver.SPAReceiver;

/**
 * Provides methods related to AntennaPodSP (https://github.com/danieloeh/AntennaPodSP)
 */
public class SPAUtil {
    private static final String TAG = "SPAUtil";

    private static final String PREF_HAS_QUERIED_SP_APPS = "prefSPAUtil.hasQueriedSPApps";

    private SPAUtil() {
    }

    /**
     * Sends an ACTION_SP_APPS_QUERY_FEEDS intent to all AntennaPod Single Purpose apps.
     * The receiving single purpose apps will then send their feeds back to AntennaPod via an
     * ACTION_SP_APPS_QUERY_FEEDS_RESPONSE intent.
     * This intent will only be sent once.
     *
     * @return True if an intent was sent, false otherwise (for example if the intent has already been
     * sent before.
     */
    public static synchronized boolean sendSPAppsQueryFeedsIntent(Context context) {
        assert context != null : "context = null";
        final Context appContext = context.getApplicationContext();
        if (appContext == null) {
            Log.wtf(TAG, "Unable to get application context");
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        if (!prefs.getBoolean(PREF_HAS_QUERIED_SP_APPS, false)) {
            appContext.sendBroadcast(new Intent(SPAReceiver.ACTION_SP_APPS_QUERY_FEEDS));
            if (BuildConfig.DEBUG) Log.d(TAG, "Sending SP_APPS_QUERY_FEEDS intent");

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PREF_HAS_QUERIED_SP_APPS, true);
            editor.apply();

            return true;
        } else {
            return false;
        }
    }
}
