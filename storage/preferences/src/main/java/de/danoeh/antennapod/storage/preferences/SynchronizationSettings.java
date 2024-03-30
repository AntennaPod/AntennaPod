package de.danoeh.antennapod.storage.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class SynchronizationSettings {
    public static final String LAST_SYNC_ATTEMPT_TIMESTAMP = "last_sync_attempt_timestamp";
    private static final String PREF_NAME = "synchronization";
    private static final String SELECTED_SYNC_PROVIDER = "selected_sync_provider";
    private static final String LAST_SYNC_ATTEMPT_SUCCESS = "last_sync_attempt_success";
    private static final String LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP = "last_episode_actions_sync_timestamp";
    private static final String LAST_SUBSCRIPTION_SYNC_TIMESTAMP = "last_sync_timestamp";

    private static SharedPreferences prefs;

    public static void init(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isProviderConnected() {
        return getSelectedSyncProviderKey() != null;
    }

    public static void resetTimestamps() {
        prefs.edit()
                .putLong(LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0)
                .putLong(LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0)
                .putLong(LAST_SYNC_ATTEMPT_TIMESTAMP, 0)
                .apply();
    }

    public static boolean isLastSyncSuccessful() {
        return prefs.getBoolean(LAST_SYNC_ATTEMPT_SUCCESS, false);
    }

    public static long getLastSyncAttempt() {
        return prefs.getLong(LAST_SYNC_ATTEMPT_TIMESTAMP, 0);
    }

    public static void setSelectedSyncProvider(String providerIdentifier) {
        prefs.edit().putString(SELECTED_SYNC_PROVIDER, providerIdentifier).apply();
    }

    public static String getSelectedSyncProviderKey() {
        return prefs.getString(SELECTED_SYNC_PROVIDER, null);
    }

    public static void updateLastSynchronizationAttempt() {
        prefs.edit().putLong(LAST_SYNC_ATTEMPT_TIMESTAMP, System.currentTimeMillis()).apply();
    }

    public static void setLastSynchronizationAttemptSuccess(boolean isSuccess) {
        prefs.edit().putBoolean(LAST_SYNC_ATTEMPT_SUCCESS, isSuccess).apply();
    }

    public static long getLastSubscriptionSynchronizationTimestamp() {
        return prefs.getLong(LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0);
    }

    public static void setLastSubscriptionSynchronizationAttemptTimestamp(long newTimeStamp) {
        prefs.edit().putLong(LAST_SUBSCRIPTION_SYNC_TIMESTAMP, newTimeStamp).apply();
    }

    public static long getLastEpisodeActionSynchronizationTimestamp() {
        return prefs.getLong(LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0);
    }

    public static void setLastEpisodeActionSynchronizationAttemptTimestamp(long timestamp) {
        prefs.edit().putLong(LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, timestamp).apply();
    }
}
