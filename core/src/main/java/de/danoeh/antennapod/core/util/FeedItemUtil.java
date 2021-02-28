package de.danoeh.antennapod.core.util;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedItemFilter;
import de.danoeh.antennapod.core.storage.DBReader;

import static de.danoeh.antennapod.core.feed.FeedItem.TAG_FAVORITE;

public class FeedItemUtil {

    private FeedItemUtil() {
    }

    public static int indexOfItemWithId(List<FeedItem> items, long id) {
        for (int i = 0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            if (item != null && item.getId() == id) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOfItemWithMediaId(List<FeedItem> items, long mediaId) {
        for (int i = 0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            if (item != null && item.getMedia() != null && item.getMedia().getId() == mediaId) {
                return i;
            }
        }
        return -1;
    }

    public static long[] getIds(List<FeedItem> items) {
        if (items == null || items.size() == 0) {
            return new long[0];
        }
        long[] result = new long[items.size()];
        for (int i = 0; i < items.size(); i++) {
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


    /**
     * Run a list of feed items through a {@link FeedItemFilter}.
     */
    public static List<FeedItem> filter(List<FeedItem> items, FeedItemFilter feedItemFilter) {
        if (feedItemFilter.getValues().length == 0) {
            return items;
        }

        // Check for filter combinations that will always return an empty list
        // (e.g. requiring played and unplayed at the same time)
        if (feedItemFilter.showPlayed && feedItemFilter.showUnplayed) {
            return Collections.emptyList();
        }
        if (feedItemFilter.showQueued && feedItemFilter.showNotQueued) {
            return Collections.emptyList();
        }
        if (feedItemFilter.showDownloaded && feedItemFilter.showNotDownloaded) {
            return Collections.emptyList();
        }

        List<FeedItem> result = new ArrayList<>();
        final LongList queuedIds = DBReader.getQueueIDList();
        for (FeedItem item : items) {
            // If the item does not meet a requirement, skip it.

            if (feedItemFilter.showPlayed && !item.isPlayed()) {
                continue;
            }
            if (feedItemFilter.showUnplayed && item.isPlayed()) {
                continue;
            }

            if (feedItemFilter.showPaused && item.getState() != FeedItem.State.IN_PROGRESS) {
                continue;
            }
            if (feedItemFilter.showNotPaused && item.getState() == FeedItem.State.IN_PROGRESS) {
                continue;
            }

            boolean queued = queuedIds.contains(item.getId());
            if (feedItemFilter.showQueued && !queued) {
                continue;
            }
            if (feedItemFilter.showNotQueued && queued) {
                continue;
            }

            boolean downloaded = item.getMedia() != null && item.getMedia().isDownloaded();
            if (feedItemFilter.showDownloaded && !downloaded) {
                continue;
            }
            if (feedItemFilter.showNotDownloaded && downloaded) {
                continue;
            }

            if (feedItemFilter.showHasMedia && !item.hasMedia()) {
                continue;
            }
            if (feedItemFilter.showNoMedia && item.hasMedia()) {
                continue;
            }

            if (feedItemFilter.showIsFavorite && !item.isTagged(TAG_FAVORITE)) {
                continue;
            }
            if (feedItemFilter.showNotFavorite && item.isTagged(TAG_FAVORITE)) {
                continue;
            }

            // If the item reaches here, it meets all criteria
            result.add(item);
        }
        return result;
    }
}
