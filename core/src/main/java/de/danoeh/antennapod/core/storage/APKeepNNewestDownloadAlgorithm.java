package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.PowerUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static de.danoeh.antennapod.core.storage.APKeepNNewestCleanupAlgorithm.DEFAULT_KEEP_COUNT;

public class APKeepNNewestDownloadAlgorithm implements AutomaticDownloadAlgorithm {
    private static final String TAG = "APKeepNNewestDownload";

    private final int keepCount;

    public APKeepNNewestDownloadAlgorithm() {
        this(DEFAULT_KEEP_COUNT);
    }

    public APKeepNNewestDownloadAlgorithm(final int keepCount) {
        this.keepCount = keepCount;
    }

    public int getKeepCount() {
        return keepCount;
    }

    @Override
    public Runnable autoDownloadUndownloadedItems(Context context) {
        return () -> {
            try {

                if (!isAutoDownloadEnabled() || !isAutoDownloadNetworkAvailable() || !isAutoDownloadPowerAvailable(context)) {
                    return;
                }

                List<FeedItem> downloadCandidates = new ArrayList<>();

                for (Feed feed : getFeedList()) {
                    List<FeedItem> items = getFeedItemList(feed);
                    Collections.sort(items, feedItemComparator);

                    // Consider only the newest N items for auto download
                    for (FeedItem item : items.subList(0, Math.min(items.size(), keepCount))) {
                        if (!item.isAutoDownloadable()
                                || item.getMedia() == null
                                || item.getMedia().isDownloaded()
                                || item.isPlayed()) {
                            continue;
                        }
                        downloadCandidates.add(item);
                    }
                }

                makeRoomForEpisodes(context, downloadCandidates.size());

                final int availableSpace;
                if (isEpisodeCacheUnlimited()) {
                    availableSpace = Integer.MAX_VALUE;
                } else {
                    availableSpace = Math.max(0, getEpisodeCacheSize() - getNumberOfDownloadedEpisodes());
                }

                Collections.sort(downloadCandidates, feedItemComparator);

                List<FeedItem> itemsToDownload = downloadCandidates.subList(0, Math.min(downloadCandidates.size(), availableSpace));

                Log.d(TAG, String.format("Performing auto-dl of %d (of %d) undownloaded episodes",
                        itemsToDownload.size(), downloadCandidates.size()));

                if (!itemsToDownload.isEmpty()) {
                    downloadFeedItems(context, itemsToDownload);
                }

            } catch (DownloadRequestException e) {
                e.printStackTrace();
            }
        };
    }

    /**
     * Wraps static method call to allow mocking in tests.
     *
     * @return A list of Feeds, sorted alphabetically by their
     * title. A Feed-object of the returned list does NOT have its
     * list of FeedItems yet. The FeedItem-list can be loaded
     * separately with {@link #getFeedItemList(Feed)}.
     */
    protected List<Feed> getFeedList() {
        return DBReader.getFeedList();
    }

    /**
     * Wraps static method call to allow mocking in tests.
     *
     * @param feed
     *         The Feed whose items should be loaded
     * @return A list with the FeedItems of the Feed. The
     * Feed-attribute of the FeedItems will already be set correctly.
     */
    protected List<FeedItem> getFeedItemList(final Feed feed) {
        return DBReader.getFeedItemList(feed);
    }

    /**
     * Wraps static method call to allow mocking in tests.
     *
     * @param context
     *         Android context to work within
     * @param feedItems
     *         Items to download
     * @throws DownloadRequestException
     *         on download errors
     */
    protected void downloadFeedItems(final Context context, final List<FeedItem> feedItems) throws DownloadRequestException {
        FeedItem[] array = new FeedItem[feedItems.size()];
        feedItems.toArray(array);

        DBTasks.downloadFeedItems(false, context, array);
    }

    /**
     * Wraps static method call to allow mocking in tests.
     *
     * @return true if user has enabled automatic downloads, false otherwise.
     */
    protected boolean isAutoDownloadEnabled() {
        return UserPreferences.isEnableAutodownload();
    }

    /**
     * Wraps static method call to allow mocking in tests.
     *
     * @return true if network conditions allow for automatic downloads, false otherwise.
     */
    protected boolean isAutoDownloadNetworkAvailable() {
        return NetworkUtils.autodownloadNetworkAvailable();
    }

    /**
     * Wraps static method call to allow mocking in tests.
     *
     * @return true if plugged in or downloads while on battery is allowed, false otherwise.
     */
    protected boolean isAutoDownloadPowerAvailable(final Context context) {
        return PowerUtils.deviceCharging(context)
                || UserPreferences.isEnableAutodownloadOnBattery();
    }

    /**
     * Wraps static method call to allow mocking in tests.
     */
    protected int getEpisodeCacheSize() {
        return UserPreferences.getEpisodeCacheSize();
    }

    /**
     * Wraps static method call to allow mocking in tests.
     */
    protected boolean isEpisodeCacheUnlimited() {
        return getEpisodeCacheSize() == UserPreferences.getEpisodeCacheSizeUnlimited();
    }

    /**
     * Wraps static method call to allow mocking in tests.
     */
    protected int getNumberOfDownloadedEpisodes() {
        return DBReader.getNumberOfDownloadedEpisodes();
    }


    /**
     * Wraps static method call to allow mocking in tests.
     *
     * @return number of deleted episodes
     */
    protected int makeRoomForEpisodes(Context context, int count) {
        return UserPreferences.getEpisodeCleanupAlgorithm()
                              .makeRoomForEpisodes(context, count);
    }

    public static final Comparator<FeedItem> feedItemComparator = (lhs, rhs) -> {
        Date l = lhs.getPubDate();
        Date r = rhs.getPubDate();

        if (l == null && r == null) {
            return 0;
        } else if (l == null) {
            return 1;
        } else if (r == null) {
            return -1;
        } else {
            return r.compareTo(l);
        }
    };
}
