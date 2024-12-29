package de.danoeh.antennapod.net.sync.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.SyncServiceEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.net.common.UrlChecker;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.net.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.net.sync.nextcloud.NextcloudSyncService;
import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeAction;
import de.danoeh.antennapod.net.sync.serviceinterface.EpisodeActionChanges;
import de.danoeh.antennapod.net.sync.serviceinterface.ISyncService;
import de.danoeh.antennapod.net.sync.serviceinterface.SubscriptionChanges;
import de.danoeh.antennapod.net.sync.serviceinterface.SyncServiceException;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationProvider;
import de.danoeh.antennapod.net.sync.serviceinterface.UploadChangesResponse;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.storage.database.LongList;
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials;
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SyncService extends Worker {
    public static final String TAG = "SyncService";

    private static boolean currentlyActive = false;
    private final SynchronizationQueueStorage synchronizationQueueStorage;

    public SyncService(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        synchronizationQueueStorage = new SynchronizationQueueStorage(context);
    }

    @Override
    @NonNull
    public Result doWork() {
        ISyncService activeSyncProvider = getActiveSyncProvider();
        if (activeSyncProvider == null) {
            return Result.success();
        }

        if (currentlyActive) {
            return Result.success();
        }
        currentlyActive = true;
        SynchronizationSettings.updateLastSynchronizationAttempt();
        try {
            activeSyncProvider.login();
            syncSubscriptions(activeSyncProvider);
            waitForDownloadServiceCompleted();
            if (someFeedWasNotRefreshedYet()) {
                // Note that this service might get called several times before the FeedUpdate completes
                Log.d(TAG, "Found new subscriptions. Need to refresh them before syncing episode actions");
                EventBus.getDefault().postSticky(new SyncServiceEvent(R.string.sync_status_wait_for_downloads));
                FeedUpdateManager.getInstance().runOnce(getApplicationContext());
                return Result.success();
            }
            syncEpisodeActions(activeSyncProvider);
            activeSyncProvider.logout();
            clearErrorNotifications();
            EventBus.getDefault().postSticky(new SyncServiceEvent(R.string.sync_status_success));
            SynchronizationSettings.setLastSynchronizationAttemptSuccess(true);
            return Result.success();
        } catch (Exception e) {
            EventBus.getDefault().postSticky(new SyncServiceEvent(R.string.sync_status_error));
            SynchronizationSettings.setLastSynchronizationAttemptSuccess(false);
            Log.e(TAG, Log.getStackTraceString(e));

            if (e instanceof SyncServiceException) {
                if (getRunAttemptCount() % 3 == 2) {
                    // Do not spam users with notification and retry before notifying
                    updateErrorNotification(e);
                }
                return Result.retry();
            } else {
                updateErrorNotification(e);
                return Result.failure();
            }
        } finally {
            currentlyActive = false;
        }
    }

    private void waitForDownloadServiceCompleted() {
        EventBus.getDefault().postSticky(new SyncServiceEvent(R.string.sync_status_wait_for_downloads));
        try {
            while (true) {
                FeedUpdateRunningEvent event = EventBus.getDefault().getStickyEvent(FeedUpdateRunningEvent.class);
                if (event == null || !event.isFeedUpdateRunning) {
                    return;
                }
                //noinspection BusyWait
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean someFeedWasNotRefreshedYet() {
        for (Feed feed : DBReader.getFeedList()) {
            if (feed.getPreferences().getKeepUpdated() && feed.getLastRefreshAttempt() == 0) {
                return true;
            }
        }
        return false;
    }

    /* package-private */ static boolean isCurrentlyActive() {
        return currentlyActive;
    }

    private void syncSubscriptions(ISyncService syncServiceImpl) throws SyncServiceException {
        final long lastSync = SynchronizationSettings.getLastSubscriptionSynchronizationTimestamp();
        EventBus.getDefault().postSticky(new SyncServiceEvent(R.string.sync_status_subscriptions));
        final List<String> localSubscriptions = DBReader.getFeedListDownloadUrls();
        SubscriptionChanges subscriptionChanges = syncServiceImpl.getSubscriptionChanges(lastSync);
        long newTimeStamp = subscriptionChanges.getTimestamp();

        List<String> queuedRemovedFeeds = synchronizationQueueStorage.getQueuedRemovedFeeds();
        List<String> queuedAddedFeeds = synchronizationQueueStorage.getQueuedAddedFeeds();

        Log.d(TAG, "Downloaded subscription changes: " + subscriptionChanges);
        for (String downloadUrl : subscriptionChanges.getAdded()) {
            if (!downloadUrl.startsWith("http")) { // Also matches https
                Log.d(TAG, "Skipping url: " + downloadUrl);
                continue;
            }
            if (!UrlChecker.containsUrl(localSubscriptions, downloadUrl) && !queuedRemovedFeeds.contains(downloadUrl)) {
                Feed feed = new Feed(downloadUrl, null, "Unknown podcast");
                feed.setItems(Collections.emptyList());
                FeedDatabaseWriter.updateFeed(getApplicationContext(), feed, false);
            }
        }

        // remove subscription if not just subscribed (again)
        for (String downloadUrl : subscriptionChanges.getRemoved()) {
            if (!queuedAddedFeeds.contains(downloadUrl)) {
                DBWriter.removeFeedWithDownloadUrl(getApplicationContext(), downloadUrl);
            }
        }

        if (lastSync == 0) {
            Log.d(TAG, "First sync. Adding all local subscriptions.");
            queuedAddedFeeds = localSubscriptions;
        }

        queuedAddedFeeds.removeAll(subscriptionChanges.getAdded());
        queuedRemovedFeeds.removeAll(subscriptionChanges.getRemoved());

        if (queuedAddedFeeds.isEmpty() && queuedRemovedFeeds.isEmpty()) {
            Log.d(TAG, "No feeds to add or remove from server");
            synchronizationQueueStorage.clearFeedQueues();
        } else {
            Log.d(TAG, "Added: " + StringUtils.join(queuedAddedFeeds, ", "));
            Log.d(TAG, "Removed: " + StringUtils.join(queuedRemovedFeeds, ", "));

            LockingAsyncExecutor.lock();
            try {
                UploadChangesResponse uploadResponse = syncServiceImpl
                        .uploadSubscriptionChanges(queuedAddedFeeds, queuedRemovedFeeds);
                synchronizationQueueStorage.clearFeedQueues();
                newTimeStamp = uploadResponse.timestamp;
            } finally {
                LockingAsyncExecutor.unlock();
            }
        }
        SynchronizationSettings.setLastSubscriptionSynchronizationAttemptTimestamp(newTimeStamp);
    }

    private void syncEpisodeActions(ISyncService syncServiceImpl) throws SyncServiceException {
        final long lastSync = SynchronizationSettings.getLastEpisodeActionSynchronizationTimestamp();
        EventBus.getDefault().postSticky(new SyncServiceEvent(R.string.sync_status_episodes_download));
        EpisodeActionChanges getResponse = syncServiceImpl.getEpisodeActionChanges(lastSync);
        long newTimeStamp = getResponse.getTimestamp();
        List<EpisodeAction> remoteActions = getResponse.getEpisodeActions();
        processEpisodeActions(remoteActions);

        // upload local actions
        EventBus.getDefault().postSticky(new SyncServiceEvent(R.string.sync_status_episodes_upload));
        List<EpisodeAction> queuedEpisodeActions = synchronizationQueueStorage.getQueuedEpisodeActions();
        if (lastSync == 0) {
            EventBus.getDefault().postSticky(new SyncServiceEvent(R.string.sync_status_upload_played));
            List<FeedItem> readItems = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                    new FeedItemFilter(FeedItemFilter.PLAYED), SortOrder.DATE_NEW_OLD);
            Log.d(TAG, "First sync. Upload state for all " + readItems.size() + " played episodes");
            for (FeedItem item : readItems) {
                FeedMedia media = item.getMedia();
                if (media == null) {
                    continue;
                }
                EpisodeAction played = new EpisodeAction.Builder(item, EpisodeAction.PLAY)
                        .currentTimestamp()
                        .started(media.getDuration() / 1000)
                        .position(media.getDuration() / 1000)
                        .total(media.getDuration() / 1000)
                        .build();
                queuedEpisodeActions.add(played);
            }
        }
        if (!queuedEpisodeActions.isEmpty()) {
            LockingAsyncExecutor.lock();
            try {
                Log.d(TAG, "Uploading " + queuedEpisodeActions.size() + " actions: "
                        + StringUtils.join(queuedEpisodeActions, ", "));
                UploadChangesResponse postResponse = syncServiceImpl.uploadEpisodeActions(queuedEpisodeActions);
                newTimeStamp = postResponse.timestamp;
                Log.d(TAG, "Upload episode response: " + postResponse);
                synchronizationQueueStorage.clearEpisodeActionQueue();
            } finally {
                LockingAsyncExecutor.unlock();
            }
        }
        SynchronizationSettings.setLastEpisodeActionSynchronizationAttemptTimestamp(newTimeStamp);
    }

    private synchronized void processEpisodeActions(List<EpisodeAction> remoteActions) {
        Log.d(TAG, "Processing " + remoteActions.size() + " actions");
        if (remoteActions.isEmpty()) {
            return;
        }

        Map<Pair<String, String>, EpisodeAction> playActionsToUpdate = EpisodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions,
                        synchronizationQueueStorage.getQueuedEpisodeActions());
        LongList queueToBeRemoved = new LongList();
        List<FeedItem> updatedItems = new ArrayList<>();
        for (EpisodeAction action : playActionsToUpdate.values()) {
            String guid = GuidValidator.isValidGuid(action.getGuid()) ? action.getGuid() : null;
            FeedItem feedItem = DBReader.getFeedItemByGuidOrEpisodeUrl(guid, action.getEpisode());
            if (feedItem == null) {
                Log.i(TAG, "Unknown feed item: " + action);
                continue;
            }
            if (feedItem.getMedia() == null) {
                Log.i(TAG, "Feed item has no media: " + action);
                continue;
            }
            FeedMedia media = feedItem.getMedia();
            media.setPosition(action.getPosition() * 1000);
            int smartMarkAsPlayedSecs = UserPreferences.getSmartMarkAsPlayedSecs();
            boolean almostEnded = media.getDuration() > 0
                    && media.getPosition() >= media.getDuration() - smartMarkAsPlayedSecs * 1000;
            if (almostEnded) {
                Log.d(TAG, "Marking as played: " + action);
                feedItem.setPlayed(true);
                media.setPosition(0);
                queueToBeRemoved.add(feedItem.getId());
            } else {
                Log.d(TAG, "Setting position: " + action);
            }
            updatedItems.add(feedItem);
        }
        DBWriter.removeQueueItem(getApplicationContext(), false, queueToBeRemoved.toArray());
        DBReader.loadAdditionalFeedItemListData(updatedItems);
        DBWriter.setItemList(updatedItems);
    }

    private void clearErrorNotifications() {
        NotificationManager nm = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(R.id.notification_gpodnet_sync_error);
        nm.cancel(R.id.notification_gpodnet_sync_autherror);
    }

    private void updateErrorNotification(Exception exception) {
        Log.d(TAG, "Posting sync error notification");
        final String description = getApplicationContext().getString(R.string.gpodnetsync_error_descr)
                + exception.getMessage();

        if (!UserPreferences.gpodnetNotificationsEnabled()) {
            Log.d(TAG, "Skipping sync error notification because of user setting");
            return;
        }
        if (EventBus.getDefault().hasSubscriberForEvent(MessageEvent.class)) {
            EventBus.getDefault().post(new MessageEvent(description));
            return;
        }

        Intent intent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(
                getApplicationContext().getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                R.id.pending_intent_sync_error, intent, PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification notification = new NotificationCompat.Builder(getApplicationContext(),
                NotificationUtils.CHANNEL_ID_SYNC_ERROR)
                .setContentTitle(getApplicationContext().getString(R.string.gpodnetsync_error_title))
                .setContentText(description)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification_sync_error)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        NotificationManager nm = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            nm.notify(R.id.notification_gpodnet_sync_error, notification);
        }
    }

    private ISyncService getActiveSyncProvider() {
        String selectedSyncProviderKey = SynchronizationSettings.getSelectedSyncProviderKey();
        SynchronizationProvider selectedService = SynchronizationProvider
                .fromIdentifier(selectedSyncProviderKey);
        if (selectedService == null) {
            return null;
        }
        switch (selectedService) {
            case GPODDER_NET:
                return new GpodnetService(AntennapodHttpClient.getHttpClient(),
                        SynchronizationCredentials.getHosturl(), SynchronizationCredentials.getDeviceId(),
                        SynchronizationCredentials.getUsername(), SynchronizationCredentials.getPassword());
            case NEXTCLOUD_GPODDER:
                return new NextcloudSyncService(AntennapodHttpClient.getHttpClient(),
                        SynchronizationCredentials.getHosturl(), SynchronizationCredentials.getUsername(),
                        SynchronizationCredentials.getPassword());
            default:
                return null;
        }
    }
}
