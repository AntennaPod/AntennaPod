package de.danoeh.antennapod.core.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.SafeJobIntentService;
import androidx.collection.ArrayMap;
import android.util.Log;
import android.util.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceAuthenticationException;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeAction;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeActionGetResponse;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeActionPostResponse;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetSubscriptionChange;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetUploadChangesResponse;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.URLChecker;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;

/**
 * Synchronizes local subscriptions with gpodder.net service. The service should be started with ACTION_SYNC as an action argument.
 * This class also provides static methods for starting the GpodnetSyncService.
 */
public class GpodnetSyncService extends SafeJobIntentService {

    private static final String TAG = "GpodnetSyncService";

    private static final long WAIT_INTERVAL = 5000L;

    private static final String ARG_ACTION = "action";

    private static final String ACTION_SYNC = "de.danoeh.antennapod.intent.action.sync";
    private static final String ACTION_SYNC_SUBSCRIPTIONS = "de.danoeh.antennapod.intent.action.sync_subscriptions";
    private static final String ACTION_SYNC_ACTIONS = "de.danoeh.antennapod.intent.action.sync_ACTIONS";

    private GpodnetService service;

    private static final AtomicInteger syncActionCount = new AtomicInteger(0);
    private static boolean syncSubscriptions = false;
    private static boolean syncActions = false;

    private static final int JOB_ID = -17000;

