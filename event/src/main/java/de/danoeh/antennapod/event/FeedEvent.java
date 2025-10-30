package de.danoeh.antennapod.event;

import androidx.annotation.NonNull;

public class FeedEvent {

    public enum Action {
        FILTER_CHANGED,
        SORT_ORDER_CHANGED
    }

    private final Action action;
    public final long feedId;

    public FeedEvent(Action action, long feedId) {
        this.action = action;
        this.feedId = feedId;
    }

    @NonNull
    @Override
    public String toString() {
        return "FeedEvent{action=" + action + ", feedId=" + feedId + '}';
    }
}
