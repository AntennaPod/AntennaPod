package de.danoeh.antennapod.core.util.playback;

import android.app.Activity;
import android.content.*;
import android.content.res.TypedArray;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlaybackServiceMediaPlayer;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.playback.Playable.PlayableUtils;

import java.util.concurrent.*;

/**
 * Communicates with the playback service. GUI classes should use this class to
 * control playback instead of communicating with the PlaybackService directly.
 */
public abstract class PlaybackController {
    private static final String TAG = "PlaybackController";

    public static final int INVALID_TIME = -1;

    private final Activity activity;

    private PlaybackService playbackService;
    private Playable media;
    private PlayerStatus status;

    private ScheduledThreadPoolExecutor schedExecutor;
    private static final int SCHED_EX_POOLSIZE = 1;

    protected MediaPositionObserver positionObserver;
    protected ScheduledFuture positionObserverFuture;

    private boolean mediaInfoLoaded = false;
    private boolean released = false;

    /**
     * True if controller should reinit playback service if 'pause' button is
     * pressed.
     */
    private boolean reinitOnPause;

    public PlaybackController(Activity activity, boolean reinitOnPause) {
        Validate.notNull(activity);

        this.activity = activity;
        this.reinitOnPause = reinitOnPause;
        schedExecutor = new ScheduledThreadPoolExecutor(SCHED_EX_POOLSIZE,
                new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setPriority(Thread.MIN_PRIORITY);
                        return t;
                    }
                }, new RejectedExecutionHandler() {

            @Override
            public void rejectedExecution(Runnable r,
                                          ThreadPoolExecutor executor) {
                Log.w(TAG,
                        "Rejected execution of runnable in schedExecutor");
            }
        }
        );
    }

    /**
     * Creates a new connection to the playbackService. Should be called in the
     * activity's onResume() method.
     */
    public void init() {
        activity.registerReceiver(statusUpdate, new IntentFilter(
                PlaybackService.ACTION_PLAYER_STATUS_CHANGED));

        activity.registerReceiver(notificationReceiver, new IntentFilter(
                PlaybackService.ACTION_PLAYER_NOTIFICATION));

        activity.registerReceiver(shutdownReceiver, new IntentFilter(
                PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));

        if (!released) {
            bindToService();
        } else {
            throw new IllegalStateException(
                    "Can't call init() after release() has been called");
        }
    }

    /**
     * Should be called if the PlaybackController is no longer needed, for
     * example in the activity's onStop() method.
     */
    public void release() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Releasing PlaybackController");

        try {
            activity.unregisterReceiver(statusUpdate);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        try {
            activity.unregisterReceiver(notificationReceiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        try {
            activity.unbindService(mConnection);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        try {
            activity.unregisterReceiver(shutdownReceiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        cancelPositionObserver();
        schedExecutor.shutdownNow();
        media = null;
        released = true;

    }

    /**
     * Should be called in the activity's onPause() method.
     */
    public void pause() {
        mediaInfoLoaded = false;
    }

    /**
     * Tries to establish a connection to the PlaybackService. If it isn't
     * running, the PlaybackService will be started with the last played media
     * as the arguments of the launch intent.
     */
    private void bindToService() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Trying to connect to service");
        AsyncTask<Void, Void, Intent> intentLoader = new AsyncTask<Void, Void, Intent>() {
            @Override
            protected Intent doInBackground(Void... voids) {
                return getPlayLastPlayedMediaIntent();
            }

            @Override
            protected void onPostExecute(Intent serviceIntent) {
                boolean bound = false;
                if (!PlaybackService.started) {
                    if (serviceIntent != null) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Calling start service");
                        activity.startService(serviceIntent);
                        bound = activity.bindService(serviceIntent, mConnection, 0);
                    } else {
                        status = PlayerStatus.STOPPED;
                        setupGUI();
                        handleStatus();
                    }
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG,
                                "PlaybackService is running, trying to connect without start command.");
                    bound = activity.bindService(new Intent(activity,
                            PlaybackService.class), mConnection, 0);
                }
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Result for service binding: " + bound);
            }
        };
        intentLoader.execute();
    }

    /**
     * Returns an intent that starts the PlaybackService and plays the last
     * played media or null if no last played media could be found.
     */
    private Intent getPlayLastPlayedMediaIntent() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Trying to restore last played media");
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(activity.getApplicationContext());
        long currentlyPlayingMedia = PlaybackPreferences
                .getCurrentlyPlayingMedia();
        if (currentlyPlayingMedia != PlaybackPreferences.NO_MEDIA_PLAYING) {
            Playable media = PlayableUtils.createInstanceFromPreferences(activity,
                    (int) currentlyPlayingMedia, prefs);
            if (media != null) {
                Intent serviceIntent = new Intent(activity,
                        PlaybackService.class);
                serviceIntent.putExtra(PlaybackService.EXTRA_PLAYABLE, media);
                serviceIntent.putExtra(
                        PlaybackService.EXTRA_START_WHEN_PREPARED, false);
                serviceIntent.putExtra(
                        PlaybackService.EXTRA_PREPARE_IMMEDIATELY, false);
                boolean fileExists = media.localFileAvailable();
                boolean lastIsStream = PlaybackPreferences
                        .getCurrentEpisodeIsStream();
                if (!fileExists && !lastIsStream && media instanceof FeedMedia) {
                    DBTasks.notifyMissingFeedMediaFile(
                            activity, (FeedMedia) media);
                }
                serviceIntent.putExtra(PlaybackService.EXTRA_SHOULD_STREAM,
                        lastIsStream || !fileExists);
                return serviceIntent;
            }
        }
        if (BuildConfig.DEBUG)
            Log.d(TAG, "No last played media found");
        return null;
    }

    public abstract void setupGUI();

    private void setupPositionObserver() {
        if ((positionObserverFuture != null && positionObserverFuture
                .isCancelled())
                || (positionObserverFuture != null && positionObserverFuture
                .isDone()) || positionObserverFuture == null) {

            if (BuildConfig.DEBUG)
                Log.d(TAG, "Setting up position observer");
            positionObserver = new MediaPositionObserver();
            positionObserverFuture = schedExecutor.scheduleWithFixedDelay(
                    positionObserver, MediaPositionObserver.WAITING_INTERVALL,
                    MediaPositionObserver.WAITING_INTERVALL,
                    TimeUnit.MILLISECONDS);
        }
    }

    private void cancelPositionObserver() {
        if (positionObserverFuture != null) {
            boolean result = positionObserverFuture.cancel(true);
            if (BuildConfig.DEBUG)
                Log.d(TAG, "PositionObserver cancelled. Result: " + result);
        }
    }

    public abstract void onPositionObserverUpdate();

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            playbackService = ((PlaybackService.LocalBinder) service)
                    .getService();
            if (!released) {
                queryService();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Connection to Service established");
            } else {
                Log.i(TAG, "Connection to playback service has been established, but controller has already been released");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Disconnected from Service");

        }
    };

    protected BroadcastReceiver statusUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Received statusUpdate Intent.");
            if (isConnectedToPlaybackService()) {
                PlaybackServiceMediaPlayer.PSMPInfo info = playbackService.getPSMPInfo();
                status = info.playerStatus;
                media = info.playable;
                handleStatus();
            } else {
                Log.w(TAG,
                        "Couldn't receive status update: playbackService was null");
                bindToService();
            }
        }
    };

    protected BroadcastReceiver notificationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isConnectedToPlaybackService()) {
                int type = intent.getIntExtra(
                        PlaybackService.EXTRA_NOTIFICATION_TYPE, -1);
                int code = intent.getIntExtra(
                        PlaybackService.EXTRA_NOTIFICATION_CODE, -1);
                if (code != -1 && type != -1) {
                    switch (type) {
                        case PlaybackService.NOTIFICATION_TYPE_ERROR:
                            handleError(code);
                            break;
                        case PlaybackService.NOTIFICATION_TYPE_BUFFER_UPDATE:
                            float progress = ((float) code) / 100;
                            onBufferUpdate(progress);
                            break;
                        case PlaybackService.NOTIFICATION_TYPE_RELOAD:
                            cancelPositionObserver();
                            mediaInfoLoaded = false;
                            queryService();
                            onReloadNotification(intent.getIntExtra(
                                    PlaybackService.EXTRA_NOTIFICATION_CODE, -1));
                            break;
                        case PlaybackService.NOTIFICATION_TYPE_SLEEPTIMER_UPDATE:
                            onSleepTimerUpdate();
                            break;
                        case PlaybackService.NOTIFICATION_TYPE_BUFFER_START:
                            onBufferStart();
                            break;
                        case PlaybackService.NOTIFICATION_TYPE_BUFFER_END:
                            onBufferEnd();
                            break;
                        case PlaybackService.NOTIFICATION_TYPE_PLAYBACK_END:
                            onPlaybackEnd();
                            break;
                        case PlaybackService.NOTIFICATION_TYPE_PLAYBACK_SPEED_CHANGE:
                            onPlaybackSpeedChange();
                            break;
                    }

                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Bad arguments. Won't handle intent");
                }
            } else {
                bindToService();
            }
        }

    };

    private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isConnectedToPlaybackService()) {
                if (StringUtils.equals(intent.getAction(),
                        PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE)) {
                    release();
                    onShutdownNotification();
                }
            }
        }
    };

    public abstract void onPlaybackSpeedChange();

    public abstract void onShutdownNotification();

    /**
     * Called when the currently displayed information should be refreshed.
     */
    public abstract void onReloadNotification(int code);

    public abstract void onBufferStart();

    public abstract void onBufferEnd();

    public abstract void onBufferUpdate(float progress);

    public abstract void onSleepTimerUpdate();

    public abstract void handleError(int code);

    public abstract void onPlaybackEnd();

    public void repeatHandleStatus() {
        if (status != null && playbackService != null) {
            handleStatus();
        }
    }

    /**
     * Is called whenever the PlaybackService changes it's status. This method
     * should be used to update the GUI or start/cancel background threads.
     */
    private void handleStatus() {
        final int playResource;
        final int pauseResource;
        final CharSequence playText = activity.getString(R.string.play_label);
        final CharSequence pauseText = activity.getString(R.string.pause_label);

        if (PlaybackService.getCurrentMediaType() == MediaType.AUDIO) {
            TypedArray res = activity.obtainStyledAttributes(new int[]{
                    R.attr.av_play_big, R.attr.av_pause_big});
            playResource = res.getResourceId(0, R.drawable.ic_play_arrow_grey600_36dp);
            pauseResource = res.getResourceId(1, R.drawable.ic_pause_grey600_36dp);
            res.recycle();
        } else {
            playResource = R.drawable.ic_av_play_circle_outline_80dp;
            pauseResource = R.drawable.ic_av_pause_circle_outline_80dp;
        }

        switch (status) {

            case ERROR:
                postStatusMsg(R.string.player_error_msg);
                handleError(MediaPlayer.MEDIA_ERROR_UNKNOWN);
                break;
            case PAUSED:
                clearStatusMsg();
                checkMediaInfoLoaded();
                cancelPositionObserver();
                updatePlayButtonAppearance(playResource, playText);
                if (PlaybackService.getCurrentMediaType() == MediaType.VIDEO) {
                    setScreenOn(false);
                }
                break;
            case PLAYING:
                clearStatusMsg();
                checkMediaInfoLoaded();
                if (PlaybackService.getCurrentMediaType() == MediaType.VIDEO) {
                    onAwaitingVideoSurface();
                    setScreenOn(true);
                }
                setupPositionObserver();
                updatePlayButtonAppearance(pauseResource, pauseText);
                break;
            case PREPARING:
                postStatusMsg(R.string.player_preparing_msg);
                checkMediaInfoLoaded();
                if (playbackService != null) {
                    if (playbackService.isStartWhenPrepared()) {
                        updatePlayButtonAppearance(pauseResource, pauseText);
                    } else {
                        updatePlayButtonAppearance(playResource, playText);
                    }
                }
                break;
            case STOPPED:
                postStatusMsg(R.string.player_stopped_msg);
                break;
            case PREPARED:
                checkMediaInfoLoaded();
                postStatusMsg(R.string.player_ready_msg);
                updatePlayButtonAppearance(playResource, playText);
                break;
            case SEEKING:
                postStatusMsg(R.string.player_seeking_msg);
                break;
            case INITIALIZED:
                checkMediaInfoLoaded();
                clearStatusMsg();
                updatePlayButtonAppearance(playResource, playText);
                break;
        }
    }

    private void checkMediaInfoLoaded() {
        mediaInfoLoaded = (mediaInfoLoaded || loadMediaInfo());
    }

    private void updatePlayButtonAppearance(int resource, CharSequence contentDescription) {
        ImageButton butPlay = getPlayButton();
        butPlay.setImageResource(resource);
        butPlay.setContentDescription(contentDescription);
    }

    public abstract ImageButton getPlayButton();

    public abstract void postStatusMsg(int msg);

    public abstract void clearStatusMsg();

    public abstract boolean loadMediaInfo();

    public abstract void onAwaitingVideoSurface();

    /**
     * Called when connection to playback service has been established or
     * information has to be refreshed
     */
    void queryService() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Querying service info");
        if (playbackService != null) {
            status = playbackService.getStatus();
            media = playbackService.getPlayable();
            /*
            if (media == null) {
                Log.w(TAG,
                        "PlaybackService has no media object. Trying to restore last played media.");
                Intent serviceIntent = getPlayLastPlayedMediaIntent();
                if (serviceIntent != null) {
                    activity.startService(serviceIntent);
                }
            }
            */
            onServiceQueried();

            setupGUI();
            handleStatus();
            // make sure that new media is loaded if it's available
            mediaInfoLoaded = false;

        } else {
            Log.e(TAG,
                    "queryService() was called without an existing connection to playbackservice");
        }
    }

    public abstract void onServiceQueried();

    /**
     * Should be used by classes which implement the OnSeekBarChanged interface.
     */
    public float onSeekBarProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser, TextView txtvPosition) {
        if (fromUser && playbackService != null && media != null) {
            float prog = progress / ((float) seekBar.getMax());
            int duration = media.getDuration();
            txtvPosition.setText(Converter
                    .getDurationStringLong((int) (prog * duration)));
            return prog;
        }
        return 0;

    }

    /**
     * Should be used by classes which implement the OnSeekBarChanged interface.
     */
    public void onSeekBarStartTrackingTouch(SeekBar seekBar) {
        // interrupt position Observer, restart later
        cancelPositionObserver();
    }

    /**
     * Should be used by classes which implement the OnSeekBarChanged interface.
     */
    public void onSeekBarStopTrackingTouch(SeekBar seekBar, float prog) {
        if (playbackService != null) {
            playbackService.seekTo((int) (prog * media.getDuration()));
            setupPositionObserver();
        }
    }

    /**
     * Should be implemented by classes that show a video. The default implementation
     * does nothing
     *
     * @param enable True if the screen should be kept on, false otherwise
     */
    protected void setScreenOn(boolean enable) {

    }

    public OnClickListener newOnPlayButtonClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playbackService != null) {
                    switch (status) {
                        case PLAYING:
                            playbackService.pause(true, reinitOnPause);
                            break;
                        case PAUSED:
                        case PREPARED:
                            playbackService.resume();
                            break;
                        case PREPARING:
                            playbackService.setStartWhenPrepared(!playbackService
                                    .isStartWhenPrepared());
                            if (reinitOnPause
                                    && playbackService.isStartWhenPrepared() == false) {
                                playbackService.reinit();
                            }
                            break;
                        case INITIALIZED:
                            playbackService.setStartWhenPrepared(true);
                            playbackService.prepare();
                            break;
                    }
                } else {
                    Log.w(TAG,
                            "Play/Pause button was pressed, but playbackservice was null!");
                }
            }

        };
    }

    public OnClickListener newOnRevButtonClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status == PlayerStatus.PLAYING) {
                    playbackService.seekDelta(-UserPreferences.getSeekDeltaMs());
                }
            }
        };
    }

    public OnClickListener newOnFFButtonClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status == PlayerStatus.PLAYING) {
                    playbackService.seekDelta(UserPreferences.getSeekDeltaMs());
                }
            }
        };
    }

    public boolean serviceAvailable() {
        return playbackService != null;
    }

    public int getPosition() {
        if (playbackService != null) {
            return playbackService.getCurrentPosition();
        } else {
            return PlaybackService.INVALID_TIME;
        }
    }

    public int getDuration() {
        if (playbackService != null) {
            return playbackService.getDuration();
        } else {
            return PlaybackService.INVALID_TIME;
        }
    }

    public Playable getMedia() {
        return media;
    }

    public boolean sleepTimerActive() {
        return playbackService != null && playbackService.sleepTimerActive();
    }

    public boolean sleepTimerNotActive() {
        return playbackService != null && !playbackService.sleepTimerActive();
    }

    public void disableSleepTimer() {
        if (playbackService != null) {
            playbackService.disableSleepTimer();
        }
    }

    public long getSleepTimerTimeLeft() {
        if (playbackService != null) {
            return playbackService.getSleepTimerTimeLeft();
        } else {
            return INVALID_TIME;
        }
    }

    public void setSleepTimer(long time) {
        if (playbackService != null) {
            playbackService.setSleepTimer(time);
        }
    }

    public void seekToChapter(Chapter chapter) {
        if (playbackService != null) {
            playbackService.seekToChapter(chapter);
        }
    }

    public void seekTo(int time) {
        if (playbackService != null) {
            playbackService.seekTo(time);
        }
    }

    public void setVideoSurface(SurfaceHolder holder) {
        if (playbackService != null) {
            playbackService.setVideoSurface(holder);
        }
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public boolean canSetPlaybackSpeed() {
        return playbackService != null && playbackService.canSetSpeed();
    }

    public void setPlaybackSpeed(float speed) {
        if (playbackService != null) {
            playbackService.setSpeed(speed);
        }
    }

    public float getCurrentPlaybackSpeedMultiplier() {
        if (canSetPlaybackSpeed()) {
            return playbackService.getCurrentPlaybackSpeed();
        } else {
            return -1;
        }
    }

    public boolean isPlayingVideo() {
        if (playbackService != null) {
            return PlaybackService.getCurrentMediaType() == MediaType.VIDEO;
        }
        return false;
    }

    public Pair<Integer, Integer> getVideoSize() {
        if (playbackService != null) {
            return playbackService.getVideoSize();
        } else {
            return null;
        }
    }


    /**
     * Returns true if PlaybackController can communicate with the playback
     * service.
     */
    public boolean isConnectedToPlaybackService() {
        return playbackService != null;
    }

    public void notifyVideoSurfaceAbandoned() {
        if (playbackService != null) {
            playbackService.notifyVideoSurfaceAbandoned();
        }
    }

    /**
     * Move service into INITIALIZED state if it's paused to save bandwidth
     */
    public void reinitServiceIfPaused() {
        if (playbackService != null
                && playbackService.isStreaming()
                && (playbackService.getStatus() == PlayerStatus.PAUSED || (playbackService
                .getStatus() == PlayerStatus.PREPARING && playbackService
                .isStartWhenPrepared() == false))) {
            playbackService.reinit();
        }
    }

    /**
     * Refreshes the current position of the media file that is playing.
     */
    public class MediaPositionObserver implements Runnable {

        public static final int WAITING_INTERVALL = 1000;

        @Override
        public void run() {
            if (playbackService != null && playbackService.getStatus() == PlayerStatus.PLAYING) {
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        onPositionObserverUpdate();
                    }
                });
            }
        }
    }
}
