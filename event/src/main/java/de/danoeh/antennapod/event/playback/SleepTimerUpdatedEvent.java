package de.danoeh.antennapod.event.playback;

import de.danoeh.antennapod.model.playback.TimerValue;

public class SleepTimerUpdatedEvent {
    private static final long CANCELLED = Long.MAX_VALUE;
    private final TimerValue timerValue;

    private SleepTimerUpdatedEvent(final TimerValue timerValue) {
        this.timerValue = timerValue;
    }

    public static SleepTimerUpdatedEvent justEnabled(final TimerValue timer) {
        return new SleepTimerUpdatedEvent(new TimerValue(timer.getDisplayValue(), -timer.getMillisValue()));
    }

    public static SleepTimerUpdatedEvent updated(final TimerValue timer) {
        return new SleepTimerUpdatedEvent(
                new TimerValue(Math.max(timer.getDisplayValue(), 0), Math.max(0, timer.getMillisValue())));
    }

    public static SleepTimerUpdatedEvent cancelled() {
        return new SleepTimerUpdatedEvent(new TimerValue(CANCELLED, CANCELLED));
    }

    public long getMillisTimeLeft() {
        return Math.abs(timerValue.getMillisValue());
    }

    public long getDisplayTimeLeft() {
        return Math.abs(timerValue.getDisplayValue());
    }

    public boolean isOver() {
        return timerValue.getMillisValue() == 0;
    }

    public boolean wasJustEnabled() {
        return timerValue.getMillisValue() < 0;
    }

    public boolean isCancelled() {
        return timerValue.getMillisValue() == CANCELLED;
    }
}
