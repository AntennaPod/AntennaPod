package de.danoeh.antennapod.event;


import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.model.feed.FeedItem;

public class FeedItemEvent {

    public enum Action {
        UPDATE, DELETE_MEDIA
    }

    @NonNull
    private final Action action;
    @NonNull public final List<FeedItem> items;

    private FeedItemEvent(@NonNull Action action, @NonNull List<FeedItem> items) {
        this.action = action;
        this.items = items;
    }

    public static FeedItemEvent deletedMedia(List<FeedItem> items) {
        return new FeedItemEvent(Action.DELETE_MEDIA, items);
    }

    public static FeedItemEvent deletedMedia(FeedItem... items) {
        return deletedMedia(Arrays.asList(items));
    }

    public static FeedItemEvent updated(List<FeedItem> items) {
        return new FeedItemEvent(Action.UPDATE, items);
    }

    public static FeedItemEvent updated(FeedItem... items) {
        return updated(Arrays.asList(items));
    }
}
