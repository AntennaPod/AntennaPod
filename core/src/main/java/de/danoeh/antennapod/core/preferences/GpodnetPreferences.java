package de.danoeh.antennapod.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.sync.SyncService;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetService;

/**
 * Manages preferences for accessing gpodder.net service
 */
public class GpodnetPreferences {

    private GpodnetPreferences(){}

    private static final String TAG = "GpodnetPreferences";

    private static final String PREF_NAME = "gpodder.net";
    private static final String PREF_GPODNET_USERNAME = "de.danoeh.antennapod.preferences.gpoddernet.username";
    private static final String PREF_GPODNET_PASSWORD = "de.danoeh.antennapod.preferences.gpoddernet.password";
    private static final String PREF_GPODNET_DEVICEID = "de.danoeh.antennapod.preferences.gpoddernet.deviceID";
    private static final String PREF_GPODNET_HOSTNAME = "prefGpodnetHostname";

    private static String username;
    private static String password;
    private static String deviceID;
    private static String hosturl;

    private static boolean preferencesLoaded = false;

    private static SharedPreferences getPreferences() {
        return ClientConfig.applicationCallbacks.getApplicationInstance().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static synchronized void ensurePreferencesLoaded() {
        if (!preferencesLoaded) {
            SharedPreferences prefs = getPreferences();
            username = prefs.getString(PREF_GPODNET_USERNAME, null);
            password = prefs.getString(PREF_GPODNET_PASSWORD, null);
            deviceID = prefs.getString(PREF_GPODNET_DEVICEID, null);
            hosturl = prefs.getString(PREF_GPODNET_HOSTNAME, GpodnetService.DEFAULT_BASE_HOST);

            preferencesLoaded = true;
        }
    }

    private static void writePreference(String key, String value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getUsername() {
        ensurePreferencesLoaded();
        return username;
    }

    public static void setUsername(String username) {
        GpodnetPreferences.username = username;
        writePreference(PREF_GPODNET_USERNAME, username);
    }

    public static String getPassword() {
        ensurePreferencesLoaded();
        return password;
    }

    public static void setPassword(String password) {
        GpodnetPreferences.password = password;
        writePreference(PREF_GPODNET_PASSWORD, password);
    }

    public static String getDeviceID() {
        ensurePreferencesLoaded();
        return deviceID;
    }

    public static void setDeviceID(String deviceID) {
        GpodnetPreferences.deviceID = deviceID;
        writePreference(PREF_GPODNET_DEVICEID, deviceID);
    }

    public static String getHosturl() {
        ensurePreferencesLoaded();
        return hosturl;
    }

    public static void setHosturl(String value) {
        if (!value.equals(hosturl)) {
            logout();
            writePreference(PREF_GPODNET_HOSTNAME, value);
            hosturl = value;
        }
    }

    /**
     * Returns true if device ID, username and password have a non-null value
     */
    public static boolean loggedIn() {
        ensurePreferencesLoaded();
        return deviceID != null && username != null && password != null;
    }

    public static synchronized void logout() {
        if (BuildConfig.DEBUG) Log.d(TAG, "Logout: Clearing preferences");
        setUsername(null);
        setPassword(null);
        setDeviceID(null);
        SyncService.clearQueue(ClientConfig.applicationCallbacks.getApplicationInstance());
        UserPreferences.setGpodnetNotificationsEnabled();
    }

}
