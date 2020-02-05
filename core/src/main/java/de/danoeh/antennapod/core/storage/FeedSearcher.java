package de.danoeh.antennapod.core.storage;

import android.content.Context;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedComponent;
import de.danoeh.antennapod.core.feed.FeedItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Performs search on Feeds and FeedItems.
 */
public class FeedSearcher {
    private FeedSearcher() {

    }

    /**
     * Search through a feed, or all feeds, for episodes that match the query in either the title,
     * chapter, or show notes. The search is first performed on titles, then chapters, and finally
     * show notes. The list of resulting episodes also describes where the first match occurred
     * (title, chapters, or show notes).
     *
     * @param context Used for database access
     * @param query search query
     * @param selectedFeed feed to search, 0 to search through all feeds
     * @return list of episodes containing the query
     */
    @NonNull
    public static List<FeedComponent> performSearch(final Context context, final String query, final long selectedFeed) {
        final List<FeedComponent> result = new ArrayList<>();
        try {
            FutureTask<List<FeedItem>> itemSearchTask = DBTasks.searchFeedItems(context, selectedFeed, query);
            FutureTask<List<Feed>> feedSearchTask = DBTasks.searchFeeds(context, query);
            itemSearchTask.run();
            feedSearchTask.run();

            result.addAll(feedSearchTask.get());
            result.addAll(itemSearchTask.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }
}
