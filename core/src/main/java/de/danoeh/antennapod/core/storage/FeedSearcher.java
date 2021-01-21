package de.danoeh.antennapod.core.storage;

import android.content.Context;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Performs search on Feeds and FeedItems.
 */
public class FeedSearcher {
    private FeedSearcher() {

    }

    @NonNull
    public static List<FeedItem> searchFeedItems(final Context context, final String query, final long selectedFeed) {
        try {
            FutureTask<List<FeedItem>> itemSearchTask = DBTasks.searchFeedItems(context, selectedFeed, query);
            itemSearchTask.run();
            return itemSearchTask.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @NonNull
    public static List<Feed> searchFeeds(final Context context, final String query) {
        try {
            FutureTask<List<Feed>> feedSearchTask = DBTasks.searchFeeds(context, query);
            feedSearchTask.run();
            return feedSearchTask.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
