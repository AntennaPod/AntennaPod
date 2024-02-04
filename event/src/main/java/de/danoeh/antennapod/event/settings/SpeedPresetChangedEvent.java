package de.danoeh.antennapod.event.settings;

public class SpeedPresetChangedEvent {
    private final float speed;
    private final Boolean skipSilence;
    private final long feedId;

    public SpeedPresetChangedEvent(float speed, long feedId, Boolean skipSilence) {
        this.speed = speed;
        this.feedId = feedId;
        this.skipSilence = skipSilence;
    }

    public float getSpeed() {
        return speed;
    }
    public Boolean getSkipSilence() {
        return skipSilence;
    }

    public long getFeedId() {
        return feedId;
    }
}
