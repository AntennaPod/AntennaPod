package de.danoeh.antennapod.core.feed;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("action", action)
                .append("feedId", feedId)
                .toString();
    }

}
