package de.danoeh.antennapod.core.util.playback;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Communicates with the playback service. GUI classes should use this class to
 * control playback instead of communicating with the PlaybackService directly.
 */
public abstract class PlaybackController {

    private static final String TAG = "PlaybackController";

    private static final int INVALID_TIME = -1;

    private final Activity activity;

    private PlaybackService playbackService;
    private Playable media;
    private PlayerStatus status;

    private final ScheduledThreadPoolExecutor schedExecutor;
    private static final int SCHED_EX_POOLSIZE = 1;

    private MediaPositionObserver positionObserver;
    private ScheduledFuture<?> positionObserverFuture;

    private boolean mediaInfoLoaded = false;
    private boolean released = false;

    private Subscription serviceBinder;

    /**
     * True if controller should reinit playback service if 'pause' button is
     * pressed.
     */
    private final boolean reinitOnPause;

    public PlaybackController(@NonNull Activity activity, boolean reinitOnPause) {

        this.activity = activity;
        this.reinitOnPause = reinitOnPause;
        schedExecutor = new ScheduledThreadPoolExecutor(SCHED_EX_POOLSIZE,
                r -> {
                    Thread t = new Thread(r);
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                }, new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r,
                                          ThreadPoolExecutor executor) {
                Log.w(TAG, "Rejected execution of runnable in schedExecutor");
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
            throw new IllegalStateException("Can't call init() after release() has been called");
        }
        checkMediaInfoLoaded();
    }

    /**
     * Should be called if the PlaybackController is no longer needed, for
     * example in the activity's onStop() method.
     */
    public void release() {
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

        if(serviceBinder != null) {
            serviceBinder.unsubscribe();
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
        Log.d(TAG, "Trying to connect to service");
        if(serviceBinder != null) {
            serviceBinder.unsubscribe();
        }
        serviceBinder = Observable.fromCallable(this::getPlayLastPlayedMediaIntent)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(intent -> {
                    boolean bound = false;
                    if (!PlaybackService.started) {
                        if (intent != null) {
                            Log.d(TAG, "Calling start service");
                            activity.startService(intent);
                            bound = activity.bindService(intent, mConnection, 0);
                        } else {
                            status = PlayerStatus.STOPPED;
                            setupGUI();
                            handleStatus();
                        }
                    } else {
                        Log.d(TAG, "PlaybackService is running, trying to connect without start command.");
                        bound = activity.bindService(new Intent(activity, PlaybackService.class),
                                mConnection, 0);
                    }
                    Log.d(TAG, "Result for service binding: " + bound);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    /**
     * Returns an intent that starts the PlaybackService and plays the last
     * played media or null if no last played media could be found.
     */
    private Intent getPlayLastPlayedMediaIntent() {
        Log.d(TAG, "Trying to restore last played media");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        long currentlyPlayingMedia = PlaybackPreferences.getCurrentlyPlayingMedia();
        if (currentlyPlayingMedia != PlaybackPreferences.NO_MEDIA_PLAYING) {
            Playable media = PlayableUtils.createInstanceFromPreferences(activity,
                    (int) currentlyPlayingMedia, prefs);
            if (media != null) {
                Intent serviceIntent = new Intent(activity, PlaybackService.class);
                serviceIntent.putExtra(PlaybackService.EXTRA_PLAYABLE, media);
                serviceIntent.putExtra(PlaybackService.EXTRA_START_WHEN_PREPARED, false);
                serviceIntent.putExtra(PlaybackService.EXTRA_PREPARE_IMMEDIATELY, true);
                boolean fileExists = media.localFileAvailable();
                boolean lastIsStream = PlaybackPreferences.getCurrentEpisodeIsStream();
                if (!fileExists && !lastIsStream && media instanceof FeedMedia) {
                    DBTasks.notifyMissingFeedMediaFile(activity, (FeedMedia) media);
                }
                serviceIntent.putExtra(PlaybackService.EXTRA_SHOULD_STREAM,
                        lastIsStream || !fileExists);
                return serviceIntent;
            }
        }
        Log.d(TAG, "No last played media found");
        return null;
    }



    private void setupPositionObserver() {
        if (positionObserverFuture == null ||
                positionObserverFuture.isCancelled() ||
                positionObserverFuture.isDone()) {

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
            Log.d(TAG, "PositionObserver cancelled. Result: " + result);
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if(service instanceof PlaybackService.LocalBinder) {
                playbackService = ((PlaybackService.LocalBinder) service).getService();
                if (!released) {
                    queryService();
                    Log.d(TAG, "Connection to Service established");
                } else {
                    Log.i(TAG, "Connection to playback service has been established, " +
                            "but controller has already been released");
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
            Log.d(TAG, "Disconnected from Service");
        }
    };

    private final BroadcastReceiver statusUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received statusUpdate Intent.");
            if (isConnectedToPlaybackService()) {
                PlaybackServiceMediaPlayer.PSMPInfo info = playbackService.getPSMPInfo();
                status = info.playerStatus;
                media = info.playable;
                handleStatus();
            } else {
                Log.w(TAG, "Couldn't receive status update: playbackService was null");
                bindToService();
            }
        }
    };

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isConnectedToPlaybackService()) {
                bindToService();
                return;
            }
                int type = intent.getIntExtra(PlaybackService.EXTRA_NOTIFICATION_TYPE, -1);
                int code = intent.getIntExtra(PlaybackService.EXTRA_NOTIFICATION_CODE, -1);
            if(code == -1 || type == -1) {
                Log.d(TAG, "Bad arguments. Won't handle intent");
                return;
            }
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
                case PlaybackService.NOTIFICATION_TYPE_SET_SPEED_ABILITY_CHANGED:
                    onSetSpeedAbilityChanged();
                    break;
                case PlaybackService.NOTIFICATION_TYPE_SHOW_TOAST:
                    postStatusMsg(code, true);
            }
        }

    };

