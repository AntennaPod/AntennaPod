package de.danoeh.antennapod.core.sync;

import android.content.Context;
import android.content.SharedPreferences;

import de.danoeh.antennapod.core.ClientConfig;

public class SynchronizationSettings {

    public static final String LAST_SYNC_ATTEMPT_TIMESTAMP = "last_sync_attempt_timestamp";
    private static final String NAME = "synchronization";
    private static final String SELECTED_SYNC_PROVIDER = "selected_sync_provider";
    private static final String IS_SYNC_PROVIDER_CONNECTED = "provider_is_connected";
    private static final String LAST_SYNC_ATTEMPT_SUCCESS = "last_sync_attempt_success";
    private static final String LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP = "last_episode_actions_sync_timestamp";
    private static final String LAST_SUBSCRIPTION_SYNC_TIMESTAMP = "last_sync_timestamp";

    public static boolean isSynchronizationProviderActive() {
        return SynchronizationSettings.getSelectedSyncProviderKey() != null
                && SynchronizationSettings.isProviderConnected();
    }

    public static void resetTimestamps() {
        getSharedPreferences().edit()
                .putLong(LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0)
                .putLong(LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0)
                .putLong(LAST_SYNC_ATTEMPT_TIMESTAMP, 0)
                .apply();
    }

    public static boolean isLastSyncSuccessful() {
        return getSharedPreferences().getBoolean(LAST_SYNC_ATTEMPT_SUCCESS, false);
    }

    public static long getLastSyncAttempt() {
        return getSharedPreferences().getLong(LAST_SYNC_ATTEMPT_TIMESTAMP, 0);
    }

    public static void setSelectedSyncProvider(SynchronizationProviderViewData userSelect) {
        String userSelectName = null;
        if (userSelect != null) {
            userSelectName = userSelect.getIdentifier();
        }

        getSharedPreferences()
                .edit()
                .putString(SELECTED_SYNC_PROVIDER, userSelectName)
                .apply();
    }

    public static String getSelectedSyncProviderKey() {
        return getSharedPreferences().getString(SELECTED_SYNC_PROVIDER, null);
    }

    public static void setIsProviderConnected(boolean isConnected) {
        getSharedPreferences()
                .edit()
                .putBoolean(IS_SYNC_PROVIDER_CONNECTED, isConnected)
                .apply();
    }

    public static boolean isProviderConnected() {
        return getSharedPreferences()
                .getBoolean(IS_SYNC_PROVIDER_CONNECTED, false);
    }

    public static void updateLastSynchronizationAttempt() {
        getSharedPreferences().edit()
                .putLong(LAST_SYNC_ATTEMPT_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }

    public static void setLastSynchronizationAttemptSuccess(boolean isSuccess) {
        getSharedPreferences().edit()
                .putBoolean(LAST_SYNC_ATTEMPT_SUCCESS, isSuccess)
                .apply();
    }

    public static long getLastSubscriptionSynchronizationTimestamp() {
        return getSharedPreferences().getLong(LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0);
    }

    public static void setLastSubscriptionSynchronizationAttemptTimestamp(long newTimeStamp) {
        getSharedPreferences().edit()
                .putLong(LAST_SUBSCRIPTION_SYNC_TIMESTAMP, newTimeStamp).apply();
    }

    public static long getLastEpisodeActionSynchronizationTimestamp() {
        return getSharedPreferences()
                .getLong(LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0);
    }

    public static void setLastEpisodeActionSynchronizationAttemptTimestamp(long timestamp) {
        getSharedPreferences().edit()
                .putLong(LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, timestamp).apply();
    }

    private static SharedPreferences getSharedPreferences() {
        return ClientConfig.applicationCallbacks.getApplicationInstance()
                .getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }
}