package de.danoeh.antennapod.core.util.comparator;

import java.util.Comparator;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.SearchResult;

public class SearchResultDateComparator implements Comparator<SearchResult> {
    /**
     * Compare items and sort it on chronological order.
     */
    @Override
    public int compare(SearchResult o1, SearchResult o2) {
        if(o1.getComponent() instanceof FeedItem && o2.getComponent() instanceof FeedItem){
            return ((FeedItem) o2.getComponent()).getPubDate().compareTo(((FeedItem) o1.getComponent()).getPubDate());
        }
        return 0;
    }
}
