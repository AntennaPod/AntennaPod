package de.danoeh.antennapod.net.download.service.episode.autodownload;

import android.content.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;

public class RedownloadAlgorithm {

    /**
     * Enqueues downloads for all episodes marked with
     * {@link de.danoeh.antennapod.model.feed.FeedMedia#DOWNLOAD_DATE_WANTS_REDOWNLOAD}.
     * Priority order: currently-playing episode (expedited), then queue order, then the rest.
     */
    public void enqueueRedownloads(Context context) {
        List<FeedItem> toRedownload = DBReader.getEpisodesMarkedForRedownload();
        if (toRedownload.isEmpty()) {
            return;
        }
        long currentMediaId = PlaybackPreferences.getCurrentlyPlayingFeedMediaId();
        Map<Long, FeedItem> remaining = new LinkedHashMap<>();
        for (FeedItem item : toRedownload) {
            remaining.put(item.getId(), item);
        }
        for (FeedItem item : toRedownload) {
            if (item.getMedia() != null && item.getMedia().getId() == currentMediaId) {
                DownloadServiceInterface.get().downloadNow(context, item, false, true);
                remaining.remove(item.getId());
                break;
            }
        }
        for (FeedItem queueItem : DBReader.getQueue()) {
            FeedItem item = remaining.remove(queueItem.getId());
            if (item != null) {
                DownloadServiceInterface.get().download(context, item, true);
            }
        }
        for (FeedItem item : remaining.values()) {
            DownloadServiceInterface.get().download(context, item, true);
        }
    }
}
