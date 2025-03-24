package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;

public class ClockSleepTimer implements SleepTimer, Runnable {
    private static final String TAG = "ClockSleepTimer";
    private static final long UPDATE_INTERVAL = 1000L;
    private final Context context;
    private final long initialWaitingTime;
    private long timeLeft;
    private boolean isPaused = false;

    protected boolean hasVibrated = false;
    protected ShakeListener shakeListener;

    private final ScheduledExecutorService schedExecutor = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> sleepTimerFuture;

    public ClockSleepTimer(final Context context, long initialWaitingTime) {
        this.context = context;
        this.initialWaitingTime = initialWaitingTime;
        this.timeLeft = initialWaitingTime;

        EventBus.getDefault().post(SleepTimerUpdatedEvent.justEnabled(initialWaitingTime));
        resume();
    }

    protected boolean vibrate() {
        final Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager)
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        final int duration = 500;
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final VibrationEffect effect = VibrationEffect.createOneShot(
                        duration, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(duration);
            }
            return true;
        }

        return false;
    }

    protected void notifyAboutExpiry() {
        Log.d(TAG, "Sleep timer is about to expire");
        if (SleepTimerPreferences.vibrate() && !hasVibrated) {
            hasVibrated = vibrate();
        }
        // start listening for shakes if shake to reset is enabled
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
        if (isPaused()) {
            isPaused = false;
        } else {
            sleepTimerFuture = schedExecutor.schedule(this, 0, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public void pause() {
        if (isActive()) {
            isPaused = true;
        }
    }

    @Override
    public void stop() {
        sleepTimerFuture.cancel(true);
        if (shakeListener != null) {
            shakeListener.pause();
        }
        shakeListener = null;
        EventBus.getDefault().post(SleepTimerUpdatedEvent.cancelled());
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
            // if we've been told not to pause while playing or we're not paused then subtract, otherwise
            // we just report the same time left over and over
            // this so that the event is submitted and the Playback dialog shows the remaining time
            // so that the user knows a sleep timer is active
            if (!SleepTimerPreferences.pauseWhileNotPlaying() || !isPaused()) {
                timeLeft -= now - lastTick;
            }
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

    protected Context getContext() {
        return context;
    }

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
