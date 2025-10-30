package de.danoeh.antennapod.model.playback;

public class TimerValue {
    private final long displayValue; // Value shown to user (milliseconds or number of episodes)
    private final long millisValue;

    public TimerValue(long displayValue, long millisValue) {
        this.displayValue = displayValue;
        this.millisValue = millisValue;
    }

    public long getDisplayValue() {
        return displayValue;
    }

    public long getMillisValue() {
        return millisValue;
    }
}
