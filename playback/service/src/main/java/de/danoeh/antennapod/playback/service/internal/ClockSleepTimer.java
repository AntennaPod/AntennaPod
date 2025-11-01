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
    private long initialWaitingTime;
    private long timeLeft;
    private boolean isRunning = false;
    private long lastTick = 0;
    private boolean hasVibrated = false;
    private ShakeListener shakeListener;

    public ClockSleepTimer(final Context context) {
        this.context = context;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void playbackPositionUpdate(PlaybackPositionEvent playbackPositionEvent) {
        Log.d(TAG, "playback position updated");
        long now = System.currentTimeMillis();
        long timeSinceLastTick = now - lastTick;
        lastTick = now;
        if (timeSinceLastTick > 10 * 1000) {
            return; // Ticks should arrive every second. If they didn't, playback was paused for a while.
        }
        timeLeft -= timeSinceLastTick;

        EventBus.getDefault().postSticky(SleepTimerUpdatedEvent.updated(timeLeft));
        if (timeLeft < NOTIFICATION_THRESHOLD) {
            notifyAboutExpiry();
        }
        if (timeLeft <= 0) {
            Log.d(TAG, "Sleep timer expired");
            stop();
        }
    }

    protected void vibrate() {
        final Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager)
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null) {
            return;
        }
        final int duration = 500;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(duration);
        }
    }

    protected void notifyAboutExpiry() {
        Log.d(TAG, "Sleep timer is about to expire");
        if (SleepTimerPreferences.vibrate() && !hasVibrated) {
            vibrate();
            hasVibrated = true;
        }
        // start listening for shakes if shake to reset is enabled
        if (shakeListener == null && SleepTimerPreferences.shakeToReset()) {
            shakeListener = new ShakeListener(getContext(), this);
        }
    }

    @Override
    public boolean isActive() {
        return isRunning && timeLeft > 0;
    }

    public void start(long initialWaitingTime) {
        this.initialWaitingTime = initialWaitingTime;
        this.timeLeft = initialWaitingTime;

        // make sure we've registered for events first
        EventBus.getDefault().register(this);
        final long left = getTimeLeft();
        EventBus.getDefault().post(SleepTimerUpdatedEvent.justEnabled(left));

        lastTick = System.currentTimeMillis();
        EventBus.getDefault().postSticky(SleepTimerUpdatedEvent.updated(timeLeft));

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
        EventBus.getDefault().postSticky(SleepTimerUpdatedEvent.cancelled());
    }

    protected Context getContext() {
        return context;
    }

    @Override
    public long getTimeLeft() {
        return timeLeft;
    }

    @Override
    public void updateRemainingTime(long waitingTimeOrEpisodes) {
        this.timeLeft = waitingTimeOrEpisodes;
    }

    @Override
    public void reset() {
        EventBus.getDefault().post(SleepTimerUpdatedEvent.cancelled());
        updateRemainingTime(initialWaitingTime);
        EventBus.getDefault().post(SleepTimerUpdatedEvent.justEnabled(getTimeLeft()));
    }
}
