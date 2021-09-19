package de.danoeh.antennapod.core.sync;

import android.content.Context;
import android.content.SharedPreferences;

import de.danoeh.antennapod.core.ClientConfig;

public class SynchronizationSettings {

    public static final String NAME = "synchronization";

    public static void resetTimestamps() {
        getSharedPreferences().edit()
                .putLong(SynchronizationSharedPreferenceKeys.LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0)
                .putLong(SynchronizationSharedPreferenceKeys.LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0)
                .putLong(SynchronizationSharedPreferenceKeys.LAST_SYNC_ATTEMPT_TIMESTAMP, 0)
                .apply();
    }

    public static boolean isLastSyncSuccessful() {
        return getSharedPreferences().getBoolean(SynchronizationSharedPreferenceKeys.LAST_SYNC_ATTEMPT_SUCCESS, false);
    }

    public static long getLastSyncAttempt() {
        return getSharedPreferences().getLong(SynchronizationSharedPreferenceKeys.LAST_SYNC_ATTEMPT_TIMESTAMP, 0);
    }

    public static void setSelectedSyncProvider(SynchronizationProviderViewData userSelect) {
        String userSelectName = null;
        if (userSelect != null) {
            userSelectName = userSelect.getIdentifier();
        }

        getSharedPreferences()
                .edit()
                .putString(SynchronizationSharedPreferenceKeys.SELECTED_SYNC_PROVIDER, userSelectName)
                .apply();
    }

    public static String getSelectedSyncProviderKey() {
        return getSharedPreferences().getString(SynchronizationSharedPreferenceKeys.SELECTED_SYNC_PROVIDER, null);
    }

    public static void setIsProviderConnected(boolean isConnected) {
        getSharedPreferences()
                .edit()
                .putBoolean(SynchronizationSharedPreferenceKeys.IS_SYNC_PROVIDER_CONNECTED, isConnected)
                .apply();
    }

    public static boolean isProviderConnected() {
        return getSharedPreferences()
                .getBoolean(SynchronizationSharedPreferenceKeys.IS_SYNC_PROVIDER_CONNECTED, false);
    }

    public static void updateLastSynchronizationAttempt() {
        getSharedPreferences().edit()
                .putLong(SynchronizationSharedPreferenceKeys.LAST_SYNC_ATTEMPT_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }

    public static void setLastSynchronizationAttemptSuccess(boolean isSuccess) {
        getSharedPreferences().edit()
                .putBoolean(SynchronizationSharedPreferenceKeys.LAST_SYNC_ATTEMPT_SUCCESS, isSuccess)
                .apply();
    }

    public static long getLastSubscriptionSynchronizationTimestamp() {
        return getSharedPreferences().getLong(SynchronizationSharedPreferenceKeys.LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0);
    }

    public static void setLastSubscriptionSynchronizationAttemptTimestamp(long newTimeStamp) {
        getSharedPreferences().edit()
                .putLong(SynchronizationSharedPreferenceKeys.LAST_SUBSCRIPTION_SYNC_TIMESTAMP, newTimeStamp).apply();
    }

    public static long getLastEpisodeActionSynchronizationTimestamp() {
        return getSharedPreferences()
                .getLong(SynchronizationSharedPreferenceKeys.LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0);
    }

    public static void setLastEpisodeActionSynchronizationAttemptTimestamp(long timestamp) {
        getSharedPreferences().edit()
                .putLong(SynchronizationSharedPreferenceKeys.LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, timestamp).apply();
    }

    private static SharedPreferences getSharedPreferences() {
        return ClientConfig.applicationCallbacks.getApplicationInstance()
                .getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }
}