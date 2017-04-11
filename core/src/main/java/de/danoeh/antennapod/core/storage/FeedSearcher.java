package de.danoeh.antennapod.core.storage;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.SearchResult;
import de.danoeh.antennapod.core.util.comparator.SearchResultValueComparator;

/**
 * Performs search on Feeds and FeedItems
 */
public class FeedSearcher {
    private static final String TAG = "FeedSearcher";


    /**
     * Search through a feed, or all feeds, for episodes that match the query in either the title,
     * chapter, or show notes. The search is first performed on titles, then chapters, and finally
     * show notes. The list of resulting episodes also describes where the first match occurred
     * (title, chapters, or show notes).
     *
     * @param context
     * @param query search query
     * @param selectedFeed feed to search, 0 to search through all feeds
     * @return list of episodes containing the query
     */
    public static List<SearchResult> performSearch(final Context context,
                                                   final String query, final long selectedFeed) {
        final int values[] = {2, 1, 0, 0, 0, 0};
        final String[] subtitles = {context.getString(R.string.found_in_title_label),
                context.getString(R.string.found_in_chapters_label),
                context.getString(R.string.found_in_shownotes_label),
                context.getString(R.string.found_in_shownotes_label),
                context.getString(R.string.found_in_authors_label),
                context.getString(R.string.found_in_feeds_label)};

        List<SearchResult> result = new ArrayList<>();

        List<FutureTask<List<FeedItem>>> tasks = new ArrayList<>();
        tasks.add(DBTasks.searchFeedItemTitle(context, selectedFeed, query));
        tasks.add(DBTasks.searchFeedItemChapters(context, selectedFeed, query));
        tasks.add(DBTasks.searchFeedItemDescription(context, selectedFeed, query));
        tasks.add(DBTasks.searchFeedItemContentEncoded(context, selectedFeed, query));
        tasks.add(DBTasks.searchFeedItemAuthor(context, selectedFeed, query));
        tasks.add(DBTasks.searchFeedItemFeedIdentifier(context, selectedFeed, query));

        for (FutureTask<List<FeedItem>> task : tasks) {
            task.run();
        }
        try {
            Set<Long> set = new HashSet<>();

            for (int i = 0; i < tasks.size(); i++) {
                FutureTask<List<FeedItem>> task = tasks.get(i);
                List<FeedItem> items = task.get();
                for (FeedItem item : items) {
                    if (!set.contains(item.getId())) { // to prevent duplicate results
                        result.add(new SearchResult(item, values[i], subtitles[i]));
                        set.add(item.getId());
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        Collections.sort(result, new SearchResultValueComparator());
        return result;
    }
}
