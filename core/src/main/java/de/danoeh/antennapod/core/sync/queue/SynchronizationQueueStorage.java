package de.danoeh.antennapod.core.sync.queue;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import de.danoeh.antennapod.core.sync.SynchronizationSettings;
import de.danoeh.antennapod.net.sync.model.EpisodeAction;

public class SynchronizationQueueStorage {

    private static final String NAME = "synchronization";
    private static final String QUEUED_EPISODE_ACTIONS = "sync_queued_episode_actions";
    private static final String QUEUED_FEEDS_REMOVED = "sync_removed";
    private static final String QUEUED_FEEDS_ADDED = "sync_added";
    private final SharedPreferences sharedPreferences;

    public SynchronizationQueueStorage(Context context) {
        this.sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public ArrayList<EpisodeAction> getQueuedEpisodeActions() {
        ArrayList<EpisodeAction> actions = new ArrayList<>();
        try {
            String json = getSharedPreferences()
                    .getString(QUEUED_EPISODE_ACTIONS, "[]");
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
                    .getString(QUEUED_FEEDS_REMOVED, "[]");
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
                    .getString(QUEUED_FEEDS_ADDED, "[]");
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
                .putString(QUEUED_EPISODE_ACTIONS, "[]").apply();

    }

    public void clearFeedQueues() {
        getSharedPreferences().edit()
                .putString(QUEUED_FEEDS_ADDED, "[]")
                .putString(QUEUED_FEEDS_REMOVED, "[]")
                .apply();
    }

    protected void clearQueue() {
        SynchronizationSettings.resetTimestamps();
        getSharedPreferences().edit()
                .putString(QUEUED_EPISODE_ACTIONS, "[]")
                .putString(QUEUED_FEEDS_ADDED, "[]")
                .putString(QUEUED_FEEDS_REMOVED, "[]")
                .apply();

    }

    protected void enqueueFeedAdded(String downloadUrl) {
        SharedPreferences sharedPreferences = getSharedPreferences();
        try {
            JSONArray addedQueue = new JSONArray(sharedPreferences.getString(QUEUED_FEEDS_ADDED, "[]"));
            addedQueue.put(downloadUrl);
            JSONArray removedQueue = new JSONArray(sharedPreferences.getString(QUEUED_FEEDS_REMOVED, "[]"));
            removedQueue.remove(indexOf(downloadUrl, removedQueue));
            sharedPreferences.edit()
                    .putString(QUEUED_FEEDS_ADDED, addedQueue.toString())
                    .putString(QUEUED_FEEDS_REMOVED, removedQueue.toString())
                    .apply();

        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
    }

    protected void enqueueFeedRemoved(String downloadUrl) {
        SharedPreferences sharedPreferences = getSharedPreferences();
        try {
            JSONArray removedQueue = new JSONArray(sharedPreferences.getString(QUEUED_FEEDS_REMOVED, "[]"));
            removedQueue.put(downloadUrl);
            JSONArray addedQueue = new JSONArray(sharedPreferences.getString(QUEUED_FEEDS_ADDED, "[]"));
            addedQueue.remove(indexOf(downloadUrl, addedQueue));
            sharedPreferences.edit()
                    .putString(QUEUED_FEEDS_ADDED, addedQueue.toString())
                    .putString(QUEUED_FEEDS_REMOVED, removedQueue.toString())
                    .apply();
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
    }

    private int indexOf(String string, JSONArray array) {
        try {
            for (int i = 0; i < array.length(); i++) {
                if (array.getString(i).equals(string)) {
                    return i;
                }
            }
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
        return -1;
    }

    protected void enqueueEpisodeAction(EpisodeAction action) {
        SharedPreferences sharedPreferences = getSharedPreferences();
        String json = sharedPreferences.getString(QUEUED_EPISODE_ACTIONS, "[]");
        try {
            JSONArray queue = new JSONArray(json);
            queue.put(action.writeToJsonObject());
            sharedPreferences.edit().putString(
                    QUEUED_EPISODE_ACTIONS, queue.toString()
            ).apply();
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
    }

    private SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
}
