package de.danoeh.antennapod.core.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceAuthenticationException;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeAction;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeActionGetResponse;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetEpisodeActionPostResponse;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetSubscriptionChange;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetUploadChangesResponse;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.NetworkUtils;

/**
 * Synchronizes local subscriptions with gpodder.net service. The service should be started with ACTION_SYNC as an action argument.
 * This class also provides static methods for starting the GpodnetSyncService.
 */
public class GpodnetSyncService extends Service {
    private static final String TAG = "GpodnetSyncService";

    private static final long WAIT_INTERVAL = 5000L;

    public static final String ARG_ACTION = "action";

    public static final String ACTION_SYNC = "de.danoeh.antennapod.intent.action.sync";

    private GpodnetService service;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = (intent != null) ? intent.getStringExtra(ARG_ACTION) : null;
        if (action != null && action.equals(ACTION_SYNC)) {
        Log.d(TAG, String.format("Waiting %d milliseconds before uploading changes", WAIT_INTERVAL));
            syncWaiterThread.restart();
        } else {
            Log.e(TAG, "Received invalid intent: action argument is null or invalid");
        }
        return START_FLAG_REDELIVERY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        syncWaiterThread.interrupt();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private synchronized GpodnetService tryLogin() throws GpodnetServiceException {
        if (service == null) {
            service = new GpodnetService();
            service.authenticate(GpodnetPreferences.getUsername(), GpodnetPreferences.getPassword());
        }
        return service;
    }


    private synchronized void sync() {
        if (GpodnetPreferences.loggedIn() == false || NetworkUtils.networkAvailable(this) == false) {
            stopSelf();
            return;
        }
        syncSubscriptionChanges();
        syncEpisodeActions();
        stopSelf();
    }

    private synchronized void syncSubscriptionChanges() {
        final long timestamp = GpodnetPreferences.getLastSubscriptionSyncTimestamp();
        try {
            final List<String> localSubscriptions = DBReader.getFeedListDownloadUrls(this);
            GpodnetService service = tryLogin();

            // first sync: download all subscriptions...
            GpodnetSubscriptionChange subscriptionChanges = service.getSubscriptionChanges(GpodnetPreferences.getUsername(),
                    GpodnetPreferences.getDeviceID(), timestamp);
            long lastUpdate = subscriptionChanges.getTimestamp();

            Log.d(TAG, "Downloaded subscription changes: " + subscriptionChanges);
            processSubscriptionChanges(localSubscriptions, subscriptionChanges);

            Collection<String> added;
            Collection<String> removed;
            if (timestamp == 0) {
                added = localSubscriptions;
                GpodnetPreferences.removeRemovedFeeds(GpodnetPreferences.getRemovedFeedsCopy());
                removed = Collections.emptyList();
            } else {
                added = GpodnetPreferences.getAddedFeedsCopy();
                removed = GpodnetPreferences.getRemovedFeedsCopy();
            }
            if(added.size() > 0 || removed.size() > 0) {
                Log.d(TAG, String.format("Uploading subscriptions, Added: %s\nRemoved: %s",
                        added, removed));
                GpodnetUploadChangesResponse uploadResponse = service.uploadChanges(GpodnetPreferences.getUsername(),
                        GpodnetPreferences.getDeviceID(), added, removed);
                lastUpdate = uploadResponse.timestamp;
                Log.d(TAG, "Upload changes response: " + uploadResponse);
                GpodnetPreferences.removeAddedFeeds(added);
                GpodnetPreferences.removeRemovedFeeds(removed);
            }
            GpodnetPreferences.setLastSubscriptionSyncTimestamp(lastUpdate);
            clearErrorNotifications();
        } catch (GpodnetServiceException e) {
            e.printStackTrace();
            updateErrorNotification(e);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
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
            clearErrorNotifications();
        } catch (GpodnetServiceException e) {
            e.printStackTrace();
            updateErrorNotification(e);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
        }
    }

    private synchronized void processSubscriptionChanges(List<String> localSubscriptions, GpodnetSubscriptionChange changes) throws DownloadRequestException {
        for (String downloadUrl : changes.getAdded()) {
            if (!localSubscriptions.contains(downloadUrl)) {
                Feed feed = new Feed(downloadUrl, new Date());
                DownloadRequester.getInstance().downloadFeed(this, feed);
            }
        }
        for (String downloadUrl : changes.getRemoved()) {
            DBTasks.removeFeedWithDownloadUrl(GpodnetSyncService.this, downloadUrl);
        }
    }

