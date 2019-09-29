package de.danoeh.antennapod.core.util;

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

    /**
     * Sorts the queue by the given sort order and sends a broadcast update.
     *
     * @param sortOrder Sort order.
     * @param broadcastUpdate Send broadcast update?
     */
    public static void sort(SortOrder sortOrder, boolean broadcastUpdate) {
        Permutor<FeedItem> permutor = getPermutor(sortOrder);
        if (permutor != null) {
            DBWriter.reorderQueue(permutor, broadcastUpdate);
        }
    }

    /**
     * Returns a Permutor that sorts a list appropriate to the given sort order.
     *
     * @param sortOrder Sort order.
     * @return Permutor that sorts a list appropriate to the given sort order. <code>null</code> if the order is unknown or <code>null</code>.
     */
    public static Permutor<FeedItem> getPermutor(SortOrder sortOrder) {
        if (sortOrder == null) {
            return null;
        }

        Comparator<FeedItem> comparator = null;
        Permutor<FeedItem> permutor = null;

        switch (sortOrder) {
            case EPISODE_TITLE_A_Z:
                comparator = (f1, f2) -> f1.getTitle().compareTo(f2.getTitle());
                break;
            case EPISODE_TITLE_Z_A:
                comparator = (f1, f2) -> f2.getTitle().compareTo(f1.getTitle());
                break;
            case DATE_OLD_NEW:
                comparator = (f1, f2) -> f1.getPubDate().compareTo(f2.getPubDate());
                break;
            case DATE_NEW_OLD:
                comparator = (f1, f2) -> f2.getPubDate().compareTo(f1.getPubDate());
                break;
            case DURATION_SHORT_LONG:
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
            case DURATION_LONG_SHORT:
                comparator = (f1, f2) -> {
                    FeedMedia f1Media = f1.getMedia();
                    FeedMedia f2Media = f2.getMedia();
                    int duration1 = f1Media != null ? f1Media.getDuration() : -1;
                    int duration2 = f2Media != null ? f2Media.getDuration() : -1;

                    return -1 * (duration1 - duration2);
                };
                break;
            case FEED_TITLE_A_Z:
                comparator = (f1, f2) -> f1.getFeed().getTitle().compareTo(f2.getFeed().getTitle());
                break;
            case FEED_TITLE_Z_A:
                comparator = (f1, f2) -> f2.getFeed().getTitle().compareTo(f1.getFeed().getTitle());
                break;
            case RANDOM:
                permutor = Collections::shuffle;
                break;
            case SMART_SHUFFLE_OLD_NEW:
                permutor = (queue) -> smartShuffle(queue, true);
                break;
            case SMART_SHUFFLE_NEW_OLD:
                permutor = (queue) -> smartShuffle(queue, false);
                break;
        }

        if (comparator != null) {
            final Comparator<FeedItem> comparator2 = comparator;
            permutor = (queue) -> Collections.sort(queue, comparator2);
        }
        return permutor;
    }

    /**
     * Implements a reordering by pubdate that avoids consecutive episodes from the same feed in
     * the queue.
     *
     * A listener might want to hear episodes from any given feed in pubdate order, but would
     * prefer a more balanced ordering that avoids having to listen to clusters of consecutive
     * episodes from the same feed. This is what "Smart Shuffle" tries to accomplish.
     *
     * The Smart Shuffle algorithm involves spreading episodes from each feed out over the whole
     * queue. To do this, we calculate the number of episodes in each feed, then a common multiple
     * (not the smallest); each episode is then spread out, and we sort the resulting list of
     * episodes by "spread out factor" and feed name.
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

        // Calculate the spread

        long spread = 0;
        for (Map.Entry<Long, List<FeedItem>> mapEntry : map.entrySet()) {
            List<FeedItem> feedItems = mapEntry.getValue();
            Collections.sort(feedItems, itemComparator);
            if (spread == 0) {
                spread = feedItems.size();
            } else if (spread % feedItems.size() != 0){
                spread *= feedItems.size();
            }
        }

        // Create a list of the individual FeedItems lists, and sort it by feed title (ascending).
        // Doing this ensures that the feed order we use is predictable/deterministic.

        List<List<FeedItem>> feeds = new ArrayList<>(map.values());
        Collections.sort(feeds,
                (f1, f2) -> f1.get(0).getFeed().getTitle().compareTo(f2.get(0).getFeed().getTitle()));

        // Spread each episode out
        Map<Long, List<FeedItem>> spreadItems = new HashMap<>();
        for (List<FeedItem> feedItems : feeds) {
            long thisSpread = spread / feedItems.size();
            if (thisSpread == 0) {
                thisSpread = 1;
            }
            // Starting from 0 ensures we front-load, so the queue starts with one episode from
            // each feed in the queue
            long itemSpread = 0;
            for (FeedItem feedItem : feedItems) {
                if (!spreadItems.containsKey(itemSpread)) {
                    spreadItems.put(itemSpread, new ArrayList<>());
                }
                spreadItems.get(itemSpread).add(feedItem);
                itemSpread += thisSpread;
            }
        }

        // Go through the spread items and add them to the queue
        List<Long> spreads = new ArrayList<>(spreadItems.keySet());
        Collections.sort(spreads);
        for (long itemSpread : spreads) {
            queue.addAll(spreadItems.get(itemSpread));
        }
    }
}
