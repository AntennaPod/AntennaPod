package de.danoeh.antennapod.core.storage;

import android.content.Context;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.SearchResult;
import de.danoeh.antennapod.core.util.comparator.SearchResultValueComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Performs search on Feeds and FeedItems
 */
public class FeedSearcher {
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

        List<SearchResult> result = new ArrayList<SearchResult>();

        FutureTask<List<FeedItem>>[] tasks = new FutureTask[4];
        (tasks[0] = DBTasks.searchFeedItemContentEncoded(context, selectedFeed, query)).run();
        (tasks[1] = DBTasks.searchFeedItemDescription(context, selectedFeed, query)).run();
        (tasks[2] = DBTasks.searchFeedItemChapters(context, selectedFeed, query)).run();
        (tasks[3] = DBTasks.searchFeedItemTitle(context, selectedFeed, query)).run();
        try {
            for (int i = 0; i < tasks.length; i++) {
                FutureTask task = tasks[i];
                List<FeedItem> items = (List<FeedItem>) task.get();
                for (FeedItem item : items) {
                    result.add(new SearchResult(item, values[i], subtitles[i]));
                }

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        Collections.sort(result, new SearchResultValueComparator());
        return result;
    }
}
