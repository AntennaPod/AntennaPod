package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import androidx.annotation.NonNull;
import android.util.Log;

import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.core.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.widget.WidgetUpdater;
import io.reactivex.disposables.Disposable;
import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.model.playback.Playable;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/**
 * Manages the background tasks of PlaybackSerivce, i.e.
 * the sleep timer, the position saver, the widget updater and
 * the queue loader.
 * <p/>
 * The PlaybackServiceTaskManager(PSTM) uses a callback object (PSTMCallback)
 * to notify the PlaybackService about updates from the running tasks.
 */
public class PlaybackServiceTaskManager {
    private static final String TAG = "PlaybackServiceTaskMgr";

    /**
     * Update interval of position saver in milliseconds.
     */
    public static final int POSITION_SAVER_WAITING_INTERVAL = 5000;
    /**
     * Notification interval of widget updater in milliseconds.
     */
    public static final int WIDGET_UPDATER_NOTIFICATION_INTERVAL = 1000;

    private static final int SCHED_EX_POOL_SIZE = 2;
    private final ScheduledThreadPoolExecutor schedExecutor;

    private ScheduledFuture<?> positionSaverFuture;
    private ScheduledFuture<?> widgetUpdaterFuture;
    private ScheduledFuture<?> sleepTimerFuture;
    private volatile Disposable chapterLoaderFuture;

    private SleepTimer sleepTimer;

    private final Context context;
    private final PSTMCallback callback;

