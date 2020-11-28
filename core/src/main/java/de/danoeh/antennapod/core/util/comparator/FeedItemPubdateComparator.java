package de.danoeh.antennapod.core.util.comparator;

import java.util.Comparator;

import de.danoeh.antennapod.core.feed.FeedItem;

/**
 * Compares the pubDate of two FeedItems for sorting.
 */
public class FeedItemPubdateComparator implements Comparator<FeedItem> {

    /**
     * Returns a new instance of this comparator in reverse order.
     */
    @Override
    public int compare(FeedItem lhs, FeedItem rhs) {
        if (rhs.getPubDate() == null && lhs.getPubDate() == null) {
            return 0;
        } else if (rhs.getPubDate() == null) {
            return 1;
        } else if (lhs.getPubDate() == null) {
            return -1;
        }
        return rhs.getPubDate().compareTo(lhs.getPubDate());
    }

}
