package de.danoeh.antennapod.event.playback;

public class SleepTimerUpdatedEvent {
    private static final long CANCELLED = Long.MAX_VALUE;
    private final long milisTimeLeft;
    private final long displayTimeLeft;

    private SleepTimerUpdatedEvent(long displayTimeLeft, long milisTimeLeft) {
        this.displayTimeLeft = displayTimeLeft;
        this.milisTimeLeft = milisTimeLeft;
    }

    public static SleepTimerUpdatedEvent justEnabled(long displayTimeLeft, long milisTimeLeft) {
        return new SleepTimerUpdatedEvent(displayTimeLeft, -milisTimeLeft);
    }

    public static SleepTimerUpdatedEvent updated(long displayTimeLeft, long milisTimeLeft) {
        return new SleepTimerUpdatedEvent(Math.max(displayTimeLeft, 0), Math.max(0, milisTimeLeft));
    }

    public static SleepTimerUpdatedEvent cancelled() {
        return new SleepTimerUpdatedEvent(CANCELLED, CANCELLED);
    }

    public long getMilisTimeLeft() {
        return Math.abs(milisTimeLeft);
    }

    public long getDisplayTimeLeft() {
        return Math.abs(displayTimeLeft);
    }

    public boolean isOver() {
        return milisTimeLeft == 0;
    }

    public boolean wasJustEnabled() {
        return milisTimeLeft < 0;
    }

    public boolean isCancelled() {
        return milisTimeLeft == CANCELLED;
    }
}