    private static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GpodnetSyncService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String action = intent.getStringExtra(ARG_ACTION);
        if (action != null) {
            switch(action) {
                case ACTION_SYNC:
                    syncSubscriptions = true;
                    syncActions = true;
                    break;
                case ACTION_SYNC_SUBSCRIPTIONS:
                    syncSubscriptions = true;
                    break;
                case ACTION_SYNC_ACTIONS:
                    syncActions = true;
                    break;
                default:
                    Log.e(TAG, "Received invalid intent: action argument is invalid");
            }
            if(syncSubscriptions || syncActions) {
                Log.d(TAG, String.format("Waiting %d milliseconds before uploading changes", WAIT_INTERVAL));
                int syncActionId = syncActionCount.incrementAndGet();
                try {
                    Thread.sleep(WAIT_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (syncActionId == syncActionCount.get()) {
                    // onHandleWork was not called again in the meantime
                    sync();
                }
            }
        } else {
            Log.e(TAG, "Received invalid intent: action argument is null");
        }
    }

    private synchronized GpodnetService tryLogin() throws GpodnetServiceException {
        if (service == null) {
            service = new GpodnetService();
            service.authenticate(GpodnetPreferences.getUsername(), GpodnetPreferences.getPassword());
        }
        return service;
    }


    private synchronized void sync() {
        if (!GpodnetPreferences.loggedIn() || !NetworkUtils.networkAvailable()) {
            stopForeground(true);
            stopSelf();
            return;
        }
        boolean initialSync =  GpodnetPreferences.getLastSubscriptionSyncTimestamp() == 0 &&
                GpodnetPreferences.getLastEpisodeActionsSyncTimestamp() == 0;
        if(syncSubscriptions) {
            syncSubscriptionChanges();
            syncSubscriptions = false;
        }
        if(syncActions) {
            // we only sync episode actions after the subscriptions have been added to the database
            if(!initialSync) {
                syncEpisodeActions();
            }
            syncActions = false;
        }
    }

    private synchronized void syncSubscriptionChanges() {
        final long timestamp = GpodnetPreferences.getLastSubscriptionSyncTimestamp();
        try {
            final List<String> localSubscriptions = DBReader.getFeedListDownloadUrls();
            Collection<String> localAdded = GpodnetPreferences.getAddedFeedsCopy();
            Collection<String> localRemoved = GpodnetPreferences.getRemovedFeedsCopy();
            GpodnetService service = tryLogin();

            // first sync: download all subscriptions...
            GpodnetSubscriptionChange subscriptionChanges = service.getSubscriptionChanges(GpodnetPreferences.getUsername(),
                    GpodnetPreferences.getDeviceID(), timestamp);
            long newTimeStamp = subscriptionChanges.getTimestamp();

            Log.d(TAG, "Downloaded subscription changes: " + subscriptionChanges);
            processSubscriptionChanges(localSubscriptions, localAdded, localRemoved, subscriptionChanges);

            if(timestamp == 0) {
                // this is this apps first sync with gpodder:
                // only submit changes gpodder has not just sent us
                localAdded = localSubscriptions;
                localAdded.removeAll(subscriptionChanges.getAdded());
                localRemoved.removeAll(subscriptionChanges.getRemoved());
            }
            if(localAdded.size() > 0 || localRemoved.size() > 0) {
                Log.d(TAG, String.format("Uploading subscriptions, Added: %s\nRemoved: %s",
                        localAdded, localRemoved));
                GpodnetUploadChangesResponse uploadResponse = service.uploadChanges(GpodnetPreferences.getUsername(),
                        GpodnetPreferences.getDeviceID(), localAdded, localRemoved);
                newTimeStamp = uploadResponse.timestamp;
                Log.d(TAG, "Upload changes response: " + uploadResponse);
                GpodnetPreferences.removeAddedFeeds(localAdded);
                GpodnetPreferences.removeRemovedFeeds(localRemoved);
            }
            GpodnetPreferences.setLastSubscriptionSyncTimestamp(newTimeStamp);
            GpodnetPreferences.setLastSyncAttempt(true, System.currentTimeMillis());
            clearErrorNotifications();
        } catch (GpodnetServiceException e) {
            e.printStackTrace();
            updateErrorNotification(e);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
        }
    }

    private synchronized void processSubscriptionChanges(List<String> localSubscriptions,
                                                         Collection<String> localAdded,
                                                         Collection<String> localRemoved,
                                                         GpodnetSubscriptionChange changes) throws DownloadRequestException {
        // local changes are always superior to remote changes!
        // add subscription if (1) not already subscribed and (2) not just unsubscribed
        for (String downloadUrl : changes.getAdded()) {
            if (!URLChecker.containsUrl(localSubscriptions, downloadUrl) && !localRemoved.contains(downloadUrl)) {
                Feed feed = new Feed(downloadUrl, null);
                DownloadRequester.getInstance().downloadFeed(this, feed);
            }
        }
        // remove subscription if not just subscribed (again)
        for (String downloadUrl : changes.getRemoved()) {
            if (!localAdded.contains(downloadUrl)) {
                DBTasks.removeFeedWithDownloadUrl(GpodnetSyncService.this, downloadUrl);
            }
        }
    }

    private synchronized void syncEpisodeActions() {
        final long timestamp = GpodnetPreferences.getLastEpisodeActionsSyncTimestamp();
        Log.d(TAG, "last episode actions sync timestamp: " + timestamp);
        try {
            GpodnetService service = tryLogin();

            // download episode actions
            GpodnetEpisodeActionGetResponse getResponse = service.getEpisodeChanges(timestamp);
            long lastUpdate = getResponse.getTimestamp();
            Log.d(TAG, "Downloaded episode actions: " + getResponse);
            List<GpodnetEpisodeAction> remoteActions = getResponse.getEpisodeActions();

            List<GpodnetEpisodeAction> localActions = GpodnetPreferences.getQueuedEpisodeActions();
            processEpisodeActions(localActions, remoteActions);

            // upload local actions
            if(localActions.size() > 0) {
                Log.d(TAG, "Uploading episode actions: " + localActions);
                GpodnetEpisodeActionPostResponse postResponse = service.uploadEpisodeActions(localActions);
                lastUpdate = postResponse.timestamp;
                Log.d(TAG, "Upload episode response: " + postResponse);
                GpodnetPreferences.removeQueuedEpisodeActions(localActions);
            }
            GpodnetPreferences.setLastEpisodeActionsSyncTimestamp(lastUpdate);
            GpodnetPreferences.setLastSyncAttempt(true, System.currentTimeMillis());
            clearErrorNotifications();
        } catch (GpodnetServiceException e) {
            e.printStackTrace();
            updateErrorNotification(e);
        }
    }


    private synchronized void processEpisodeActions(List<GpodnetEpisodeAction> localActions,
                                                    List<GpodnetEpisodeAction> remoteActions) {
        if(remoteActions.size() == 0) {
            return;
        }
        Map<Pair<String, String>, GpodnetEpisodeAction> localMostRecentPlayAction = new ArrayMap<>();
        for(GpodnetEpisodeAction action : localActions) {
            Pair<String, String> key = new Pair<>(action.getPodcast(), action.getEpisode());
            GpodnetEpisodeAction mostRecent = localMostRecentPlayAction.get(key);
            if (mostRecent == null || mostRecent.getTimestamp() == null) {
                localMostRecentPlayAction.put(key, action);
            } else if (mostRecent.getTimestamp().before(action.getTimestamp())) {
                localMostRecentPlayAction.put(key, action);
            }
        }

        // make sure more recent local actions are not overwritten by older remote actions
        Map<Pair<String, String>, GpodnetEpisodeAction> mostRecentPlayAction = new ArrayMap<>();
        for (GpodnetEpisodeAction action : remoteActions) {
            switch (action.getAction()) {
                case NEW:
                    FeedItem newItem = DBReader.getFeedItem(action.getPodcast(), action.getEpisode());
                    if(newItem != null) {
                        DBWriter.markItemPlayed(newItem, FeedItem.UNPLAYED, true);
                    } else {
                        Log.i(TAG, "Unknown feed item: " + action);
                    }
                    break;
                case DOWNLOAD:
                    break;
                case PLAY:
                    Pair<String, String> key = new Pair<>(action.getPodcast(), action.getEpisode());
                    GpodnetEpisodeAction localMostRecent = localMostRecentPlayAction.get(key);
                    if(localMostRecent == null ||
                            localMostRecent.getTimestamp() == null ||
                            localMostRecent.getTimestamp().before(action.getTimestamp())) {
                        GpodnetEpisodeAction mostRecent = mostRecentPlayAction.get(key);
                        if (mostRecent == null || mostRecent.getTimestamp() == null) {
                            mostRecentPlayAction.put(key, action);
                        } else if (action.getTimestamp() != null && mostRecent.getTimestamp().before(action.getTimestamp())) {
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
        for (GpodnetEpisodeAction action : mostRecentPlayAction.values()) {
            FeedItem playItem = DBReader.getFeedItem(action.getPodcast(), action.getEpisode());
            if (playItem != null) {
                FeedMedia media = playItem.getMedia();
                media.setPosition(action.getPosition() * 1000);
                DBWriter.setFeedMedia(media);
                if(playItem.getMedia().hasAlmostEnded()) {
                    DBWriter.markItemPlayed(playItem, FeedItem.PLAYED, true);
                    DBWriter.addItemToPlaybackHistory(playItem.getMedia());
                }
            }
        }
    }

    private void clearErrorNotifications() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(R.id.notification_gpodnet_sync_error);
        nm.cancel(R.id.notification_gpodnet_sync_autherror);
    }

    private void updateErrorNotification(GpodnetServiceException exception) {
     Log.d(TAG, "Posting error notification");
        GpodnetPreferences.setLastSyncAttempt(false, System.currentTimeMillis());

        final String title;
        final String description;
        final int id;
        if (exception instanceof GpodnetServiceAuthenticationException) {
            title = getString(R.string.gpodnetsync_auth_error_title);
            description = getString(R.string.gpodnetsync_auth_error_descr);
            id = R.id.notification_gpodnet_sync_autherror;
        } else {
            if (UserPreferences.gpodnetNotificationsEnabled()) {
                title = getString(R.string.gpodnetsync_error_title);
                description = getString(R.string.gpodnetsync_error_descr) + exception.getMessage();
                id = R.id.notification_gpodnet_sync_error;
            } else {
                return;
            }
        }

        PendingIntent activityIntent = ClientConfig.gpodnetCallbacks.getGpodnetSyncServiceErrorNotificationPendingIntent(this);
        Notification notification = new NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_ERROR)
                .setContentTitle(title)
                .setContentText(description)
                .setContentIntent(activityIntent)
                .setSmallIcon(R.drawable.stat_notify_sync_error)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, notification);
    }

    public static void sendSyncIntent(Context context) {
        if (GpodnetPreferences.loggedIn()) {
            Intent intent = new Intent(context, GpodnetSyncService.class);
            intent.putExtra(ARG_ACTION, ACTION_SYNC);
            enqueueWork(context, intent);
        }
    }

    public static void sendSyncSubscriptionsIntent(Context context) {
        if (GpodnetPreferences.loggedIn()) {
            Intent intent = new Intent(context, GpodnetSyncService.class);
            intent.putExtra(ARG_ACTION, ACTION_SYNC_SUBSCRIPTIONS);
            enqueueWork(context, intent);
        }
    }

    public static void sendSyncActionsIntent(Context context) {
        if (GpodnetPreferences.loggedIn()) {
            Intent intent = new Intent(context, GpodnetSyncService.class);
            intent.putExtra(ARG_ACTION, ACTION_SYNC_ACTIONS);
            enqueueWork(context, intent);
        }
    }
}
