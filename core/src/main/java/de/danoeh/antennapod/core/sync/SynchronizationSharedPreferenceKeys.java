package de.danoeh.antennapod.core.sync;

public interface SynchronizationSharedPreferenceKeys {
    String NAME = "synchronization";
    String SELECTED_SYNC_PROVIDER = "selected_sync_provider";
    String IS_SYNC_PROVIDER_CONNECTED = "provider_is_connected";
    String LAST_SUBSCRIPTION_SYNC_TIMESTAMP = "last_sync_timestamp";
    String LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP = "last_episode_actions_sync_timestamp";
    String QUEUED_FEEDS_ADDED = "sync_added";
    String QUEUED_FEEDS_REMOVED = "sync_removed";
    String QUEUED_EPISODE_ACTIONS = "sync_queued_episode_actions";
    String LAST_SYNC_ATTEMPT_TIMESTAMP = "last_sync_attempt_timestamp";
    String LAST_SYNC_ATTEMPT_SUCCESS = "last_sync_attempt_success";
}
