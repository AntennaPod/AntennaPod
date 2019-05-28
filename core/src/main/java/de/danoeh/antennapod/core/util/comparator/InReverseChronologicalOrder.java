package de.danoeh.antennapod.core.util.comparator;

import java.util.Comparator;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.SearchResult;

public class InReverseChronologicalOrder implements Comparator<SearchResult> {
    /**
     * Compare items and sort it on chronological order.
     */
    @Override
    public int compare(SearchResult o1, SearchResult o2) {
        if ((o1.getComponent() instanceof FeedItem) && (o2.getComponent() instanceof FeedItem)) {
            FeedItem item1 = (FeedItem) o1.getComponent();
            FeedItem item2 = (FeedItem) o2.getComponent();
            return item2.getPubDate().compareTo(item1.getPubDate());
        }
        return 0;
    }
}
