package de.danoeh.antennapod.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.service.GpodnetSyncService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages preferences for accessing gpodder.net service
 */
public class GpodnetPreferences {

    private static final String TAG = "GpodnetPreferences";

    private static final String PREF_NAME = "gpodder.net";
    public static final String PREF_GPODNET_USERNAME = "de.danoeh.antennapod.preferences.gpoddernet.username";
    public static final String PREF_GPODNET_PASSWORD = "de.danoeh.antennapod.preferences.gpoddernet.password";
    public static final String PREF_GPODNET_DEVICEID = "de.danoeh.antennapod.preferences.gpoddernet.deviceID";

    public static final String PREF_LAST_SYNC_TIMESTAMP = "de.danoeh.antennapod.preferences.gpoddernet.last_sync_timestamp";
    public static final String PREF_SYNC_ADDED = "de.danoeh.antennapod.preferences.gpoddernet.sync_added";
    public static final String PREF_SYNC_REMOVED = "de.danoeh.antennapod.preferences.gpoddernet.sync_removed";

    private static String username;
    private static String password;
    private static String deviceID;

    private static ReentrantLock feedListLock = new ReentrantLock();
    private static Set<String> addedFeeds;
    private static Set<String> removedFeeds;

    /**
     * Last value returned by getSubscriptionChanges call. Will be used for all subsequent calls of getSubscriptionChanges.
     */
    private static long lastSyncTimestamp;

    private static boolean preferencesLoaded = false;

    private static SharedPreferences getPreferences() {
        return PodcastApp.getInstance().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static synchronized void ensurePreferencesLoaded() {
        if (!preferencesLoaded) {
            SharedPreferences prefs = getPreferences();
            username = prefs.getString(PREF_GPODNET_USERNAME, null);
            password = prefs.getString(PREF_GPODNET_PASSWORD, null);
            deviceID = prefs.getString(PREF_GPODNET_DEVICEID, null);
            lastSyncTimestamp = prefs.getLong(PREF_LAST_SYNC_TIMESTAMP, 0);
            addedFeeds = readListFromString(prefs.getString(PREF_SYNC_ADDED, ""));
            removedFeeds = readListFromString(prefs.getString(PREF_SYNC_REMOVED, ""));

            preferencesLoaded = true;
        }
    }

    private static void writePreference(String key, String value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(key, value);
        editor.commit();
    }

    private static void writePreference(String key, long value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putLong(key, value);
        editor.commit();
    }

    private static void writePreference(String key, Collection<String> value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(key, writeListToString(value));
        editor.commit();
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

    public static long getLastSyncTimestamp() {
        ensurePreferencesLoaded();
        return lastSyncTimestamp;
    }

    public static void setLastSyncTimestamp(long lastSyncTimestamp) {
        GpodnetPreferences.lastSyncTimestamp = lastSyncTimestamp;
        writePreference(PREF_LAST_SYNC_TIMESTAMP, lastSyncTimestamp);
    }

    public static void addAddedFeed(String feed) {
        ensurePreferencesLoaded();
        feedListLock.lock();
        if (addedFeeds.add(feed)) {
            writePreference(PREF_SYNC_ADDED, addedFeeds);
        }
        if (removedFeeds.remove(feed)) {
            writePreference(PREF_SYNC_REMOVED, removedFeeds);
        }
        feedListLock.unlock();
        GpodnetSyncService.sendActionUploadIntent(PodcastApp.getInstance());
    }

    public static void addRemovedFeed(String feed) {
        ensurePreferencesLoaded();
        feedListLock.lock();
        if (removedFeeds.add(feed)) {
            writePreference(PREF_SYNC_REMOVED, removedFeeds);
        }
        if (addedFeeds.remove(feed)) {
            writePreference(PREF_SYNC_ADDED, addedFeeds);
        }
        feedListLock.unlock();
        GpodnetSyncService.sendActionUploadIntent(PodcastApp.getInstance());
    }

    public static Set<String> getAddedFeedsCopy() {
        ensurePreferencesLoaded();
        Set<String> copy = new HashSet<String>();
        feedListLock.lock();
        copy.addAll(addedFeeds);
        feedListLock.unlock();
        return copy;
    }

    public static void removeAddedFeeds(Set<String> removed) {
        ensurePreferencesLoaded();
        feedListLock.lock();
        addedFeeds.removeAll(removed);
        writePreference(PREF_SYNC_ADDED, addedFeeds);
        feedListLock.unlock();
    }

    public static Set<String> getRemovedFeedsCopy() {
        ensurePreferencesLoaded();
        Set<String> copy = new HashSet<String>();
        feedListLock.lock();
        copy.addAll(removedFeeds);
        feedListLock.unlock();
        return copy;
    }

    public static void removeRemovedFeeds(Set<String> removed) {
        ensurePreferencesLoaded();
        removedFeeds.removeAll(removed);
        writePreference(PREF_SYNC_REMOVED, removedFeeds);

    }

    /**
     * Returns true if device ID, username and password have a non-null value
     */
    public static boolean loggedIn() {
        ensurePreferencesLoaded();
        return deviceID != null && username != null && password != null;
    }

    public static void logout() {
        setUsername(null);
        setPassword(null);
        setDeviceID(null);
        setLastSyncTimestamp(0);
    }

    private static Set<String> readListFromString(String s) {
        Set<String> result = new HashSet<String>();
        for (String item : s.split(" ")) {
            result.add(item);
        }
        return result;
    }

    private static String writeListToString(Collection<String> c) {
        StringBuilder result = new StringBuilder();
        for (String item : c) {
            result.append(item);
            result.append(" ");
        }
        return result.toString().trim();
    }
}
