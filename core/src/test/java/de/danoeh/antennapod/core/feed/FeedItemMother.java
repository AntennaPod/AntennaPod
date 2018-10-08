package de.danoeh.antennapod.core.feed;

import android.support.annotation.NonNull;

import java.util.Date;

import static de.danoeh.antennapod.core.feed.FeedMother.anyFeed;

class FeedItemMother {
    private static final String IMAGE_URL = "http://example.com/image";

    @NonNull
    static FeedItem anyFeedItemWithImage() {
        FeedItem item = new FeedItem(0, "Item", "Item", "url", new Date(), FeedItem.PLAYED, anyFeed());
        item.setImageUrl(IMAGE_URL);
        return item;
    }

}
