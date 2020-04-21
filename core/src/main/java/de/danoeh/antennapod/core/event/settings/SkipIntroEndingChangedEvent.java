package de.danoeh.antennapod.core.event.settings;

public class SkipIntroEndingChangedEvent {
    private final int skipIntro;
    private final int skipEnding;
    private final long feedId;

    public SkipIntroEndingChangedEvent(int skipIntro, int skipEnding, long feedId) {
        this.skipIntro= skipIntro;
        this.skipEnding = skipEnding;
        this.feedId = feedId;
    }

    public int getSkipIntro() {
        return skipIntro;
    }

    public int getSkipEnding() {
        return skipEnding;
    }

    public long getFeedId() {
        return feedId;
    }
}
