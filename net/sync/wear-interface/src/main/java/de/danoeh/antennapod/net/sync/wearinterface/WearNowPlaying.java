package de.danoeh.antennapod.net.sync.wearinterface;

import de.danoeh.antennapod.model.feed.FeedItem;

public class WearNowPlaying {
    public final FeedItem item;
    public final boolean isPlaying;
    public final float playbackSpeed;

    public WearNowPlaying(FeedItem item, boolean isPlaying, float playbackSpeed) {
        this.item = item;
        this.isPlaying = isPlaying;
        this.playbackSpeed = playbackSpeed;
    }
}
