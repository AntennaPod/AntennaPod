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
}
