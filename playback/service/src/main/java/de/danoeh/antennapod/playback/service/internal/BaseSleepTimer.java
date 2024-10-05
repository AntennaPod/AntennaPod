package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;

public abstract class BaseSleepTimer implements SleepTimer, Runnable {
    private static final String TAG = "BaseSleepTimer";
    protected boolean hasVibrated = false;
    protected ShakeListener shakeListener;

    private ScheduledExecutorService schedExecutor = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> sleepTimerFuture;

    protected abstract Context getContext();

    protected abstract long getInitialTime();

    protected void notifyAboutExpiry() {
        Log.d(TAG, "Sleep timer is about to expire");
        if (SleepTimerPreferences.vibrate() && !hasVibrated) {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                v.vibrate(500);
                hasVibrated = true;
            }
        }
        if (shakeListener == null && SleepTimerPreferences.shakeToReset()) {
            shakeListener = new ShakeListener(getContext(), getInitialTime(), this);
        }
    }

    protected void stopShakeListener() {
        if (shakeListener != null) {
            shakeListener.pause();
            shakeListener = null;
        }
        hasVibrated = false;
    }

    @Override
    public boolean isActive() {
        return sleepTimerFuture != null
                && !sleepTimerFuture.isCancelled()
                && !sleepTimerFuture.isDone()
                && getTimeLeft() > 0;
    }


    @Override
    public void resume() {
        if (isActive()) {
            pause();
        }

        sleepTimerFuture = schedExecutor.schedule(this, 0, TimeUnit.MILLISECONDS);
        if (shakeListener == null) {
            shakeListener = new ShakeListener(getContext(), getInitialTime(), this);
        }
    }

    @Override
    public void pause() {
        if (isActive()) {
            sleepTimerFuture.cancel(true);
        }
        sleepTimerFuture = null;
        shakeListener.pause();
        shakeListener = null;
    }

    @Override
    public void stop() {
        sleepTimerFuture.cancel(true);
        if (shakeListener != null) {
            shakeListener.pause();
        }
        EventBus.getDefault().post(SleepTimerUpdatedEvent.cancelled());
    }
}
