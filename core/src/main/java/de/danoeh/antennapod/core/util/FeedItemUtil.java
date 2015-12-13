package de.danoeh.antennapod.core.util;

import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;

public class FeedItemUtil {

    public static int indexOfItemWithDownloadUrl(List<FeedItem> items, String downloadUrl) {
        if(items == null) {
            return -1;
        }
        for(int i=0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            if(item.hasMedia() && item.getMedia().getDownload_url().equals(downloadUrl)) {
                return i;
            }
        }
        return -1;
    }

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

    public static long[] getIds(FeedItem... items) {
        if(items == null || items.length == 0) {
            return new long[0];
        }
        long[] result = new long[items.length];
        for(int i=0; i < items.length; i++) {
            result[i] = items[i].getId();
        }
        return result;
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

    public static boolean containsAnyId(List<FeedItem> items, long[] ids) {
        if(items == null || items.size() == 0) {
            return false;
        }
        for(FeedItem item : items) {
            for(long id : ids) {
                if(item.getId() == id) {
                    return true;
                }
            }
        }
        return false;
    }

}
