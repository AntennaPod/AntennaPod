package de.danoeh.antennapod.core.event;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

import de.danoeh.antennapod.model.feed.FeedItem;

public class QueueEvent {

    public enum Action {
        ADDED, ADDED_ITEMS, SET_QUEUE, REMOVED, IRREVERSIBLE_REMOVED, CLEARED, DELETED_MEDIA, SORTED, MOVED
    }

    public final Action action;
    public final FeedItem item;
    public final int position;
    public final List<FeedItem> items;


    private QueueEvent(Action action,
                       @Nullable FeedItem item,
                       @Nullable List<FeedItem> items,
                       int position) {
        this.action = action;
        this.item = item;
        this.items = items;
        this.position = position;
    }

    public static QueueEvent added(FeedItem item, int position) {
        return new QueueEvent(Action.ADDED, item, null, position);
    }

    public static QueueEvent setQueue(List<FeedItem> queue) {
        return new QueueEvent(Action.SET_QUEUE, null, queue, -1);
    }

    public static QueueEvent removed(FeedItem item) {
        return new QueueEvent(Action.REMOVED, item, null, -1);
    }

    public static QueueEvent irreversibleRemoved(FeedItem item) {
        return new QueueEvent(Action.IRREVERSIBLE_REMOVED, item, null, -1);
    }

    public static QueueEvent cleared() {
        return new QueueEvent(Action.CLEARED, null, null, -1);
    }

    public static QueueEvent sorted(List<FeedItem> sortedQueue) {
        return new QueueEvent(Action.SORTED, null, sortedQueue, -1);
    }

    public static QueueEvent moved(FeedItem item, int newPosition) {
        return new QueueEvent(Action.MOVED, item, null, newPosition);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("action", action)
                .append("item", item)
                .append("items", items)
                .append("position", position)
                .toString();
    }
}
