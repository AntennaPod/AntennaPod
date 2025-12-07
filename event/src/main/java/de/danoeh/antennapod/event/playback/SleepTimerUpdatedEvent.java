package de.danoeh.antennapod.event.playback;

import java.lang.ref.WeakReference;

// using SleepTimer instance would drag that class into this module, we use generics instead
public class SleepTimerUpdatedEvent<T> {
    private static final long CANCELLED = Long.MAX_VALUE;
    private final long timeLeft;
    private final WeakReference<T> instance;

    private SleepTimerUpdatedEvent(T instance, long timeLeft) {
        this.instance = new WeakReference<>(instance);
        this.timeLeft = timeLeft;
    }

    public static <T> SleepTimerUpdatedEvent<T> justEnabled(T instance, long timeLeft) {
        return new SleepTimerUpdatedEvent<>(instance, -timeLeft);
    }

    public static <T> SleepTimerUpdatedEvent<T> updated(T instance, long timeLeft) {
        return new SleepTimerUpdatedEvent<>(instance, Math.max(0, timeLeft));
    }

    public static <T> SleepTimerUpdatedEvent<T> cancelled(T instance) {
        return new SleepTimerUpdatedEvent<>(instance, CANCELLED);
    }

    public T getCallerInstance() {
        return instance.get();
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
