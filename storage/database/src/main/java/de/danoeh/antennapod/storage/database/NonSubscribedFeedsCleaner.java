package de.danoeh.antennapod.storage.database;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;

import java.util.List;

public class NonSubscribedFeedsCleaner {
    private static final String TAG = "NonSubscrFeedsCleaner";
    private static final long TIME_TO_KEEP = 1000L * 3600 * 24 * 30; // 30 days

    public static void deleteOldNonSubscribedFeeds(Context context) {
        List<Feed> feeds = DBReader.getFeedList();
        for (Feed feed : feeds) {
            if (feed.getState() != Feed.STATE_NOT_SUBSCRIBED) {
                continue;
            }
            DBReader.getFeedItemList(feed, FeedItemFilter.unfiltered(), SortOrder.DATE_NEW_OLD, 0, Integer.MAX_VALUE);
            if (shouldDelete(feed)) {
                Log.d(TAG, "Deleting unsubscribed feed " + feed.getTitle());
                DBWriter.deleteFeed(context, feed.getId());
            }
            feed.setItems(null); // Let it be garbage collected
        }
    }

    public static boolean shouldDelete(Feed feed) {
        if (feed.getState() != Feed.STATE_NOT_SUBSCRIBED) {
            return false;
        } else if (feed.getItems() == null) {
            return false;
        }
        for (FeedItem item : feed.getItems()) {
            if (item.isTagged(FeedItem.TAG_FAVORITE)
                    || item.isTagged(FeedItem.TAG_QUEUE)
                    || item.isDownloaded()) {
                return false;
            }
        }
        return feed.getLastRefreshAttempt() < System.currentTimeMillis() - TIME_TO_KEEP;
    }
}
