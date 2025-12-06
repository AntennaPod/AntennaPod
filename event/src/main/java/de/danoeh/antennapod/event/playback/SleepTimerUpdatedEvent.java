package de.danoeh.antennapod.event.playback;

// using SleepTimer instance would drag that class into this module, we use generics instead
public class SleepTimerUpdatedEvent<T> {
    private static final long CANCELLED = Long.MAX_VALUE;
    private final long timeLeft;
    private final T instance;

    private SleepTimerUpdatedEvent(T instance, long timeLeft) {
        this.instance = instance;
        this.timeLeft = timeLeft;
    }

    public static <T> SleepTimerUpdatedEvent<T> justEnabled(T instance, long timeLeft) {
        return new SleepTimerUpdatedEvent<T>(instance, -timeLeft);
    }

    public static <T> SleepTimerUpdatedEvent<T> updated(T instance, long timeLeft) {
        return new SleepTimerUpdatedEvent<T>(instance, Math.max(0, timeLeft));
    }

    public static <T> SleepTimerUpdatedEvent<T> cancelled(T instance) {
        return new SleepTimerUpdatedEvent<T>(instance, CANCELLED);
    }

    public T getCallerInstance() {
        return instance;
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
