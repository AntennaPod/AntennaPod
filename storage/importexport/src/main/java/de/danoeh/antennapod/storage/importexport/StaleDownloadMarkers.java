package de.danoeh.antennapod.storage.importexport;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for detecting and clearing stale download markers: episodes the database thinks are
 * downloaded but whose files are missing from disk. Typically used after a database import.
 */
public class StaleDownloadMarkers {

    private StaleDownloadMarkers() {
    }

    public static List<Long> findMissingMediaIds() {
        List<FeedItem> downloadedItems = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                new FeedItemFilter(FeedItemFilter.DOWNLOADED), SortOrder.DATE_NEW_OLD);
        List<Long> missingIds = new ArrayList<>();
        for (FeedItem item : downloadedItems) {
            if (item.getMedia() != null && !item.getMedia().fileExists()) {
                missingIds.add(item.getMedia().getId());
            }
        }
        return missingIds;
    }

    /**
     * Reconcile download markers for episodes whose feed is not subscribed (i.e. removed or
     * archived) and whose file is missing on disk. We don't want to re-download  these,
     * so we silently clear the marker. We leave items alone if the file still exists.
     */
    public static void clearStaleDownloadsFromUnsubscribedFeeds() {
        List<FeedItem> downloadedItems = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                new FeedItemFilter(FeedItemFilter.DOWNLOADED, FeedItemFilter.INCLUDE_NOT_SUBSCRIBED),
                SortOrder.DATE_NEW_OLD);
        List<Long> missingIds = new ArrayList<>();
        for (FeedItem item : downloadedItems) {
            if (item.getFeed() == null || item.getFeed().getState() == Feed.STATE_SUBSCRIBED) {
                continue;
            }
            if (item.getMedia() != null && !item.getMedia().fileExists()) {
                missingIds.add(item.getMedia().getId());
            }
        }
        if (!missingIds.isEmpty()) {
            clearDownloadStateForItems(missingIds);
        }
    }

    public static void clearDownloadStateForItems(List<Long> mediaIds) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            adapter.setDownloadStateForMediaIds(mediaIds, 0);
        } finally {
            adapter.close();
        }
    }

    public static void setWantsRedownloadForItems(List<Long> mediaIds) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            adapter.setDownloadStateForMediaIds(mediaIds, FeedMedia.DOWNLOAD_DATE_WANTS_REDOWNLOAD);
        } finally {
            adapter.close();
        }
    }
}