    private final BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isConnectedToPlaybackService()) {
                if (TextUtils.equals(intent.getAction(),
                        PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE)) {
                    release();
                    onShutdownNotification();
                }
            }
        }
    };

    public void setupGUI() {}

    public void onPositionObserverUpdate() {}


    public void onPlaybackSpeedChange() {}

    public void onSetSpeedAbilityChanged() {}

    public void onShutdownNotification() {}

    /**
     * Called when the currently displayed information should be refreshed.
     */
    public void onReloadNotification(int code) {}

    public void onBufferStart() {}

    public void onBufferEnd() {}

    public void onBufferUpdate(float progress) {}

    public void onSleepTimerUpdate() {}

    public void handleError(int code) {}

    public void onPlaybackEnd() {}

    public void repeatHandleStatus() {
        if (status != null && playbackService != null) {
            handleStatus();
        }
    }

    /**
     * Is called whenever the PlaybackService changes its status. This method
     * should be used to update the GUI or start/cancel background threads.
     */
    private void handleStatus() {
        final int playResource;
        final int pauseResource;
        final CharSequence playText = activity.getString(R.string.play_label);
        final CharSequence pauseText = activity.getString(R.string.pause_label);

        if (PlaybackService.getCurrentMediaType() == MediaType.AUDIO ||
                PlaybackService.isCasting()) {
            TypedArray res = activity.obtainStyledAttributes(new int[]{
                    R.attr.av_play_big, R.attr.av_pause_big});
            playResource = res.getResourceId(0, R.drawable.ic_play_arrow_grey600_36dp);
            pauseResource = res.getResourceId(1, R.drawable.ic_pause_grey600_36dp);
            res.recycle();
        } else {
            playResource = R.drawable.ic_av_play_circle_outline_80dp;
            pauseResource = R.drawable.ic_av_pause_circle_outline_80dp;
        }

        Log.d(TAG, "status: " + status.toString());
        switch (status) {
            case ERROR:
                postStatusMsg(R.string.player_error_msg, false);
                handleError(MediaPlayer.MEDIA_ERROR_UNKNOWN);
                break;
            case PAUSED:
                clearStatusMsg();
                checkMediaInfoLoaded();
                cancelPositionObserver();
                onPositionObserverUpdate();
                updatePlayButtonAppearance(playResource, playText);
                if (!PlaybackService.isCasting() &&
                        PlaybackService.getCurrentMediaType() == MediaType.VIDEO) {
                    setScreenOn(false);
                }
                break;
            case PLAYING:
                clearStatusMsg();
                checkMediaInfoLoaded();
                if (!PlaybackService.isCasting() &&
                        PlaybackService.getCurrentMediaType() == MediaType.VIDEO) {
                    onAwaitingVideoSurface();
                    setScreenOn(true);
                }
                setupPositionObserver();
                updatePlayButtonAppearance(pauseResource, pauseText);
                break;
            case PREPARING:
                postStatusMsg(R.string.player_preparing_msg, false);
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
                postStatusMsg(R.string.player_stopped_msg, false);
                break;
            case PREPARED:
                checkMediaInfoLoaded();
                postStatusMsg(R.string.player_ready_msg, false);
                updatePlayButtonAppearance(playResource, playText);
                onPositionObserverUpdate();
                break;
            case SEEKING:
                onPositionObserverUpdate();
                postStatusMsg(R.string.player_seeking_msg, false);
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
        if(butPlay != null) {
            butPlay.setImageResource(resource);
            butPlay.setContentDescription(contentDescription);
        }
    }

    public ImageButton getPlayButton() {
        return null;
    }

    public void postStatusMsg(int msg, boolean showToast) {}

    public void clearStatusMsg() {}

    public boolean loadMediaInfo() {
        return false;
    }

    public  void onAwaitingVideoSurface()  {}

    /**
     * Called when connection to playback service has been established or
     * information has to be refreshed
     */
    private void queryService() {
        Log.d(TAG, "Querying service info");
        if (playbackService != null) {
            PlaybackServiceMediaPlayer.PSMPInfo info = playbackService.getPSMPInfo();
            status = info.playerStatus;
            media = info.playable;
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

    public void onServiceQueried()  {}

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
        if (playbackService != null && media != null) {
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

    public void playPause() {
        if (playbackService == null) {
            Log.w(TAG, "Play/Pause button was pressed, but playbackservice was null!");
            return;
        }
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
                        && !playbackService.isStartWhenPrepared()) {
                    playbackService.reinit();
                }
                break;
            case INITIALIZED:
                playbackService.setStartWhenPrepared(true);
                playbackService.prepare();
                break;
        }
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

    public void setSleepTimer(long time, boolean shakeToReset, boolean vibrate) {
        if (playbackService != null) {
            playbackService.setSleepTimer(time, shakeToReset, vibrate);
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
        return org.antennapod.audio.MediaPlayer.isPrestoLibraryInstalled(activity.getApplicationContext())
                || UserPreferences.useSonic()
                || Build.VERSION.SDK_INT >= 23
                || playbackService != null && playbackService.canSetSpeed();
    }

    public void setPlaybackSpeed(float speed) {
        if (playbackService != null) {
            playbackService.setSpeed(speed);
        }
    }

    public void setVolume(float leftVolume, float rightVolume) {
        if (playbackService != null) {
            playbackService.setVolume(leftVolume, rightVolume);
        }
    }

    public float getCurrentPlaybackSpeedMultiplier() {
        if (canSetPlaybackSpeed()) {
            return playbackService.getCurrentPlaybackSpeed();
        } else {
            return -1;
        }
    }

    public boolean canDownmix() {
        return playbackService != null && playbackService.canDownmix();
    }

    public void setDownmix(boolean enable) {
        if(playbackService != null) {
            playbackService.setDownmix(enable);
        }
    }

    public boolean isPlayingVideoLocally() {
        return playbackService != null && PlaybackService.getCurrentMediaType() == MediaType.VIDEO
                && !PlaybackService.isCasting();
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
    private boolean isConnectedToPlaybackService() {
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
                && !PlaybackService.isCasting()
                && (playbackService.getStatus() == PlayerStatus.PAUSED ||
                (playbackService.getStatus() == PlayerStatus.PREPARING &&
                        !playbackService.isStartWhenPrepared()))) {
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
                activity.runOnUiThread(PlaybackController.this::onPositionObserverUpdate);
            }
        }
    }
}
