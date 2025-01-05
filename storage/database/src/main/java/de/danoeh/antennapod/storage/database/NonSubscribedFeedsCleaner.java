package de.danoeh.antennapod.storage.database;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class NonSubscribedFeedsCleaner {
    private static final String TAG = "NonSubscrFeedsCleaner";
    private static final long TIME_TO_KEEP_UNTOUCHED = 1000L * 3600 * 24; // 1 day
    private static final long TIME_TO_KEEP_PLAYED = 1000L * 3600 * 24 * 30; // 30 days

    public static void deleteOldNonSubscribedFeeds(Context context) {
        List<Feed> feeds = DBReader.getFeedList();
        for (Feed feed : feeds) {
            if (feed.getState() != Feed.STATE_NOT_SUBSCRIBED) {
                continue;
            }
            DBReader.getFeedItemList(feed, new FeedItemFilter(FeedItemFilter.INCLUDE_NOT_SUBSCRIBED),
                    SortOrder.DATE_NEW_OLD, 0, Integer.MAX_VALUE);
            DBReader.loadAdditionalFeedItemListData(feed.getItems());
            if (shouldDelete(feed)) {
                Log.d(TAG, "Deleting unsubscribed feed " + feed.getTitle());
                try {
                    DBWriter.deleteFeed(context, feed.getId()).get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
            feed.setItems(null); // Let it be garbage collected
        }
    }

    public static boolean shouldDelete(Feed feed) {
        if (feed.getState() != Feed.STATE_NOT_SUBSCRIBED) {
            return false;
        } else if (feed.getItems() == null) {
            return false;
        } else if (feed.hasEpisodeInApp()) {
            return false;
        }
        long timeSinceLastRefresh = System.currentTimeMillis() - feed.getLastRefreshAttempt();
        if (!feed.hasInteractedWithEpisode()) {
            return timeSinceLastRefresh > TIME_TO_KEEP_UNTOUCHED;
        }
        return timeSinceLastRefresh > TIME_TO_KEEP_PLAYED;
    }
}
