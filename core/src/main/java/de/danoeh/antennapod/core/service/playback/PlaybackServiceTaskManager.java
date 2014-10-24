package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import android.util.Log;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.playback.Playable;

import java.util.List;
import java.util.concurrent.*;

/**
 * Manages the background tasks of PlaybackSerivce, i.e.
 * the sleep timer, the position saver, the widget updater and
 * the queue loader.
 * <p/>
 * The PlaybackServiceTaskManager(PSTM) uses a callback object (PSTMCallback)
 * to notify the PlaybackService about updates from the running tasks.
 */
public class PlaybackServiceTaskManager {
    private static final String TAG = "PlaybackServiceTaskManager";

    /**
     * Update interval of position saver in milliseconds.
     */
    public static final int POSITION_SAVER_WAITING_INTERVAL = 5000;
    /**
     * Notification interval of widget updater in milliseconds.
     */
    public static final int WIDGET_UPDATER_NOTIFICATION_INTERVAL = 1500;

    private static final int SCHED_EX_POOL_SIZE = 2;
    private final ScheduledThreadPoolExecutor schedExecutor;

    private ScheduledFuture positionSaverFuture;
    private ScheduledFuture widgetUpdaterFuture;
    private ScheduledFuture sleepTimerFuture;
    private volatile Future<List<FeedItem>> queueFuture;
    private volatile Future chapterLoaderFuture;

    private SleepTimer sleepTimer;

    private final Context context;
    private final PSTMCallback callback;

