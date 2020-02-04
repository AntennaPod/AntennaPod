package de.danoeh.antennapod.core.storage;

import android.content.Context;
import androidx.annotation.NonNull;

import de.danoeh.antennapod.core.feed.Chapter;
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
import de.danoeh.antennapod.core.util.comparator.InReverseChronologicalOrder;

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
    public static List<SearchResult> performSearch(final Context context, final String query, final long selectedFeed) {
        final List<SearchResult> result = new ArrayList<>();
        try {
            FutureTask<List<FeedItem>> searchTask = DBTasks.searchFeedItems(context, selectedFeed, query);
            searchTask.run();
            final List<FeedItem> items = searchTask.get();
            for (FeedItem item : items) {
                SearchLocation location;
                if (safeContains(item.getTitle(), query)) {
                    location = SearchLocation.TITLE;
                } else if (safeContains(item.getChapters(), query)) {
                    location = SearchLocation.CHAPTERS;
                } else {
                    location = SearchLocation.SHOWNOTES;
                }
                result.add(new SearchResult(item, location));
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static boolean safeContains(String haystack, String needle) {
        return haystack != null && haystack.contains(needle);
    }

    private static boolean safeContains(List<Chapter> haystack, String needle) {
        if (haystack == null) {
            return false;
        }
        for (Chapter chapter : haystack) {
            if (safeContains(chapter.getTitle(), needle)) {
                return true;
            }
        }
        return false;
    }
}
