package de.danoeh.antennapod.core.util;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBWriter;

/**
 * Provides method for sorting the queue according to rules.
 */
public class QueueSorter {
    public enum Rule {
        EPISODE_TITLE_ASC,
        EPISODE_TITLE_DESC,
        DATE_ASC,
        DATE_DESC,
        DURATION_ASC,
        DURATION_DESC,
        FEED_TITLE_ASC,
        FEED_TITLE_DESC,
        RANDOM,
        SMART_SHUFFLE_ASC,
        SMART_SHUFFLE_DESC
    }

    public static void sort(final Context context, final Rule rule, final boolean broadcastUpdate) {
        Comparator<FeedItem> comparator = null;
        Permutor<FeedItem> permutor = null;

        switch (rule) {
            case EPISODE_TITLE_ASC:
                comparator = (f1, f2) -> f1.getTitle().compareTo(f2.getTitle());
                break;
            case EPISODE_TITLE_DESC:
                comparator = (f1, f2) -> f2.getTitle().compareTo(f1.getTitle());
                break;
            case DATE_ASC:
                comparator = (f1, f2) -> f1.getPubDate().compareTo(f2.getPubDate());
                break;
            case DATE_DESC:
                comparator = (f1, f2) -> f2.getPubDate().compareTo(f1.getPubDate());
                break;
            case DURATION_ASC:
                comparator = (f1, f2) -> {
                    FeedMedia f1Media = f1.getMedia();
                    FeedMedia f2Media = f2.getMedia();
                    int duration1 = f1Media != null ? f1Media.getDuration() : -1;
                    int duration2 = f2Media != null ? f2Media.getDuration() : -1;

                    if (duration1 == -1 || duration2 == -1)
                        return duration2 - duration1;
                    else
                        return duration1 - duration2;
                };
                break;
            case DURATION_DESC:
                comparator = (f1, f2) -> {
                    FeedMedia f1Media = f1.getMedia();
                    FeedMedia f2Media = f2.getMedia();
                    int duration1 = f1Media != null ? f1Media.getDuration() : -1;
                    int duration2 = f2Media != null ? f2Media.getDuration() : -1;

                    return -1 * (duration1 - duration2);
                };
                break;
            case FEED_TITLE_ASC:
                comparator = (f1, f2) -> f1.getFeed().getTitle().compareTo(f2.getFeed().getTitle());
                break;
            case FEED_TITLE_DESC:
                comparator = (f1, f2) -> f2.getFeed().getTitle().compareTo(f1.getFeed().getTitle());
                break;
            case RANDOM:
                permutor = Collections::shuffle;
                break;
            case SMART_SHUFFLE_ASC:
                permutor = (queue) -> smartShuffle(queue, true);
                break;
            case SMART_SHUFFLE_DESC:
                permutor = (queue) -> smartShuffle(queue, false);
                break;
            default:
        }

        if (comparator != null) {
            DBWriter.sortQueue(comparator, broadcastUpdate);
        } else if (permutor != null) {
            DBWriter.reorderQueue(permutor, broadcastUpdate);
        }
    }

    /**
     * Implements a reordering by pubdate that avoids consecutive episodes from the same feed in
     * the queue.
     *
     * A listener might want to hear episodes from any given feed in pubdate order, but would
     * prefer a more balanced ordering that avoids having to listen to clusters of consecutive
     * episodes from the same feed. This is what "Smart Shuffle" tries to accomplish.
     *
     * The Smart Shuffle algorithm involves choosing episodes (in round-robin fashion) from a
     * collection of individual, pubdate-sorted lists that each contain only items from a specific
     * feed.
     *
     * Of course, clusters of consecutive episodes <i>at the end of the queue</i> may be
     * unavoidable. This seems unlikely to be an issue for most users who presumably maintain
     * large queues with new episodes continuously being added.
     *
     * For example, given a queue containing three episodes each from three different feeds
     * (A, B, and C), a simple pubdate sort might result in a queue that looks like the following:
     *
     * B1, B2, B3, A1, A2, C1, C2, C3, A3
     *
     * (note that feed B episodes were all published before the first feed A episode, so a simple
     * pubdate sort will often result in significant clustering of episodes from a single feed)
     *
     * Using Smart Shuffle, the resulting queue would look like the following:
     *
     * A1, B1, C1, A2, B2, C2, A3, B3, C3
     *
     * (note that episodes above <i>aren't strictly ordered in terms of pubdate</i>, but episodes
     * of each feed <b>do</b> appear in pubdate order)
     *
     * @param queue A (modifiable) list of FeedItem elements to be reordered.
     * @param ascending {@code true} to use ascending pubdate in the reordering;
     *                  {@code false} for descending.
     */
    private static void smartShuffle(List<FeedItem> queue, boolean ascending) {

        // Divide FeedItems into lists by feed

        Map<Long, List<FeedItem>> map = new HashMap<>();

        while (!queue.isEmpty()) {
            FeedItem item = queue.remove(0);
            Long id = item.getFeedId();
            if (!map.containsKey(id)) {
                map.put(id, new ArrayList<>());
            }
            map.get(id).add(item);
        }

        // Sort each individual list by PubDate (ascending/descending)

        Comparator<FeedItem> itemComparator = ascending
            ? (f1, f2) -> f1.getPubDate().compareTo(f2.getPubDate())
            : (f1, f2) -> f2.getPubDate().compareTo(f1.getPubDate());

        for (Long id : map.keySet()) {
            Collections.sort(map.get(id), itemComparator);
        }

        // Create a list of the individual FeedItems lists, and sort it by feed title (ascending).
        // Doing this ensures that the feed order we use is predictable/deterministic.

        List<List<FeedItem>> feeds = new ArrayList<>(map.values());
        Collections.sort(feeds,
            // (we use a desc sort here, since we're iterating back-to-front below)
            (f1, f2) -> f2.get(0).getFeed().getTitle().compareTo(f1.get(0).getFeed().getTitle()));

        // Cycle through the (sorted) feed lists in a round-robin fashion, removing the first item
        // and adding it back into to the original queue

        while (!feeds.isEmpty()) {
            // Iterate across the (sorted) list of feeds, removing the first item in each, and
            // appending it to the queue. Note that we're iterating back-to-front here, since we
            // will be deleting feed lists as they become empty.
            for (int i = feeds.size() - 1; i >= 0; --i) {
                List<FeedItem> items = feeds.get(i);
                queue.add(items.remove(0));
                // Removed the last item in this particular feed? Then remove this feed from the
                // list of feeds.
                if (items.isEmpty()) {
                    feeds.remove(i);
                }
            }
        }
    }
}