    private synchronized void processEpisodeActions(List<GpodnetEpisodeAction> localActions, List<GpodnetEpisodeAction> remoteActions) throws DownloadRequestException {
        if(remoteActions.size() == 0) {
            return;
        }
        Map<Pair<String, String>, GpodnetEpisodeAction> localMostRecentPlayAction = new HashMap<Pair<String, String>, GpodnetEpisodeAction>();
        Map<Pair<String, String>, GpodnetEpisodeAction> remoteMostRecentPlayAction = new HashMap<Pair<String, String>, GpodnetEpisodeAction>();
        // make sure more recent local actions are not overwritten by older remote actions
        for(GpodnetEpisodeAction action : localActions) {
            Pair key = new Pair(action.getPodcast(), action.getEpisode());
            GpodnetEpisodeAction mostRecent = localMostRecentPlayAction.get(key);
            if (mostRecent == null) {
                localMostRecentPlayAction.put(key, action);
            } else if (mostRecent.getTimestamp().before(action.getTimestamp())) {
                localMostRecentPlayAction.put(key, action);
            }
        }
        for (GpodnetEpisodeAction action : remoteActions) {
            switch (action.getAction()) {
                case NEW:
                    FeedItem newItem = DBReader.getFeedItem(this, action.getPodcast(), action.getEpisode());
                    if(newItem != null) {
                        DBWriter.markItemRead(this, newItem, false, true);
                    } else {
                        Log.i(TAG, "Unknown feed item: " + action);
                    }
                    break;
                case DOWNLOAD:
                    break;
                case PLAY:
                    Pair key = new Pair(action.getPodcast(), action.getEpisode());
                    GpodnetEpisodeAction localMostRecent = localMostRecentPlayAction.get(key);
                    if(localMostRecent == null ||
                            localMostRecent.getTimestamp().before(action.getTimestamp())) {
                        GpodnetEpisodeAction mostRecent = remoteMostRecentPlayAction.get(key);
                        if (mostRecent == null) {
                            remoteMostRecentPlayAction.put(key, action);
                        } else if (mostRecent.getTimestamp().before(action.getTimestamp())) {
                            remoteMostRecentPlayAction.put(key, action);
                        }
                    }
                    break;
                case DELETE:
                    // NEVER EVER call DBWriter.deleteFeedMediaOfItem() here, leads to an infinite loop
                    break;
            }
        }
        for (GpodnetEpisodeAction action : remoteMostRecentPlayAction.values()) {
            FeedItem playItem = DBReader.getFeedItem(this, action.getPodcast(), action.getEpisode());
            if (playItem != null) {
                playItem.getMedia().setPosition(action.getPosition() * 1000);
                if(playItem.getMedia().hasAlmostEnded()) {
                    DBWriter.markItemRead(this, playItem, true, true);
                    DBWriter.addItemToPlaybackHistory(this, playItem.getMedia());
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        final String title;
        final String description;
        final int id;
        if (exception instanceof GpodnetServiceAuthenticationException) {
            title = getString(R.string.gpodnetsync_auth_error_title);
            description = getString(R.string.gpodnetsync_auth_error_descr);
            id = R.id.notification_gpodnet_sync_autherror;
        } else {
            title = getString(R.string.gpodnetsync_error_title);
            description = getString(R.string.gpodnetsync_error_descr) + exception.getMessage();
            id = R.id.notification_gpodnet_sync_error;
        }

        PendingIntent activityIntent = ClientConfig.gpodnetCallbacks.getGpodnetSyncServiceErrorNotificationPendingIntent(this);
        Notification notification = builder.setContentTitle(title)
                .setContentText(description)
                .setContentIntent(activityIntent)
                .setSmallIcon(R.drawable.stat_notify_sync_error)
                .setAutoCancel(true)
                .build();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, notification);
    }

    private WaiterThread syncWaiterThread = new WaiterThread(WAIT_INTERVAL) {
        @Override
        public void onWaitCompleted() {
            sync();
        }
    };

    private abstract class WaiterThread {
        private long waitInterval;
        private Thread thread;

        private WaiterThread(long waitInterval) {
            this.waitInterval = waitInterval;
            reinit();
        }

        public abstract void onWaitCompleted();

        public void exec() {
            if (!thread.isAlive()) {
                thread.start();
            }
        }

        private void reinit() {
            if (thread != null && thread.isAlive()) {
            Log.d(TAG, "Interrupting waiter thread");
                thread.interrupt();
            }
            thread = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(waitInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!isInterrupted()) {
                        synchronized (this) {
                            onWaitCompleted();
                        }
                    }
                }
            };
        }

        public void restart() {
            reinit();
            exec();
        }

        public void interrupt() {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }
    }

    public static void sendSyncIntent(Context context) {
        if (GpodnetPreferences.loggedIn()) {
            Intent intent = new Intent(context, GpodnetSyncService.class);
            intent.putExtra(ARG_ACTION, ACTION_SYNC);
            context.startService(intent);
        }
    }
}
