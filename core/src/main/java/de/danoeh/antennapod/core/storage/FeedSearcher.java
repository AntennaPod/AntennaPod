package de.danoeh.antennapod.core.storage;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private FeedSearcher(){}

    private static final String TAG = "FeedSearcher";


    /**
     * Performs a search in all feeds or one specific feed.
     */
    public static List<SearchResult> performSearch(final Context context,
                                                   final String query, final long selectedFeed) {
        final int values[] = {0, 0, 1, 2};
        final String[] subtitles = {context.getString(R.string.found_in_shownotes_label),
                context.getString(R.string.found_in_shownotes_label),
                context.getString(R.string.found_in_chapters_label),
                context.getString(R.string.found_in_title_label)};

        List<SearchResult> result = new ArrayList<>();

        List<FutureTask<List<FeedItem>>> tasks = new ArrayList<>();
        tasks.add(DBTasks.searchFeedItemContentEncoded(context, selectedFeed, query));
        tasks.add(DBTasks.searchFeedItemDescription(context, selectedFeed, query));
        tasks.add(DBTasks.searchFeedItemChapters(context, selectedFeed, query));
        tasks.add(DBTasks.searchFeedItemTitle(context, selectedFeed, query));
        for (FutureTask<List<FeedItem>> task : tasks) {
            task.run();
        }
        try {
            for (int i = 0; i < tasks.size(); i++) {
                FutureTask<List<FeedItem>> task = tasks.get(i);
                List<FeedItem> items = task.get();
                for (FeedItem item : items) {
                    result.add(new SearchResult(item, values[i], subtitles[i]));
                }

            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        Collections.sort(result, new SearchResultValueComparator());
        return result;
    }
}
