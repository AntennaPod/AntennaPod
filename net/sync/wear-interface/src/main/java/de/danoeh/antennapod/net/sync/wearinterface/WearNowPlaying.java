package de.danoeh.antennapod.net.sync.wearinterface;

import de.danoeh.antennapod.model.feed.FeedItem;

public class WearNowPlaying {
    public final FeedItem item;
    public final boolean isPlaying;

    public WearNowPlaying(FeedItem item, boolean isPlaying) {
        this.item = item;
        this.isPlaying = isPlaying;
    }
}
