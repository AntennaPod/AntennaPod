package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.core.BuildConfig;
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
     * Looks for undownloaded episodes in the queue or list of unread items and request a download if
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
                boolean networkShouldAutoDl = NetworkUtils.autodownloadNetworkAvailable(context)
                        && UserPreferences.isEnableAutodownload();

                // true if we should auto download based on power status
                boolean powerShouldAutoDl = PowerUtils.deviceCharging(context)
                        || UserPreferences.isEnableAutodownloadOnBattery();

                // we should only auto download if both network AND power are happy
                if (networkShouldAutoDl && powerShouldAutoDl) {

                    Log.d(TAG, "Performing auto-dl of undownloaded episodes");

                    final List<FeedItem> queue = DBReader.getQueue(context);
                    final List<FeedItem> unreadItems = DBReader
                            .getUnreadItemsList(context);

                    int undownloadedEpisodes = DBTasks.getNumberOfUndownloadedEpisodes(queue,
                            unreadItems);
                    int downloadedEpisodes = DBReader
                            .getNumberOfDownloadedEpisodes(context);
                    int deletedEpisodes = cleanupAlgorithm.performCleanup(context,
                            APCleanupAlgorithm.getPerformAutoCleanupArgs(context, undownloadedEpisodes));
                    int episodeSpaceLeft = undownloadedEpisodes;
                    boolean cacheIsUnlimited = UserPreferences.getEpisodeCacheSize() == UserPreferences
                            .getEpisodeCacheSizeUnlimited();

                    if (!cacheIsUnlimited
                            && UserPreferences.getEpisodeCacheSize() < downloadedEpisodes
                            + undownloadedEpisodes) {
                        episodeSpaceLeft = UserPreferences.getEpisodeCacheSize()
                                - (downloadedEpisodes - deletedEpisodes);
                    }

                    Arrays.sort(mediaIds);    // sort for binary search
                    final boolean ignoreMediaIds = mediaIds.length == 0;
                    List<FeedItem> itemsToDownload = new ArrayList<FeedItem>();

                    if (episodeSpaceLeft > 0 && undownloadedEpisodes > 0) {
                        for (int i = 0; i < queue.size(); i++) { // ignore playing item
                            FeedItem item = queue.get(i);
                            long mediaId = (item.hasMedia()) ? item.getMedia().getId() : -1;
                            if ((ignoreMediaIds || Arrays.binarySearch(mediaIds, mediaId) >= 0)
                                    && item.hasMedia()
                                    && !item.getMedia().isDownloaded()
                                    && !item.getMedia().isPlaying()
                                    && item.getFeed().getPreferences().getAutoDownload()) {
                                itemsToDownload.add(item);
                                episodeSpaceLeft--;
                                undownloadedEpisodes--;
                                if (episodeSpaceLeft == 0 || undownloadedEpisodes == 0) {
                                    break;
                                }
                            }
                        }
                    }

                    if (episodeSpaceLeft > 0 && undownloadedEpisodes > 0) {
                        for (FeedItem item : unreadItems) {
                            long mediaId = (item.hasMedia()) ? item.getMedia().getId() : -1;
                            if ((ignoreMediaIds || Arrays.binarySearch(mediaIds, mediaId) >= 0)
                                    && item.hasMedia()
                                    && !item.getMedia().isDownloaded()
                                    && item.getFeed().getPreferences().getAutoDownload()) {
                                itemsToDownload.add(item);
                                episodeSpaceLeft--;
                                undownloadedEpisodes--;
                                if (episodeSpaceLeft == 0 || undownloadedEpisodes == 0) {
                                    break;
                                }
                            }
                        }
                    }
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Enqueueing " + itemsToDownload.size()
                                + " items for download");

                    try {
                        DBTasks.downloadFeedItems(false, context,
                                itemsToDownload.toArray(new FeedItem[itemsToDownload
                                        .size()])
                        );
                    } catch (DownloadRequestException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
    }
}
