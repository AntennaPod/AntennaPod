package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;

/**
 * Sleeps for a given time and then pauses playback.
 */
public class ClockSleepTimer extends BaseSleepTimer implements SleepTimer, Runnable {
    private static final String TAG = "ClockSleepTimer";
    private static final long UPDATE_INTERVAL = 1000L;
    private final Context context;
    private final long initialWaitingTime;
    private long timeLeft;

    public ClockSleepTimer(final Context context, long initialWaitingTime) {
        super();
        this.context = context;
        this.initialWaitingTime = initialWaitingTime;
        this.timeLeft = initialWaitingTime;

        EventBus.getDefault().post(SleepTimerUpdatedEvent.justEnabled(initialWaitingTime));
        resume();
    }

    @Override
    public void run() {
        Log.d(TAG, "Starting");
        long lastTick = System.currentTimeMillis();
        EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(timeLeft));
        while (timeLeft > 0) {
            try {
                Thread.sleep(UPDATE_INTERVAL);
            } catch (InterruptedException e) {
                Log.d(TAG, "Thread was interrupted while waiting");
                e.printStackTrace();
                break;
            }

            long now = System.currentTimeMillis();
            timeLeft -= now - lastTick;
            lastTick = now;

            EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(timeLeft));
            if (timeLeft < NOTIFICATION_THRESHOLD) {
                notifyAboutExpiry();
            }
            if (timeLeft <= 0) {
                Log.d(TAG, "Sleep timer expired");
                stopShakeListener();
            }
        }
    }

    @Override
    protected Context getContext() {
        return context;
    }

    @Override
    protected long getInitialTime() {
        return initialWaitingTime;
    }

    @Override
    public long getTimeLeft() {
        return timeLeft;
    }

    @Override
    public void reset(long waitingTimeOrEpisodes) {
        this.timeLeft = waitingTimeOrEpisodes;
    }
}
