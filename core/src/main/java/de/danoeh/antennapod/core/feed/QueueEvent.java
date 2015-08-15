package de.danoeh.antennapod.core.feed;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

public class QueueEvent {

    public enum Action {
        ADDED, ADDED_ITEMS, REMOVED, IRREVERSIBLE_REMOVED, CLEARED, DELETED_MEDIA, SORTED, MOVED
    }

    public final Action action;
    public final FeedItem item;
    public final int position;
    public final List<FeedItem> items;

    public QueueEvent(Action action) {
        this(action, null, null, -1);
    }

    public QueueEvent(Action action, FeedItem item) {
        this(action, item, null, -1);
    }

    public QueueEvent(Action action, FeedItem item, int position) {
        this(action, item, null, position);
    }

    public QueueEvent(Action action, List<FeedItem> items) {
        this(action, null, items, -1);
    }

    private QueueEvent(Action action, FeedItem item, List<FeedItem> items, int position) {
        this.action = action;
        this.item = item;
        this.items = items;
        this.position = position;
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