    /**
     * Sets up a new PSTM. This method will also start the queue loader task.
     *
     * @param context
     * @param callback A PSTMCallback object for notifying the user about updates. Must not be null.
     */
    public PlaybackServiceTaskManager(Context context, PSTMCallback callback) {
        Validate.notNull(context);
        Validate.notNull(callback);

        this.context = context;
        this.callback = callback;
        schedExecutor = new ScheduledThreadPoolExecutor(SCHED_EX_POOL_SIZE, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });
        loadQueue();
        EventDistributor.getInstance().register(eventDistributorListener);
    }

    private final EventDistributor.EventListener eventDistributorListener = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((EventDistributor.QUEUE_UPDATE & arg) != 0) {
                cancelQueueLoader();
                loadQueue();
            }
        }
    };

    private synchronized boolean isQueueLoaderActive() {
        return queueFuture != null && !queueFuture.isDone();
    }

    private synchronized void cancelQueueLoader() {
        if (isQueueLoaderActive()) {
            queueFuture.cancel(true);
        }
    }

    private synchronized void loadQueue() {
        if (!isQueueLoaderActive()) {
            queueFuture = schedExecutor.submit(new Callable<List<FeedItem>>() {
                @Override
                public List<FeedItem> call() throws Exception {
                    return DBReader.getQueue(context);
                }
            });
        }
    }

    /**
     * Returns the queue if it is already loaded or null if it hasn't been loaded yet.
     * In order to wait until the queue has been loaded, use getQueue()
     */
    public synchronized List<FeedItem> getQueueIfLoaded() {
        if (queueFuture.isDone()) {
            try {
                return queueFuture.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Returns the queue or waits until the PSTM has loaded the queue from the database.
     */
    public synchronized List<FeedItem> getQueue() throws InterruptedException {
        try {
            return queueFuture.get();
        } catch (ExecutionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Starts the position saver task. If the position saver is already active, nothing will happen.
     */
    public synchronized void startPositionSaver() {
        if (!isPositionSaverActive()) {
            Runnable positionSaver = new Runnable() {
                @Override
                public void run() {
                    callback.positionSaverTick();
                }
            };
            positionSaverFuture = schedExecutor.scheduleWithFixedDelay(positionSaver, POSITION_SAVER_WAITING_INTERVAL,
                    POSITION_SAVER_WAITING_INTERVAL, TimeUnit.MILLISECONDS);

            if (BuildConfig.DEBUG) Log.d(TAG, "Started PositionSaver");
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "Call to startPositionSaver was ignored.");
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
            if (BuildConfig.DEBUG) Log.d(TAG, "Cancelled PositionSaver");
        }
    }

    /**
     * Starts the widget updater task. If the widget updater is already active, nothing will happen.
     */
    public synchronized void startWidgetUpdater() {
        if (!isWidgetUpdaterActive()) {
            Runnable widgetUpdater = new Runnable() {
                @Override
                public void run() {
                    callback.onWidgetUpdaterTick();
                }
            };
            widgetUpdaterFuture = schedExecutor.scheduleWithFixedDelay(widgetUpdater, WIDGET_UPDATER_NOTIFICATION_INTERVAL,
                    WIDGET_UPDATER_NOTIFICATION_INTERVAL, TimeUnit.MILLISECONDS);

            if (BuildConfig.DEBUG) Log.d(TAG, "Started WidgetUpdater");
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "Call to startWidgetUpdater was ignored.");
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
        Validate.isTrue(waitingTime > 0, "Waiting time <= 0");

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Setting sleep timer to " + Long.toString(waitingTime)
                    + " milliseconds");
        if (isSleepTimerActive()) {
            sleepTimerFuture.cancel(true);
        }
        sleepTimer = new SleepTimer(waitingTime);
        sleepTimerFuture = schedExecutor.schedule(sleepTimer, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns true if the sleep timer is currently active.
     */
    public synchronized boolean isSleepTimerActive() {
        return sleepTimer != null && sleepTimerFuture != null && !sleepTimerFuture.isCancelled() && !sleepTimerFuture.isDone() && sleepTimer.isWaiting;
    }

    /**
     * Disables the sleep timer. If the sleep timer is not active, nothing will happen.
     */
    public synchronized void disableSleepTimer() {
        if (isSleepTimerActive()) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Disabling sleep timer");
            sleepTimerFuture.cancel(true);
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
            if (BuildConfig.DEBUG) Log.d(TAG, "Cancelled WidgetUpdater");
        }
    }

    private synchronized void cancelChapterLoader() {
        if (isChapterLoaderActive()) {
            chapterLoaderFuture.cancel(true);
        }
    }

    private synchronized boolean isChapterLoaderActive() {
        return chapterLoaderFuture != null && !chapterLoaderFuture.isDone();
    }

    /**
     * Starts a new thread that loads the chapter marks from a playable object. If another chapter loader is already active,
     * it will be cancelled first.
     * On completion, the callback's onChapterLoaded method will be called.
     */
    public synchronized void startChapterLoader(final Playable media) {
        Validate.notNull(media);

        if (isChapterLoaderActive()) {
            cancelChapterLoader();
        }

        Runnable chapterLoader = new Runnable() {
            @Override
            public void run() {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Chapter loader started");
                if (media.getChapters() == null) {
                    media.loadChapterMarks();
                    if (!Thread.currentThread().isInterrupted() && media.getChapters() != null) {
                        callback.onChapterLoaded(media);
                    }
                }
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Chapter loader stopped");
            }
        };
        chapterLoaderFuture = schedExecutor.submit(chapterLoader);
    }


    /**
     * Cancels all tasks. The PSTM will be in the initial state after execution of this method.
     */
    public synchronized void cancelAllTasks() {
        cancelPositionSaver();
        cancelWidgetUpdater();
        disableSleepTimer();
        cancelQueueLoader();
        cancelChapterLoader();
    }

    /**
     * Cancels all tasks and shuts down the internal executor service of the PSTM. The object should not be used after
     * execution of this method.
     */
    public synchronized void shutdown() {
        EventDistributor.getInstance().unregister(eventDistributorListener);
        cancelAllTasks();
        schedExecutor.shutdown();
    }

    /**
     * Sleeps for a given time and then pauses playback.
     */
    private class SleepTimer implements Runnable {
        private static final String TAG = "SleepTimer";
        private static final long UPDATE_INTERVALL = 1000L;
        private volatile long waitingTime;
        private volatile boolean isWaiting;

        public SleepTimer(long waitingTime) {
            super();
            this.waitingTime = waitingTime;
            isWaiting = true;
        }

        @Override
        public void run() {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Starting");
            while (waitingTime > 0) {
                try {
                    Thread.sleep(UPDATE_INTERVALL);
                    waitingTime -= UPDATE_INTERVALL;

                    if (waitingTime <= 0) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Waiting completed");
                        postExecute();
                        if (!Thread.currentThread().isInterrupted()) {
                            callback.onSleepTimerExpired();
                        }

                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Thread was interrupted while waiting");
                    break;
                }
            }
            postExecute();
        }

        protected void postExecute() {
            isWaiting = false;
        }

        public long getWaitingTime() {
            return waitingTime;
        }

        public boolean isWaiting() {
            return isWaiting;
        }

    }

    public static interface PSTMCallback {
        void positionSaverTick();

        void onSleepTimerExpired();

        void onWidgetUpdaterTick();

        void onChapterLoaded(Playable media);
    }
}
