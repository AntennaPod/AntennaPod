package de.danoeh.antennapod.core.event.settings;

public class SpeedPresetChangedEvent {
    private final float speed;
    private final long feedId;

    public SpeedPresetChangedEvent(float speed, long feedId) {
        this.speed = speed;
        this.feedId = feedId;
    }

    public float getSpeed() {
        return speed;
    }

    public long getFeedId() {
        return feedId;
    }
}
