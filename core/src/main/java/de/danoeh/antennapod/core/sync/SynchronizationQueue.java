package de.danoeh.antennapod.core.sync;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import de.danoeh.antennapod.net.sync.model.EpisodeAction;

public class SynchronizationQueue {

    public static final String NAME = "synchronization";
    private final SharedPreferences sharedPreferences;

    public SynchronizationQueue(Context context) {
        this.sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    private SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void clearQueue() {
        getSharedPreferences().edit()
                .putLong(SynchronizationSharedPreferenceKeys.LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0)
                .putLong(SynchronizationSharedPreferenceKeys.LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0)
                .putLong(SynchronizationSharedPreferenceKeys.LAST_SYNC_ATTEMPT_TIMESTAMP, 0)
                .putString(SynchronizationSharedPreferenceKeys.QUEUED_EPISODE_ACTIONS, "[]")
                .putString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_ADDED, "[]")
                .putString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_REMOVED, "[]")
                .apply();
    }

    public void enqueueFeedAdded(String downloadUrl) throws JSONException {
        SharedPreferences sharedPreferences = getSharedPreferences();
        String json = sharedPreferences
                .getString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_ADDED, "[]");
        JSONArray queue = new JSONArray(json);
        queue.put(downloadUrl);
        sharedPreferences
                .edit().putString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_ADDED, queue.toString()).apply();
    }

    public void enqueueFeedRemoved(String downloadUrl) throws JSONException {
        SharedPreferences sharedPreferences = getSharedPreferences();
        String json = sharedPreferences.getString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_REMOVED, "[]");
        JSONArray queue = new JSONArray(json);
        queue.put(downloadUrl);
        sharedPreferences.edit().putString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_REMOVED, queue.toString())
                .apply();
    }

    public void enqueueEpisodeAction(EpisodeAction action) throws JSONException {
        SharedPreferences sharedPreferences = getSharedPreferences();
        String json = sharedPreferences.getString(SynchronizationSharedPreferenceKeys.QUEUED_EPISODE_ACTIONS, "[]");
        JSONArray queue = new JSONArray(json);
        queue.put(action.writeToJsonObject());
        sharedPreferences.edit().putString(
                SynchronizationSharedPreferenceKeys.QUEUED_EPISODE_ACTIONS, queue.toString()
        ).apply();
    }

    public ArrayList<EpisodeAction> getQueuedEpisodeActions() {
        ArrayList<EpisodeAction> actions = new ArrayList<>();
        try {
            String json = getSharedPreferences()
                    .getString(SynchronizationSharedPreferenceKeys.QUEUED_EPISODE_ACTIONS, "[]");
            JSONArray queue = new JSONArray(json);
            for (int i = 0; i < queue.length(); i++) {
                actions.add(EpisodeAction.readFromJsonObject(queue.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return actions;
    }

    public ArrayList<String> getQueuedRemovedFeeds() {
        ArrayList<String> removedFeedUrls = new ArrayList<>();
        try {
            String json = getSharedPreferences()
                    .getString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_REMOVED, "[]");
            JSONArray queue = new JSONArray(json);
            for (int i = 0; i < queue.length(); i++) {
                removedFeedUrls.add(queue.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return removedFeedUrls;

    }

    public ArrayList<String> getQueuedAddedFeeds() {
        ArrayList<String> addedFeedUrls = new ArrayList<>();
        try {
            String json = getSharedPreferences()
                    .getString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_ADDED, "[]");
            JSONArray queue = new JSONArray(json);
            for (int i = 0; i < queue.length(); i++) {
                addedFeedUrls.add(queue.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return addedFeedUrls;
    }

    public void clearEpisodeActionQueue() {
        getSharedPreferences().edit()
                .putString(SynchronizationSharedPreferenceKeys.QUEUED_EPISODE_ACTIONS, "[]").apply();

    }

    public void clearFeedQueues() {
        getSharedPreferences().edit()
                .putString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_ADDED, "[]")
                .putString(SynchronizationSharedPreferenceKeys.QUEUED_FEEDS_REMOVED, "[]")
                .apply();
    }
}
