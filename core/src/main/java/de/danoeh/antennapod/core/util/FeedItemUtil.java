package de.danoeh.antennapod.core.util;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.model.feed.FeedItem;

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

    public static int indexOfItemWithDownloadUrl(List<FeedItem> items, String downloadUrl) {
        for (int i = 0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            if (item != null && item.getMedia() != null && item.getMedia().getDownload_url().equals(downloadUrl)) {
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

    @NonNull
    public static List<Long> getIdList(List<? extends FeedItem> items) {
        List<Long> result = new ArrayList<>();
        for (FeedItem item : items) {
            result.add(item.getId());
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
        } else if (StringUtils.isNotBlank(item.getLink())) {
            return item.getLink();
        } else if (StringUtils.isNotBlank(item.getFeed().getLink())) {
            return item.getFeed().getLink();
        }
        return null;
    }

    public static boolean hasAlmostEnded(FeedMedia media) {
        int smartMarkAsPlayedSecs = UserPreferences.getSmartMarkAsPlayedSecs();
        return media.getDuration() > 0 && media.getPosition() >= media.getDuration() - smartMarkAsPlayedSecs * 1000;
    }
}
