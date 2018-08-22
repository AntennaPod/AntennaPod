package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static de.danoeh.antennapod.core.feed.FeedItem.TAG_FAVORITE;
import static de.danoeh.antennapod.core.storage.APKeepNNewestDownloadAlgorithm.feedItemComparator;

/**
 * A cleanup algorithm that keeps the N newest episodes for each feed.
 */
public class APKeepNNewestCleanupAlgorithm extends EpisodeCleanupAlgorithm {

    private static final String TAG = "APKeepNNewestCleanup";
    public static final int DEFAULT_KEEP_COUNT = 5;
    private final int keepCount;

    public APKeepNNewestCleanupAlgorithm() {
        this(DEFAULT_KEEP_COUNT);
    }

    public APKeepNNewestCleanupAlgorithm(final int keepCount) {
        this.keepCount = keepCount;
    }

    public int getKeepCount() {
        return keepCount;
    }

    @Override
    public int performCleanup(Context context, int numberOfEpisodesToDelete) {
        List<FeedItem> candidates = getCandidates();

        final int toDelete = Math.min(numberOfEpisodesToDelete, candidates.size());

        int deletedCount = 0;
        for (FeedItem item : candidates.subList(0, toDelete)) {
            try {
                deleteFeedMediaOfItem(context, item.getMedia().getId()).get();
                deletedCount += 1;
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }

        Log.i(TAG, String.format(
                "Auto-delete deleted %d episodes (%d requested)", deletedCount,
                numberOfEpisodesToDelete));

        return deletedCount;
    }

    @Override
    public int getDefaultCleanupParameter() {
        return getReclaimableItems();
    }

    @Override
    public int getReclaimableItems() {
        return getCandidates().size();
    }

    @NonNull
    private List<FeedItem> getCandidates() {
        List<FeedItem> candidates = new ArrayList<>();

        for (Feed feed : getFeedList()) {
            List<FeedItem> items = getFeedItemList(feed);
            Collections.sort(items, feedItemComparator);

            for (FeedItem item: items.subList(Math.min(keepCount, items.size()), items.size())) {
                if (item.isTagged((TAG_FAVORITE))
                        || item.getMedia() == null
                        || !item.getMedia().isDownloaded()
                        || item.getMedia().isPlaying()) {
                    continue;
                }
                candidates.add(item);
            }
        }

        return candidates;
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
     *         A context that is used for opening a database
     *         connection.
     * @param mediaId
     *         ID of the FeedMedia object whose downloaded file
     *         should be deleted.
     */
    protected Future<?> deleteFeedMediaOfItem(final Context context,
                                              final long mediaId) {
        return DBWriter.deleteFeedMediaOfItem(context, mediaId);
    }
}
