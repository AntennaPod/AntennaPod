package de.danoeh.antennapod.event;

import de.danoeh.antennapod.model.feed.FeedItem;

public class FavoritesEvent {

    public enum Action {
        ADDED, REMOVED
    }

    private final Action action;
    private final FeedItem item;

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
}
