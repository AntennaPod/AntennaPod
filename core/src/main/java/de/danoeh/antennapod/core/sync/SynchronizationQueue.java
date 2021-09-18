package de.danoeh.antennapod.core.sync;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.net.sync.model.EpisodeAction;

public class SynchronizationQueue {

    public static final String NAME = "synchronization";

    private static SharedPreferences getSharedPreferences() {
        return ClientConfig.applicationCallbacks.getApplicationInstance()
                .getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static void clearQueue() {
        getSharedPreferences().edit()
                .putLong(SynchronizationSharedPreferenceKeys.LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0)
                .putLong(SynchronizationSharedPreferenceKeys.LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0)
                .putLong(SynchronizationSharedPreferenceKeys.LAST_SYNC_ATTEMPT_TIMESTAMP, 0)
                .putString(SynchronizationSharedPreferenceKeys.QUEUED_EPISODE_ACTIONS, "[]")
                .putString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_ADDED, "[]")
                .putString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_REMOVED, "[]")
                .apply();
    }

    public static void enqueueFeedAdded(String downloadUrl) throws JSONException {
        SharedPreferences sharedPreferences = getSharedPreferences();
        String json = sharedPreferences
                .getString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_ADDED, "[]");
        JSONArray queue = new JSONArray(json);
        queue.put(downloadUrl);
        sharedPreferences
                .edit().putString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_ADDED, queue.toString()).apply();
    }

    public static void enqueueFeedRemoved(String downloadUrl) throws JSONException {
        SharedPreferences sharedPreferences = getSharedPreferences();
        String json = sharedPreferences.getString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_REMOVED, "[]");
        JSONArray queue = new JSONArray(json);
        queue.put(downloadUrl);
        sharedPreferences.edit().putString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_REMOVED, queue.toString())
                .apply();
    }

    public static void enqueueEpisodeAction(EpisodeAction action) throws JSONException {
        SharedPreferences sharedPreferences = getSharedPreferences();
        String json = sharedPreferences.getString(SynchronizationSharedPreferenceKeys.QUEUED_EPISODE_ACTIONS, "[]");
        JSONArray queue = new JSONArray(json);
        queue.put(action.writeToJsonObject());
        sharedPreferences.edit().putString(
                SynchronizationSharedPreferenceKeys.QUEUED_EPISODE_ACTIONS, queue.toString()
        ).apply();
    }

    public static ArrayList<EpisodeAction> getQueuedEpisodeActions() throws JSONException {
        ArrayList<EpisodeAction> actions = new ArrayList<>();
        String json = getSharedPreferences()
                .getString(SynchronizationSharedPreferenceKeys.QUEUED_EPISODE_ACTIONS, "[]");
        JSONArray queue = new JSONArray(json);
        for (int i = 0; i < queue.length(); i++) {
            actions.add(EpisodeAction.readFromJsonObject(queue.getJSONObject(i)));
        }

        return actions;
    }

    public static ArrayList<String> getQueuedRemovedFeeds() throws JSONException {
        ArrayList<String> removedFeedUrls = new ArrayList<>();
        String json = getSharedPreferences().getString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_REMOVED, "[]");
        JSONArray queue = new JSONArray(json);
        for (int i = 0; i < queue.length(); i++) {
            removedFeedUrls.add(queue.getString(i));
        }

        return removedFeedUrls;
    }

    public static ArrayList<String> getQueuedAddedFeeds() throws JSONException {
        ArrayList<String> addedFeedUrls = new ArrayList<>();
        String json = getSharedPreferences().getString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_ADDED, "[]");
        JSONArray queue = new JSONArray(json);
        for (int i = 0; i < queue.length(); i++) {
            addedFeedUrls.add(queue.getString(i));
        }

        return addedFeedUrls;
    }
}
