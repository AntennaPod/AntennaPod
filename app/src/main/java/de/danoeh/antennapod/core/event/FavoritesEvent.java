package de.danoeh.antennapod.core.event;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.danoeh.antennapod.core.feed.FeedItem;

public class FavoritesEvent {

    public enum Action {
        ADDED, REMOVED
    }

    public final Action action;
    public final FeedItem item;

    private FavoritesEvent(Action action, FeedItem item) {
        this.action = action;
        this.item = item;
    }

    public static FavoritesEvent added(FeedItem item) {
        return new FavoritesEvent(Action.ADDED, item);
    }

    public static FavoritesEvent removed(FeedItem item) {
        return new FavoritesEvent(Action.REMOVED, item);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("action", action)
                .append("item", item)
                .toString();
    }

}
