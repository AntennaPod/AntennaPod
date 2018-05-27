package de.danoeh.antennapod.core.feed;

import java.util.Date;

import static de.danoeh.antennapod.core.feed.FeedImageMother.anyFeedImage;
import static de.danoeh.antennapod.core.feed.FeedMother.anyFeed;

class FeedItemMother {

    static FeedItem anyFeedItemWithImage() {
        FeedItem item = new FeedItem(0, "Item", "Item", "url", new Date(), FeedItem.PLAYED, anyFeed());
        item.setImage(anyFeedImage());
        return item;
    }

}
