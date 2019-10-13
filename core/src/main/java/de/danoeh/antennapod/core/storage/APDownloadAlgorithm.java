package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

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

    // Subset of DBReader static methods, for ease of stubbing in tests
    interface ItemProvider {
        int getNumberOfDownloadedEpisodes();
        @NonNull List<? extends FeedItem> getQueue();
        @NonNull List<? extends FeedItem> getNewItemsList();
    }

    // Subset of UserPreferences static methods, for ease of stubbing in tests
    interface DownloadPreferences {
        int getEpisodeCacheSize();
        boolean isCacheUnlimited();
    }

    @NonNull
    private final ItemProvider itemProvider;
    @NonNull
    private final EpisodeCleanupAlgorithm cleanupAlgorithm;
    @NonNull
    private final DownloadPreferences downloadPreferences;

    @VisibleForTesting
    APDownloadAlgorithm(@NonNull ItemProvider itemProvider,
                        @NonNull EpisodeCleanupAlgorithm cleanupAlgorithm,
                        @NonNull DownloadPreferences downloadPreferences) {
        this.itemProvider = itemProvider;
        this.cleanupAlgorithm = cleanupAlgorithm;
        this.downloadPreferences = downloadPreferences;
    }

    public APDownloadAlgorithm() {
        this.itemProvider = new ItemProviderDefaultImpl();
        this.cleanupAlgorithm = UserPreferences.getEpisodeCleanupAlgorithm();
        this.downloadPreferences = new DownloadPreferencesDefaultImpl();
    }

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
    @Override
    public Runnable autoDownloadUndownloadedItems(final Context context) {
        return () -> {

            // true if we should auto download based on network status
            boolean networkShouldAutoDl = NetworkUtils.autodownloadNetworkAvailable()
                    && UserPreferences.isEnableAutodownload();

            // true if we should auto download based on power status
            boolean powerShouldAutoDl = PowerUtils.deviceCharging(context)
                    || UserPreferences.isEnableAutodownloadOnBattery();

            // we should only auto download if both network AND power are happy
            if (networkShouldAutoDl && powerShouldAutoDl) {

                Log.d(TAG, "Performing auto-dl of undownloaded episodes");

                List<? extends FeedItem> itemsToDownloadList = getItemsToDownload(context);

                FeedItem[] itemsToDownload = itemsToDownloadList
                        .toArray(new FeedItem[itemsToDownloadList.size()]);

                Log.d(TAG, "Enqueueing " + itemsToDownload.length + " items for download");

                try {
                    DBTasks.downloadFeedItems(false, context, itemsToDownload);
                } catch (DownloadRequestException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    @VisibleForTesting
    @NonNull
    List<? extends FeedItem> getItemsToDownload(@NonNull Context context) {
        DownloadItemSelector selector = new DownloadItemSelectorEpisodicImpl(itemProvider);

        List<? extends FeedItem> candidates =
                selector.getAutoDownloadableEpisodes();

        int autoDownloadableEpisodes = candidates.size();
        int downloadedEpisodes = itemProvider.getNumberOfDownloadedEpisodes();
        int deletedEpisodes = cleanupAlgorithm.makeRoomForEpisodes(context, autoDownloadableEpisodes);
        boolean cacheIsUnlimited = downloadPreferences.isCacheUnlimited();
        int episodeCacheSize = downloadPreferences.getEpisodeCacheSize();

        int episodeSpaceLeft;
        if (cacheIsUnlimited ||
                episodeCacheSize >= downloadedEpisodes + autoDownloadableEpisodes) {
            episodeSpaceLeft = autoDownloadableEpisodes;
        } else {
            episodeSpaceLeft = episodeCacheSize - (downloadedEpisodes - deletedEpisodes);
        }
        return candidates.subList(0, episodeSpaceLeft);
    }

    @VisibleForTesting
    public static class ItemProviderDefaultImpl implements ItemProvider {
        @Override
        public int getNumberOfDownloadedEpisodes() {
            return DBReader.getNumberOfDownloadedEpisodes();
        }

        @NonNull
        @Override
        public List<? extends FeedItem> getQueue() {
            return DBReader.getQueue();
        }

        @NonNull
        @Override
        public List<? extends FeedItem> getNewItemsList() {
            return DBReader.getNewItemsList();
        }
    }

    private static class DownloadPreferencesDefaultImpl implements DownloadPreferences {
        @Override
        public int getEpisodeCacheSize() {
            return UserPreferences.getEpisodeCacheSize();
        }

        @Override
        public boolean isCacheUnlimited() {
            return UserPreferences.getEpisodeCacheSize() == UserPreferences
                    .getEpisodeCacheSizeUnlimited();
        }
    }
}
