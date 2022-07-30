package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadRequestCreator;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.PowerUtils;

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
            boolean networkShouldAutoDl = NetworkUtils.isAutoDownloadAllowed()
                    && UserPreferences.isEnableAutodownload();

            // true if we should auto download based on power status
            boolean powerShouldAutoDl = PowerUtils.deviceCharging(context)
                    || UserPreferences.isEnableAutodownloadOnBattery();

            // we should only auto download if both network AND power are happy
            if (networkShouldAutoDl && powerShouldAutoDl) {

                Log.d(TAG, "Performing auto-dl of undownloaded episodes");

                List<FeedItem> candidates;
                final List<FeedItem> queue = DBReader.getQueue();
                final List<FeedItem> newItems = DBReader.getNewItemsList(0, Integer.MAX_VALUE);
                candidates = new ArrayList<>(queue.size() + newItems.size());
                candidates.addAll(queue);
                for (FeedItem newItem : newItems) {
                    FeedPreferences feedPrefs = newItem.getFeed().getPreferences();
                    if (feedPrefs.getAutoDownload()
                            && !candidates.contains(newItem)
                            && feedPrefs.getFilter().shouldAutoDownload(newItem)) {
                        candidates.add(newItem);
                    }
                }

                // filter items that are not auto downloadable
                Iterator<FeedItem> it = candidates.iterator();
                while (it.hasNext()) {
                    FeedItem item = it.next();
                    if (!item.isAutoDownloadable(System.currentTimeMillis()) || FeedItemUtil.isPlaying(item.getMedia())
                            || item.getFeed().isLocalFeed()) {
                        it.remove();
                    }
                }

                int autoDownloadableEpisodes = candidates.size();
                int downloadedEpisodes = DBReader.getNumberOfDownloadedEpisodes();
                int deletedEpisodes = EpisodeCleanupAlgorithmFactory.build()
                        .makeRoomForEpisodes(context, autoDownloadableEpisodes);
                boolean cacheIsUnlimited =
                        UserPreferences.getEpisodeCacheSize() == UserPreferences.getEpisodeCacheSizeUnlimited();
                int episodeCacheSize = UserPreferences.getEpisodeCacheSize();

                int episodeSpaceLeft;
                if (cacheIsUnlimited || episodeCacheSize >= downloadedEpisodes + autoDownloadableEpisodes) {
                    episodeSpaceLeft = autoDownloadableEpisodes;
                } else {
                    episodeSpaceLeft = episodeCacheSize - (downloadedEpisodes - deletedEpisodes);
                }

                List<FeedItem> itemsToDownload = candidates.subList(0, episodeSpaceLeft);
                if (itemsToDownload.size() > 0) {
                    Log.d(TAG, "Enqueueing " + itemsToDownload.size() + " items for download");

                    List<DownloadRequest> requests = new ArrayList<>();
                    for (FeedItem episode : itemsToDownload) {
                        DownloadRequest.Builder request = DownloadRequestCreator.create(episode.getMedia());
                        request.withInitiatedByUser(false);
                        requests.add(request.build());
                    }
                    DownloadService.download(context, false, requests.toArray(new DownloadRequest[0]));
                }
            }
        };
    }
}
