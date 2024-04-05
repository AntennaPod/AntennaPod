package de.danoeh.antennapod.event;

import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.model.feed.FeedItem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EpisodeDownloadEvent {
    private final Map<String, DownloadStatus> map;

    public EpisodeDownloadEvent(Map<String, DownloadStatus> map) {
        this.map = map;
    }

    public Set<String> getUrls() {
        return map.keySet();
    }

    public static int indexOfItemWithDownloadUrl(List<FeedItem> items, String downloadUrl) {
        for (int i = 0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            if (item != null && item.getMedia() != null && item.getMedia().getDownloadUrl().equals(downloadUrl)) {
                return i;
            }
        }
        return -1;
    }
}
