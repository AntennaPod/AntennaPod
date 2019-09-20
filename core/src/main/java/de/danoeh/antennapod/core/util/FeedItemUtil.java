package de.danoeh.antennapod.core.util;

import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;

public class FeedItemUtil {
    private FeedItemUtil(){}

    public static int indexOfItemWithId(List<FeedItem> items, long id) {
        for(int i=0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            if(item != null && item.getId() == id) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOfItemWithMediaId(List<FeedItem> items, long mediaId) {
        for(int i=0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            if(item != null && item.getMedia() != null && item.getMedia().getId() == mediaId) {
                return i;
            }
        }
        return -1;
    }

    public static long[] getIds(List<FeedItem> items) {
        if(items == null || items.size() == 0) {
            return new long[0];
        }
        long[] result = new long[items.size()];
        for(int i=0; i < items.size(); i++) {
            result[i] = items.get(i).getId();
        }
        return result;
    }

    /**
     * Get the link for the feed item for the purpose of Share. It fallbacks to
     * use the feed's link if the named feed item has no link.
     */
    public static String getLinkWithFallback(FeedItem item) {
        if (item == null) {
            return null;
        } else if (item.getLink() != null) {
            return item.getLink();
        } else if (item.getFeed() != null) {
            return item.getFeed().getLink();
        }
        return null;
    }
}
