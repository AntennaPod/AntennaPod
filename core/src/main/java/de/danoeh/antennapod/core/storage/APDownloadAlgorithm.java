package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.PowerUtils;

/**
 * Implements the automatic download algorithm used by AntennaPod. This class assumes that
 * the client uses the APEpisodeCleanupAlgorithm.
 */
public class APDownloadAlgorithm implements AutomaticDownloadAlgorithm {
    private static final String TAG = "APDownloadAlgorithm";

    private final APCleanupAlgorithm cleanupAlgorithm = new APCleanupAlgorithm();

    /**
     * Looks for undownloaded episodes in the queue or list of new items and request a download if
     * 1. Network is available
     * 2. The device is charging or the user allows auto download on battery
     * 3. There is free space in the episode cache
     * This method is executed on an internal single thread executor.
     *
     * @param context  Used for accessing the DB.
     * @param mediaIds If this list is not empty, the method will only download a candidate for automatic downloading if
     *                 its media ID is in the mediaIds list.
     * @return A Runnable that will be submitted to an ExecutorService.
     */
    @Override
    public Runnable autoDownloadUndownloadedItems(final Context context, final long... mediaIds) {
        return new Runnable() {
            @Override
            public void run() {

                // true if we should auto download based on network status
                boolean networkShouldAutoDl = NetworkUtils.autodownloadNetworkAvailable()
                        && UserPreferences.isEnableAutodownload();

                // true if we should auto download based on power status
                boolean powerShouldAutoDl = PowerUtils.deviceCharging(context)
                        || UserPreferences.isEnableAutodownloadOnBattery();

                // we should only auto download if both network AND power are happy
                if (networkShouldAutoDl && powerShouldAutoDl) {

                    Log.d(TAG, "Performing auto-dl of undownloaded episodes");

                    List<FeedItem> candidates;
                    if(mediaIds.length > 0) {
                        candidates = DBReader.getFeedItems(context, mediaIds);
                    } else {
                        final List<FeedItem> queue = DBReader.getQueue(context);
                        final List<FeedItem> newItems = DBReader.getNewItemsList(context);
                        candidates = new ArrayList<FeedItem>(queue.size() + newItems.size());
                        candidates.addAll(queue);
                        for(FeedItem newItem : newItems) {
                            if(candidates.contains(newItem) == false) {
                                candidates.add(newItem);
                            }
                        }
                    }

                    // filter items that are not auto downloadable
                    Iterator<FeedItem> it = candidates.iterator();
                    while(it.hasNext()) {
                        FeedItem item = it.next();
                        if(item.isAutoDownloadable() == false) {
                            it.remove();
                        }
                    }

                    int autoDownloadableEpisodes = candidates.size();
                    int downloadedEpisodes = DBReader.getNumberOfDownloadedEpisodes(context);
                    int deletedEpisodes = cleanupAlgorithm.performCleanup(context,
                            APCleanupAlgorithm.getPerformAutoCleanupArgs(context, autoDownloadableEpisodes));
                    boolean cacheIsUnlimited = UserPreferences.getEpisodeCacheSize() == UserPreferences
                            .getEpisodeCacheSizeUnlimited();
                    int episodeCacheSize = UserPreferences.getEpisodeCacheSize();

                    int episodeSpaceLeft;
                    if (cacheIsUnlimited ||
                            episodeCacheSize >= downloadedEpisodes + autoDownloadableEpisodes) {
                        episodeSpaceLeft = autoDownloadableEpisodes;
                    } else {
                        episodeSpaceLeft = episodeCacheSize - (downloadedEpisodes - deletedEpisodes);
                    }

                    FeedItem[] itemsToDownload = candidates.subList(0, episodeSpaceLeft)
                            .toArray(new FeedItem[episodeSpaceLeft]);

                    Log.d(TAG, "Enqueueing " + itemsToDownload.length + " items for download");

                    try {
                        DBTasks.downloadFeedItems(false, context, itemsToDownload);
                    } catch (DownloadRequestException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
    }

}
