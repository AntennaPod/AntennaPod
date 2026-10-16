package de.danoeh.antennapod.net.download.service.episode.autodownload;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.net.download.service.R;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;

/**
 * Implements the automatic download algorithm used by AntennaPod. This class assumes that
 * the client uses the {@link EpisodeCleanupAlgorithm}.
 */
public class AutomaticDownloadAlgorithm {
    private static final String TAG = "DownloadAlgorithm";
    private static final String PREF_NAME = "AutomaticDownloadAlgorithm";
    private static final String PREF_LIMIT_NOTIFICATION_SHOWN = "episodeLimitNotificationShown";
    private static final String PREF_LIMIT_LOGGED_MEDIA = "episodeLimitLoggedMedia";
    private static final int MAX_LIMIT_LOG_ENTRIES_PER_RUN = 10;

    /**
     * Looks for undownloaded episodes in the queue or list of new items and request a download if
     * 1. Network is available
     * 2. The device is charging or the user allows auto download on battery
     * 3. There is free space in the episode cache
     * This method is executed on an internal single thread executor.
     *
     * @param context  Used for accessing the DB.
     * @return A Runnable that will be submitted to an ExecutorService.
     */
    public Runnable autoDownloadUndownloadedItems(final Context context) {
        return () -> {

            // true if we should auto download based on network status
            boolean networkShouldAutoDl = NetworkUtils.isAutoDownloadAllowed();

            // true if we should auto download based on power status
            boolean powerShouldAutoDl = deviceCharging(context) || UserPreferences.isEnableAutodownloadOnBattery();

            // we should only auto download if both network AND power are happy
            if (networkShouldAutoDl && powerShouldAutoDl) {

                Log.d(TAG, "Performing auto-dl of undownloaded episodes");

                boolean globalAutoDownloadEnabled = UserPreferences.isEnableAutodownloadGlobal();
                boolean autoDownloadQueueEnabled = UserPreferences.isEnableAutodownloadQueue();
                final List<FeedItem> newItems = DBReader.getAutoDownloadCandidates(
                        globalAutoDownloadEnabled, autoDownloadQueueEnabled);
                final List<FeedItem> candidates = new ArrayList<>();

                for (FeedItem newItem : newItems) {
                    FeedPreferences feedPrefs = newItem.getFeed().getPreferences();
                    boolean shouldAdd = (autoDownloadQueueEnabled && newItem.isTagged(FeedItem.TAG_QUEUE))
                            || (feedPrefs.isAutoDownload(globalAutoDownloadEnabled)
                            && feedPrefs.getFilter().shouldAutoDownload(newItem));
                    if (shouldAdd) {
                        candidates.add(newItem);
                    }
                }

                // filter items that are not auto downloadable
                Iterator<FeedItem> it = candidates.iterator();
                while (it.hasNext()) {
                    FeedItem item = it.next();
                    if (!item.isAutoDownloadEnabled()
                            || item.isDownloaded()
                            || !item.hasMedia()
                            || item.getFeed().isLocalFeed()) {
                        it.remove();
                    }
                }

                int autoDownloadableEpisodes = candidates.size();
                int downloadedEpisodes = DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.DOWNLOADED));
                downloadedEpisodes += DownloadServiceInterface.get().getNumberOfActiveDownloads(context);
                int deletedEpisodes = EpisodeCleanupAlgorithmFactory.build()
                        .makeRoomForEpisodes(context, autoDownloadableEpisodes);
                boolean cacheIsUnlimited =
                        UserPreferences.getEpisodeCacheSize() == UserPreferences.EPISODE_CACHE_SIZE_UNLIMITED;
                int episodeCacheSize = UserPreferences.getEpisodeCacheSize();

                int episodeSpaceLeft;
                if (cacheIsUnlimited || episodeCacheSize >= downloadedEpisodes + autoDownloadableEpisodes) {
                    episodeSpaceLeft = autoDownloadableEpisodes;
                } else {
                    episodeSpaceLeft = episodeCacheSize - (downloadedEpisodes - deletedEpisodes);
                }
                // More episodes can be downloaded than the limit (e.g. manually), so this can become negative
                episodeSpaceLeft = Math.max(0, Math.min(episodeSpaceLeft, autoDownloadableEpisodes));

                List<FeedItem> itemsToDownload = candidates.subList(0, episodeSpaceLeft);
                int skippedEpisodes = autoDownloadableEpisodes - episodeSpaceLeft;
                if (skippedEpisodes > 0) {
                    Log.d(TAG, "Episode limit reached, not downloading " + skippedEpisodes + " episodes");
                    reportEpisodeLimitReached(context,
                            candidates.subList(episodeSpaceLeft, autoDownloadableEpisodes), episodeCacheSize);
                } else {
                    resetEpisodeLimitReached(context);
                }
                if (!itemsToDownload.isEmpty()) {
                    Log.d(TAG, "Enqueueing " + itemsToDownload.size() + " items for download");

                    for (FeedItem episode : itemsToDownload) {
                        DownloadServiceInterface.get().download(context, episode);
                    }
                }
            }
        };
    }

    /**
     * Tells the user that new episodes were not downloaded because the episode limit is reached.
     * Without this, it looks like automatic download is broken. Each skipped episode gets an entry in the
     * download log (which also works if notifications are not allowed) and a notification is shown once
     * each time the limit is reached, so that refreshing does not show it over and over again.
     */
    private static void reportEpisodeLimitReached(Context context, List<FeedItem> skippedItems, int limit) {
        String explanation;
        if (UserPreferences.getEpisodeCleanupValue() == UserPreferences.EPISODE_CLEANUP_NULL) {
            explanation = context.getString(R.string.episode_limit_reached_no_cleanup, limit);
        } else {
            explanation = context.getString(R.string.episode_limit_reached_nothing_to_clean, limit);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> loggedMedia = new HashSet<>(
                prefs.getStringSet(PREF_LIMIT_LOGGED_MEDIA, Collections.emptySet()));
        int newLogEntries = 0;
        for (FeedItem item : skippedItems) {
            FeedMedia media = item.getMedia();
            if (newLogEntries >= MAX_LIMIT_LOG_ENTRIES_PER_RUN) {
                break;
            } else if (media == null || !loggedMedia.add(String.valueOf(media.getId()))) {
                continue;
            }
            DBWriter.addDownloadStatus(new DownloadResult(item.getTitle(), media.getId(),
                    FeedMedia.FEEDFILETYPE_FEEDMEDIA, false, DownloadError.ERROR_EPISODE_LIMIT_REACHED, explanation));
            newLogEntries++;
        }
        SharedPreferences.Editor editor = prefs.edit().putStringSet(PREF_LIMIT_LOGGED_MEDIA, loggedMedia);

        if (!prefs.getBoolean(PREF_LIMIT_NOTIFICATION_SHOWN, false)) {
            showEpisodeLimitReachedNotification(context, skippedItems.size(), explanation);
            editor.putBoolean(PREF_LIMIT_NOTIFICATION_SHOWN, true);
        }
        editor.apply();
    }

    private static void showEpisodeLimitReachedNotification(Context context, int skippedEpisodes,
                                                            String explanation) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String text = context.getResources().getQuantityString(
                R.plurals.episode_limit_reached_notification_text, skippedEpisodes, skippedEpisodes);
        Intent intent = new MainActivityStarter(context).withDownloadLogsOpen().getIntent();
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                R.id.pending_intent_download_service_autodownload_report, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NotificationUtils.CHANNEL_ID_DOWNLOAD_ERROR)
                .setContentTitle(context.getString(R.string.episode_limit_reached_notification_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text + "\n" + explanation))
                .setSmallIcon(R.drawable.ic_notification_sync_error)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(R.id.notification_auto_download_report, builder.build());
    }

    private static void resetEpisodeLimitReached(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(PREF_LIMIT_NOTIFICATION_SHOWN) && !prefs.contains(PREF_LIMIT_LOGGED_MEDIA)) {
            return;
        }
        prefs.edit().remove(PREF_LIMIT_NOTIFICATION_SHOWN).remove(PREF_LIMIT_LOGGED_MEDIA).apply();
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(R.id.notification_auto_download_report);
    }

    /**
     * @return true if the device is charging
     */
    public static boolean deviceCharging(Context context) {
        // from http://developer.android.com/training/monitoring-device-state/battery-monitoring.html
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intentFilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return (status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL);

    }
}
