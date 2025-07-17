package de.danoeh.antennapod.playback.service.internal;

public class TimerValue {
    private final long displayValue;
    private final long milisValue;

    public TimerValue(long displayValue, long milisValue) {
        this.displayValue = displayValue;
        this.milisValue = milisValue;
    }

    public long getDisplayValue() {
        return displayValue;
    }

    public long getMilisValue() {
        return milisValue;
    }
}