    /**
     * Sets up a new PSTM. This method will also start the queue loader task.
     *
     * @param context
     * @param callback A PSTMCallback object for notifying the user about updates. Must not be null.
     */
    public PlaybackServiceTaskManager(@NonNull Context context,
                                      @NonNull PSTMCallback callback) {
        this.context = context;
        this.callback = callback;
        schedExecutor = new ScheduledThreadPoolExecutor(SCHED_EX_POOL_SIZE, r -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    /**
     * Starts the position saver task. If the position saver is already active, nothing will happen.
     */
    public synchronized void startPositionSaver() {
        if (!isPositionSaverActive()) {
            Runnable positionSaver = callback::positionSaverTick;
            positionSaver = useMainThreadIfNecessary(positionSaver);
            positionSaverFuture = schedExecutor.scheduleWithFixedDelay(positionSaver, POSITION_SAVER_WAITING_INTERVAL,
                    POSITION_SAVER_WAITING_INTERVAL, TimeUnit.MILLISECONDS);

            Log.d(TAG, "Started PositionSaver");
        } else {
            Log.d(TAG, "Call to startPositionSaver was ignored.");
        }
    }

    /**
     * Returns true if the position saver is currently running.
     */
    public synchronized boolean isPositionSaverActive() {
        return positionSaverFuture != null && !positionSaverFuture.isCancelled() && !positionSaverFuture.isDone();
    }

    /**
     * Cancels the position saver. If the position saver is not running, nothing will happen.
     */
    public synchronized void cancelPositionSaver() {
        if (isPositionSaverActive()) {
            positionSaverFuture.cancel(false);
            Log.d(TAG, "Cancelled PositionSaver");
        }
    }

    /**
     * Starts the widget updater task. If the widget updater is already active, nothing will happen.
     */
    public synchronized void startWidgetUpdater() {
        if (!isWidgetUpdaterActive() && !schedExecutor.isShutdown()) {
            Runnable widgetUpdater = this::requestWidgetUpdate;
            widgetUpdater = useMainThreadIfNecessary(widgetUpdater);
            widgetUpdaterFuture = schedExecutor.scheduleWithFixedDelay(widgetUpdater,
                    WIDGET_UPDATER_NOTIFICATION_INTERVAL, WIDGET_UPDATER_NOTIFICATION_INTERVAL, TimeUnit.MILLISECONDS);
            Log.d(TAG, "Started WidgetUpdater");
        } else {
            Log.d(TAG, "Call to startWidgetUpdater was ignored.");
        }
    }

    /**
     * Retrieves information about the widget state in the calling thread and then displays it in a background thread.
     */
    public synchronized void requestWidgetUpdate() {
        WidgetUpdater.WidgetState state = callback.requestWidgetState();
        if (!schedExecutor.isShutdown()) {
            schedExecutor.execute(() -> WidgetUpdater.updateWidget(context, state));
        } else {
            Log.d(TAG, "Call to requestWidgetUpdate was ignored.");
        }
    }

    /**
     * Starts a new sleep timer with the given waiting time. If another sleep timer is already active, it will be
     * cancelled first.
     * After waitingTime has elapsed, onSleepTimerExpired() will be called.
     *
     * @throws java.lang.IllegalArgumentException if waitingTime <= 0
     */
    public synchronized void setSleepTimer(long waitingTime) {
        if (waitingTime <= 0) {
            throw new IllegalArgumentException("Waiting time <= 0");
        }

        Log.d(TAG, "Setting sleep timer to " + waitingTime + " milliseconds");
        if (isSleepTimerActive()) {
            sleepTimerFuture.cancel(true);
        }
        sleepTimer = new SleepTimer(waitingTime);
        sleepTimerFuture = schedExecutor.schedule(sleepTimer, 0, TimeUnit.MILLISECONDS);
        EventBus.getDefault().post(SleepTimerUpdatedEvent.justEnabled(waitingTime));
    }

    /**
     * Returns true if the sleep timer is currently active.
     */
    public synchronized boolean isSleepTimerActive() {
        return sleepTimer != null
                && sleepTimerFuture != null
                && !sleepTimerFuture.isCancelled()
                && !sleepTimerFuture.isDone()
                && sleepTimer.getWaitingTime() > 0;
    }

    /**
     * Disables the sleep timer. If the sleep timer is not active, nothing will happen.
     */
    public synchronized void disableSleepTimer() {
        if (isSleepTimerActive()) {
            Log.d(TAG, "Disabling sleep timer");
            sleepTimer.cancel();
        }
    }

    /**
     * Restarts the sleep timer. If the sleep timer is not active, nothing will happen.
     */
    public synchronized void restartSleepTimer() {
        if (isSleepTimerActive()) {
            Log.d(TAG, "Restarting sleep timer");
            sleepTimer.restart();
        }
    }

    /**
     * Returns the current sleep timer time or 0 if the sleep timer is not active.
     */
    public synchronized long getSleepTimerTimeLeft() {
        if (isSleepTimerActive()) {
            return sleepTimer.getWaitingTime();
        } else {
            return 0;
        }
    }

    /**
     * Returns true if the widget updater is currently running.
     */
    public synchronized boolean isWidgetUpdaterActive() {
        return widgetUpdaterFuture != null && !widgetUpdaterFuture.isCancelled() && !widgetUpdaterFuture.isDone();
    }

    /**
     * Cancels the widget updater. If the widget updater is not running, nothing will happen.
     */
    public synchronized void cancelWidgetUpdater() {
        if (isWidgetUpdaterActive()) {
            widgetUpdaterFuture.cancel(false);
            Log.d(TAG, "Cancelled WidgetUpdater");
        }
    }

    /**
     * Starts a new thread that loads the chapter marks from a playable object. If another chapter loader is already active,
     * it will be cancelled first.
     * On completion, the callback's onChapterLoaded method will be called.
     */
    public synchronized void startChapterLoader(@NonNull final Playable media) {
        if (chapterLoaderFuture != null) {
            chapterLoaderFuture.dispose();
            chapterLoaderFuture = null;
        }

        if (media.getChapters() == null) {
            chapterLoaderFuture = Completable.create(emitter -> {
                ChapterUtils.loadChapters(media, context, false);
                emitter.onComplete();
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> callback.onChapterLoaded(media),
                            throwable -> Log.d(TAG, "Error loading chapters: " + Log.getStackTraceString(throwable)));
        }
    }


    /**
     * Cancels all tasks. The PSTM will be in the initial state after execution of this method.
     */
    public synchronized void cancelAllTasks() {
        cancelPositionSaver();
        cancelWidgetUpdater();
        disableSleepTimer();

        if (chapterLoaderFuture != null) {
            chapterLoaderFuture.dispose();
            chapterLoaderFuture = null;
        }
    }

    /**
     * Cancels all tasks and shuts down the internal executor service of the PSTM. The object should not be used after
     * execution of this method.
     */
    public void shutdown() {
        cancelAllTasks();
        schedExecutor.shutdownNow();
    }

    private Runnable useMainThreadIfNecessary(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Called in main thread => ExoPlayer is used
            // Run on ui thread even if called from schedExecutor
            Handler handler = new Handler(Looper.getMainLooper());
            return () -> handler.post(runnable);
        } else {
            return runnable;
        }
    }

    /**
     * Sleeps for a given time and then pauses playback.
     */
    class SleepTimer implements Runnable {
        private static final String TAG = "SleepTimer";
        private static final long UPDATE_INTERVAL = 1000L;
        public static final long NOTIFICATION_THRESHOLD = 10000;
        private boolean hasVibrated = false;
        private final long waitingTime;
        private long timeLeft;
        private ShakeListener shakeListener;

        public SleepTimer(long waitingTime) {
            super();
            this.waitingTime = waitingTime;
            this.timeLeft = waitingTime;
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
                    Log.d(TAG, "Sleep timer is about to expire");
                    if (SleepTimerPreferences.vibrate() && !hasVibrated) {
                        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                        if (v != null) {
                            v.vibrate(500);
                            hasVibrated = true;
                        }
                    }
                    if (shakeListener == null && SleepTimerPreferences.shakeToReset()) {
                        shakeListener = new ShakeListener(context, this);
                    }
                }
                if (timeLeft <= 0) {
                    Log.d(TAG, "Sleep timer expired");
                    if (shakeListener != null) {
                        shakeListener.pause();
                        shakeListener = null;
                    }
                    hasVibrated = false;
                }
            }
        }

        public long getWaitingTime() {
            return timeLeft;
        }

        public void restart() {
            EventBus.getDefault().post(SleepTimerUpdatedEvent.cancelled());
            setSleepTimer(waitingTime);
            if (shakeListener != null) {
                shakeListener.pause();
                shakeListener = null;
            }
        }

        public void cancel() {
            sleepTimerFuture.cancel(true);
            if (shakeListener != null) {
                shakeListener.pause();
            }
            EventBus.getDefault().post(SleepTimerUpdatedEvent.cancelled());
        }
    }

    public interface PSTMCallback {
        void positionSaverTick();

        WidgetUpdater.WidgetState requestWidgetState();

        void onChapterLoaded(Playable media);
    }
}
