package de.danoeh.antennapod.net.download.service.episode.autodownload;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.net.common.NetworkUtils;

/**
 * Implements the automatic download algorithm used by AntennaPod. This class assumes that
 * the client uses the {@link EpisodeCleanupAlgorithm}.
 */
public class AutomaticDownloadAlgorithm {
    private static final String TAG = "DownloadAlgorithm";

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
                EpisodeCleanupAlgorithmFactory.build().makeRoomForEpisodes(context, autoDownloadableEpisodes);

                boolean cacheIsUnlimited =
                        UserPreferences.getEpisodeCacheSize() == UserPreferences.EPISODE_CACHE_SIZE_UNLIMITED;
                int episodeCacheSize = UserPreferences.getEpisodeCacheSize();

                Map<Long, List<FeedItem>> candidatesByFeed = new LinkedHashMap<>();
                for (FeedItem item : candidates) {
                    long feedId = item.getFeed().getId();
                    if (!candidatesByFeed.containsKey(feedId)) {
                        candidatesByFeed.put(feedId, new ArrayList<>());
                    }
                    candidatesByFeed.get(feedId).add(item);
                }

                List<FeedItem> itemsToDownload = new ArrayList<>();

                for (Map.Entry<Long, List<FeedItem>> entry : candidatesByFeed.entrySet()) {
                    List<FeedItem> feedCandidates = entry.getValue();
                    if (feedCandidates.isEmpty()) {
                        continue;
                    }
                    Feed feed = feedCandidates.get(0).getFeed();

                    if (cacheIsUnlimited) {
                        itemsToDownload.addAll(feedCandidates);
                    } else {
                        List<FeedItem> downloadedItems = DBReader.getFeedItemList(feed,
                                new FeedItemFilter(FeedItemFilter.DOWNLOADED), SortOrder.DATE_OLD_NEW, 0, Integer.MAX_VALUE);
                        int downloadedCount = downloadedItems.size();

                        for (FeedItem candidate : feedCandidates) {
                            if (candidate.hasMedia()
                                    && DownloadServiceInterface.get().isDownloadingEpisode(candidate.getMedia().getDownloadUrl())) {
                                downloadedCount++;
                            }
                        }

                        int spaceLeft = episodeCacheSize - downloadedCount;
                        if (spaceLeft > 0) {
                            int toTake = Math.min(spaceLeft, feedCandidates.size());
                            itemsToDownload.addAll(feedCandidates.subList(0, toTake));
                        }
                    }
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
