package de.danoeh.antennapod.core.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.core.app.NotificationCompat;
import androidx.core.app.SafeJobIntentService;
import androidx.core.util.Pair;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.sync.model.EpisodeAction;
import de.danoeh.antennapod.core.sync.model.EpisodeActionChanges;
import de.danoeh.antennapod.core.sync.model.ISyncService;
import de.danoeh.antennapod.core.sync.model.SubscriptionChanges;
import de.danoeh.antennapod.core.sync.model.SyncServiceException;
import de.danoeh.antennapod.core.sync.model.UploadChangesResponse;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.URLChecker;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SyncService extends SafeJobIntentService {
    private static final String PREF_NAME = "SyncService";
    private static final String PREF_LAST_SUBSCRIPTION_SYNC_TIMESTAMP = "last_sync_timestamp";
    private static final String PREF_LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP = "last_episode_actions_sync_timestamp";
    private static final String PREF_QUEUED_FEEDS_ADDED = "sync_added";
    private static final String PREF_QUEUED_FEEDS_REMOVED = "sync_removed";
    private static final String PREF_QUEUED_EPISODE_ACTIONS = "sync_queued_episode_actions";
    private static final String PREF_LAST_SYNC_ATTEMPT_TIMESTAMP = "last_sync_attempt_timestamp";
    private static final String TAG = "SyncService";
    private static final int JOB_ID = -17000;
    private static final Object lock = new Object();
    private static boolean syncPending = false;

    private ISyncService syncServiceImpl;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        syncServiceImpl = new GpodnetService(AntennapodHttpClient.getHttpClient(), GpodnetService.DEFAULT_BASE_HOST);

        if (!NetworkUtils.networkAvailable()) {
            stopForeground(true);
            stopSelf();
            return;
        }

        try {
            // Leave some time, so other actions can be queued
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        syncPending = false;
        try {
            syncServiceImpl.login();
            syncSubscriptions();
            syncEpisodeActions();
            syncServiceImpl.logout();
            clearErrorNotifications();
        } catch (SyncServiceException e) {
            e.printStackTrace();
            updateErrorNotification(e);
        }
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putLong(PREF_LAST_SYNC_ATTEMPT_TIMESTAMP, System.currentTimeMillis()).apply();
    }

    public static void clearQueue(Context context) {
        synchronized (lock) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putLong(PREF_LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0)
                    .putLong(PREF_LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0)
                    .putLong(PREF_LAST_SYNC_ATTEMPT_TIMESTAMP, 0)
                    .putString(PREF_QUEUED_EPISODE_ACTIONS, "[]")
                    .putString(PREF_QUEUED_FEEDS_ADDED, "[]")
                    .putString(PREF_QUEUED_FEEDS_REMOVED, "[]")
                    .apply();
        }
    }

    public static void enqueueFeedAdded(Context context, String downloadUrl) {
        synchronized (lock) {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String json = prefs.getString(PREF_QUEUED_FEEDS_ADDED, "[]");
                JSONArray queue = new JSONArray(json);
                queue.put(downloadUrl);
                prefs.edit().putString(PREF_QUEUED_FEEDS_ADDED, queue.toString()).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sync(context);
    }

    public static void enqueueFeedRemoved(Context context, String downloadUrl) {
        synchronized (lock) {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String json = prefs.getString(PREF_QUEUED_FEEDS_REMOVED, "[]");
                JSONArray queue = new JSONArray(json);
                queue.put(downloadUrl);
                prefs.edit().putString(PREF_QUEUED_FEEDS_REMOVED, queue.toString()).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sync(context);
    }

    public static void enqueueEpisodeAction(Context context, EpisodeAction action) {
        synchronized (lock) {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String json = prefs.getString(PREF_QUEUED_EPISODE_ACTIONS, "[]");
                JSONArray queue = new JSONArray(json);
                queue.put(action.writeToJSONObject());
                prefs.edit().putString(PREF_QUEUED_EPISODE_ACTIONS, queue.toString()).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sync(context);
    }

    public static void sync(Context context) {
        if (!syncPending) {
            syncPending = true;
            enqueueWork(context, SyncService.class, JOB_ID, new Intent());
        } else {
            Log.d(TAG, "Ignored sync: Job already enqueued");
        }
    }

    public static void fullSync(Context context) {
        synchronized (lock) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putLong(PREF_LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0)
                    .putLong(PREF_LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0)
                    .putLong(PREF_LAST_SYNC_ATTEMPT_TIMESTAMP, 0)
                    .apply();
        }
        sync(context);
    }


    private List<EpisodeAction> getQueuedEpisodeActions() {
        ArrayList<EpisodeAction> actions = new ArrayList<>();
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(PREF_QUEUED_EPISODE_ACTIONS, "[]");
            JSONArray queue = new JSONArray(json);
            for (int i = 0; i < queue.length(); i++) {
                actions.add(EpisodeAction.readFromJSONObject(queue.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return actions;
    }

    private List<String> getQueuedRemovedFeeds() {
        ArrayList<String> actions = new ArrayList<>();
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(PREF_QUEUED_FEEDS_REMOVED, "[]");
            JSONArray queue = new JSONArray(json);
            for (int i = 0; i < queue.length(); i++) {
                actions.add(queue.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return actions;
    }

    private List<String> getQueuedAddedFeeds() {
        ArrayList<String> actions = new ArrayList<>();
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(PREF_QUEUED_FEEDS_ADDED, "[]");
            JSONArray queue = new JSONArray(json);
            for (int i = 0; i < queue.length(); i++) {
                actions.add(queue.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return actions;
    }

    private void syncSubscriptions() throws SyncServiceException {
        final long lastSync = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(PREF_LAST_SUBSCRIPTION_SYNC_TIMESTAMP, 0);
        final List<String> localSubscriptions = DBReader.getFeedListDownloadUrls();
        SubscriptionChanges subscriptionChanges = syncServiceImpl.getSubscriptionChanges(lastSync);
        long newTimeStamp = subscriptionChanges.getTimestamp();

        List<String> queuedRemovedFeeds = getQueuedRemovedFeeds();
        List<String> queuedAddedFeeds = getQueuedAddedFeeds();

        Log.d(TAG, "Downloaded subscription changes: " + subscriptionChanges);
        for (String downloadUrl : subscriptionChanges.getAdded()) {
            if (!URLChecker.containsUrl(localSubscriptions, downloadUrl) && !queuedRemovedFeeds.contains(downloadUrl)) {
                Feed feed = new Feed(downloadUrl, null);
                try {
                    DownloadRequester.getInstance().downloadFeed(this, feed);
                } catch (DownloadRequestException e) {
                    e.printStackTrace();
                }
            }
        }

        // remove subscription if not just subscribed (again)
        for (String downloadUrl : subscriptionChanges.getRemoved()) {
            if (!queuedAddedFeeds.contains(downloadUrl)) {
                DBTasks.removeFeedWithDownloadUrl(this, downloadUrl);
            }
        }

        if (lastSync == 0) {
            Log.d(TAG, "First sync. Adding all local subscriptions.");
            queuedAddedFeeds = localSubscriptions;
            queuedAddedFeeds.removeAll(subscriptionChanges.getAdded());
            queuedRemovedFeeds.removeAll(subscriptionChanges.getRemoved());
        }

        if (queuedAddedFeeds.size() > 0 || queuedRemovedFeeds.size() > 0) {
            Log.d(TAG, "Added: " + StringUtils.join(queuedAddedFeeds, ", "));
            Log.d(TAG, "Removed: " + StringUtils.join(queuedRemovedFeeds, ", "));

            synchronized (lock) {
                UploadChangesResponse uploadResponse = syncServiceImpl
                    .uploadSubscriptionChanges(queuedAddedFeeds, queuedRemovedFeeds);
                getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                        .putString(PREF_QUEUED_FEEDS_ADDED, "[]").apply();
                getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                        .putString(PREF_QUEUED_FEEDS_REMOVED, "[]").apply();
                newTimeStamp = uploadResponse.timestamp;
            }
        }
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putLong(PREF_LAST_SUBSCRIPTION_SYNC_TIMESTAMP, newTimeStamp).apply();
    }

    private void syncEpisodeActions() throws SyncServiceException {
        final long lastSync = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(PREF_LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, 0);
        EpisodeActionChanges getResponse = syncServiceImpl.getEpisodeActionChanges(lastSync);
        long newTimeStamp = getResponse.getTimestamp();
        List<EpisodeAction> remoteActions = getResponse.getEpisodeActions();
        processEpisodeActions(remoteActions);

        // upload local actions
        List<EpisodeAction> queuedEpisodeActions = getQueuedEpisodeActions();
        if (queuedEpisodeActions.size() > 0) {
            synchronized (lock) {
                Log.d(TAG, "Uploading actions: " + StringUtils.join(queuedEpisodeActions, ", "));
                UploadChangesResponse postResponse = syncServiceImpl.uploadEpisodeActions(queuedEpisodeActions);
                newTimeStamp = postResponse.timestamp;
                Log.d(TAG, "Upload episode response: " + postResponse);
                getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                        .putString(PREF_QUEUED_EPISODE_ACTIONS, "[]").apply();
            }
        }
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putLong(PREF_LAST_EPISODE_ACTIONS_SYNC_TIMESTAMP, newTimeStamp).apply();
    }


    private synchronized void processEpisodeActions(List<EpisodeAction> remoteActions) {
        if (remoteActions.size() == 0) {
            return;
        }
        Map<Pair<String, String>, EpisodeAction> localMostRecentPlayAction = new ArrayMap<>();
        for (EpisodeAction action : getQueuedEpisodeActions()) {
            Pair<String, String> key = new Pair<>(action.getPodcast(), action.getEpisode());
            EpisodeAction mostRecent = localMostRecentPlayAction.get(key);
            if (mostRecent == null || mostRecent.getTimestamp() == null) {
                localMostRecentPlayAction.put(key, action);
            } else if (mostRecent.getTimestamp().before(action.getTimestamp())) {
                localMostRecentPlayAction.put(key, action);
            }
        }

        // make sure more recent local actions are not overwritten by older remote actions
        Map<Pair<String, String>, EpisodeAction> mostRecentPlayAction = new ArrayMap<>();
        for (EpisodeAction action : remoteActions) {
            switch (action.getAction()) {
                case NEW:
                    FeedItem newItem = DBReader.getFeedItem(action.getPodcast(), action.getEpisode());
                    if (newItem != null) {
                        DBWriter.markItemPlayed(newItem, FeedItem.UNPLAYED, true);
                    } else {
                        Log.i(TAG, "Unknown feed item: " + action);
                    }
                    break;
                case DOWNLOAD:
                    break;
                case PLAY:
                    Pair<String, String> key = new Pair<>(action.getPodcast(), action.getEpisode());
                    EpisodeAction localMostRecent = localMostRecentPlayAction.get(key);
                    if (localMostRecent == null || localMostRecent.getTimestamp() == null
                            || localMostRecent.getTimestamp().before(action.getTimestamp())) {
                        EpisodeAction mostRecent = mostRecentPlayAction.get(key);
                        if (mostRecent == null || mostRecent.getTimestamp() == null) {
                            mostRecentPlayAction.put(key, action);
                        } else if (action.getTimestamp() != null
                                && mostRecent.getTimestamp().before(action.getTimestamp())) {
                            mostRecentPlayAction.put(key, action);
                        } else {
                            Log.d(TAG, "No date information in action, skipping it");
                        }
                    }
                    break;
                case DELETE:
                    // NEVER EVER call DBWriter.deleteFeedMediaOfItem() here, leads to an infinite loop
                    break;
            }
        }

        for (EpisodeAction action : mostRecentPlayAction.values()) {
            FeedItem playItem = DBReader.getFeedItem(action.getPodcast(), action.getEpisode());
            if (playItem != null) {
                FeedMedia media = playItem.getMedia();
                media.setPosition(action.getPosition() * 1000);
                DBWriter.setFeedMedia(media);
                if (playItem.getMedia().hasAlmostEnded()) {
                    DBWriter.markItemPlayed(playItem, FeedItem.PLAYED, true);
                    DBWriter.addItemToPlaybackHistory(playItem.getMedia());
                }
            }
        }
    }

    private void clearErrorNotifications() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(R.id.notification_gpodnet_sync_error);
        nm.cancel(R.id.notification_gpodnet_sync_autherror);
    }

    private void updateErrorNotification(SyncServiceException exception) {
        Log.d(TAG, "Posting error notification");
        final String description = getString(R.string.gpodnetsync_error_descr) + exception.getMessage();

        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_ERROR)
                .setContentTitle(getString(R.string.gpodnetsync_error_title))
                .setContentText(description)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification_sync_error)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(R.id.notification_gpodnet_sync_error, notification);
    }
}
