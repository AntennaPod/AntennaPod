package de.danoeh.antennapod.event.playback;

public class SleepTimerUpdatedEvent {
    private static final long CANCELLED = Long.MAX_VALUE;
    private final long millisTimeLeft;
    private final long displayTimeLeft;

    private SleepTimerUpdatedEvent(long displayTimeLeft, long millisTimeLeft) {
        this.displayTimeLeft = displayTimeLeft;
        this.millisTimeLeft = millisTimeLeft;
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

    public long getMillisTimeLeft() {
        return Math.abs(millisTimeLeft);
    }

    public long getDisplayTimeLeft() {
        return Math.abs(displayTimeLeft);
    }

    public boolean isOver() {
        return millisTimeLeft == 0;
    }

    public boolean wasJustEnabled() {
        return millisTimeLeft < 0;
    }

    public boolean isCancelled() {
        return millisTimeLeft == CANCELLED;
    }
}
