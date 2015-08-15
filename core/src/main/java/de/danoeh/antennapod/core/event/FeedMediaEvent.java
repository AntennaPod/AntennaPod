package de.danoeh.antennapod.core.event;

import de.danoeh.antennapod.core.feed.FeedMedia;

public class FeedMediaEvent {

    public enum Action {
        UPDATE
    }

    public final Action action;
    public final FeedMedia media;

    private FeedMediaEvent(Action action, FeedMedia media) {
        this.action = action;
        this.media = media;
    }

    public static FeedMediaEvent update(FeedMedia media) {
        return new FeedMediaEvent(Action.UPDATE, media);
    }


}
