package de.danoeh.antennapod.event.settings;

import de.danoeh.antennapod.model.feed.FeedPreferences;

public class SpeedPresetChangedEvent {
    private final float speed;
    private final FeedPreferences.SkipSilence skipSilence;
    private final long feedId;

    public SpeedPresetChangedEvent(float speed, long feedId, FeedPreferences.SkipSilence skipSilence) {
        this.speed = speed;
        this.feedId = feedId;
        this.skipSilence = skipSilence;
    }

    public float getSpeed() {
        return speed;
    }

    public FeedPreferences.SkipSilence getSkipSilence() {
        return skipSilence;
    }

    public long getFeedId() {
        return feedId;
    }
}
