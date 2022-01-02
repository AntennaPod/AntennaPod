package de.danoeh.antennapod.event.playback;

public class SleepTimerUpdatedEvent {
    private static final long CANCELLED = Long.MAX_VALUE;
    private final long timeLeft;

    private SleepTimerUpdatedEvent(long timeLeft) {
        this.timeLeft = timeLeft;
    }

    public static SleepTimerUpdatedEvent justEnabled(long timeLeft) {
        return new SleepTimerUpdatedEvent(-timeLeft);
    }

    public static SleepTimerUpdatedEvent updated(long timeLeft) {
        return new SleepTimerUpdatedEvent(Math.max(0, timeLeft));
    }

    public static SleepTimerUpdatedEvent cancelled() {
        return new SleepTimerUpdatedEvent(CANCELLED);
    }

    public long getTimeLeft() {
        return Math.abs(timeLeft);
    }

    public boolean isOver() {
        return timeLeft == 0;
    }

    public boolean wasJustEnabled() {
        return timeLeft < 0;
    }

    public boolean isCancelled() {
        return timeLeft == CANCELLED;
    }
}
