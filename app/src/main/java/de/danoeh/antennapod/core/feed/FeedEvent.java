package de.danoeh.antennapod.core.feed;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class FeedEvent {

    public enum Action {
        FILTER_CHANGED
    }

    public final Action action;
    public final long feedId;

    public FeedEvent(Action action, long feedId) {
        this.action = action;
        this.feedId = feedId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("action", action)
                .append("feedId", feedId)
                .toString();
    }

}
