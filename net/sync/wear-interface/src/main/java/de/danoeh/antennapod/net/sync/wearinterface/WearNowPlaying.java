package de.danoeh.antennapod.net.sync.wearinterface;

import de.danoeh.antennapod.model.feed.FeedItem;

public class WearNowPlaying {
    public final FeedItem item;
    public final boolean isPlaying;
    public final String coverUrl;

    public WearNowPlaying(FeedItem item, boolean isPlaying, String coverUrl) {
        this.item = item;
        this.isPlaying = isPlaying;
        this.coverUrl = coverUrl;
    }
}
