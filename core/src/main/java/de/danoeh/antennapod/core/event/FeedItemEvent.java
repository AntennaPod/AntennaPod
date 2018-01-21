package de.danoeh.antennapod.core.event;


import android.support.annotation.NonNull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedItem;

public class FeedItemEvent {

    public enum Action {
        UPDATE, DELETE_MEDIA
    }

    @NonNull
    private final Action action;
    @NonNull public final List<FeedItem> items;

    private FeedItemEvent(Action action, List<FeedItem> items) {
        this.action = action;
        this.items = items;
    }

    public static FeedItemEvent deletedMedia(List<FeedItem> items) {
        return new FeedItemEvent(Action.DELETE_MEDIA, items);
    }

    public static FeedItemEvent updated(List<FeedItem> items) {
        return new FeedItemEvent(Action.UPDATE, items);
    }

    public static FeedItemEvent updated(FeedItem... items) {
        return new FeedItemEvent(Action.UPDATE, Arrays.asList(items));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("action", action)
            .append("items", items)
            .toString();
    }

}
