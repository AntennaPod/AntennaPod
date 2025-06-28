package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;

public class ClockSleepTimer implements SleepTimer {
    private static final String TAG = "ClockSleepTimer";
    private final Context context;
    private final long initialWaitingTime;
    private long timeLeft;
    private boolean isRunning = false;
    private long lastTick = 0;

    protected boolean hasVibrated = false;
    protected ShakeListener shakeListener;

    public ClockSleepTimer(final Context context, long initialWaitingTime) {
        this.context = context;
        this.initialWaitingTime = initialWaitingTime;
        this.timeLeft = initialWaitingTime;

        start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void playbackPositionUpdate(PlaybackPositionEvent playbackPositionEvent) {
        Log.d(TAG, "playback position updated");
        long now = System.currentTimeMillis();
        timeLeft -= now - lastTick;
        lastTick = now;

        EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(timeLeft));
        if (timeLeft < NOTIFICATION_THRESHOLD) {
            notifyAboutExpiry();
        }
        if (timeLeft <= 0) {
            Log.d(TAG, "Sleep timer expired");
            stop();
        }
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

    @Override
    public boolean isActive() {
        return isRunning && timeLeft > 0;
    }

    public void start() {
        registerForEvents(); // make sure we've registered for events first

        lastTick = System.currentTimeMillis();
        EventBus.getDefault().post(SleepTimerUpdatedEvent.updated(timeLeft));

        isRunning = true;
    }

    @Override
    public void stop() {
        timeLeft = 0;
        EventBus.getDefault().unregister(this);

        if (shakeListener != null) {
            shakeListener.pause();
        }
        shakeListener = null;
        EventBus.getDefault().post(SleepTimerUpdatedEvent.cancelled());
    }

    private void registerForEvents() {
        EventBus.getDefault().register(this);
        final long left = getTimeLeft();
        EventBus.getDefault().post(SleepTimerUpdatedEvent.justEnabled(left));
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
