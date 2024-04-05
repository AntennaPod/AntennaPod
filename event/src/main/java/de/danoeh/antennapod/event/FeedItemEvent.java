package de.danoeh.antennapod.event;


import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.model.feed.FeedItem;

public class FeedItemEvent {
    @NonNull public final List<FeedItem> items;

    public FeedItemEvent(@NonNull List<FeedItem> items) {
        this.items = items;
    }

    public static FeedItemEvent updated(List<FeedItem> items) {
        return new FeedItemEvent(items);
    }

    public static FeedItemEvent updated(FeedItem... items) {
        return new FeedItemEvent(Arrays.asList(items));
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
}
