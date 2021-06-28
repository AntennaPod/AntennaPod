package de.danoeh.antennapod.core.event;

import de.danoeh.antennapod.model.feed.Feed;

import java.util.ArrayList;
import java.util.List;

public class FeedListUpdateEvent {
    private final List<Long> feeds = new ArrayList<>();

    public FeedListUpdateEvent(List<Feed> feeds) {
        for (Feed feed : feeds) {
            this.feeds.add(feed.getId());
        }
    }

    public FeedListUpdateEvent(Feed feed) {
        feeds.add(feed.getId());
    }

    public FeedListUpdateEvent(long feedId) {
        feeds.add(feedId);
    }

    public boolean contains(Feed feed) {
        return feeds.contains(feed.getId());
    }
}
