package de.danoeh.antennapod.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AudioplayerActivity;
import de.danoeh.antennapod.activity.VideoplayerActivity;
import de.danoeh.antennapod.feed.*;
import de.danoeh.antennapod.preferences.PlaybackPreferences;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.receiver.PlayerWidget;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.util.BitmapDecoder;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.DuckType;
import de.danoeh.antennapod.util.flattr.FlattrUtils;
import de.danoeh.antennapod.util.playback.AudioPlayer;
import de.danoeh.antennapod.util.playback.IPlayer;
import de.danoeh.antennapod.util.playback.Playable;
import de.danoeh.antennapod.util.playback.Playable.PlayableException;
import de.danoeh.antennapod.util.playback.VideoPlayer;
import de.danoeh.antennapod.util.playback.PlaybackController;

/**
 * Controls the MediaPlayer that plays a FeedMedia-file
 */
public class PlaybackService extends Service {
    /**
     * Logging tag
     */
    private static final String TAG = "PlaybackService";

    /**
     * Parcelable of type Playable.
     */
    public static final String EXTRA_PLAYABLE = "PlaybackService.PlayableExtra";
    /**
     * True if media should be streamed.
     */
    public static final String EXTRA_SHOULD_STREAM = "extra.de.danoeh.antennapod.service.shouldStream";
    /**
     * True if playback should be started immediately after media has been
     * prepared.
     */
    public static final String EXTRA_START_WHEN_PREPARED = "extra.de.danoeh.antennapod.service.startWhenPrepared";

    public static final String EXTRA_PREPARE_IMMEDIATELY = "extra.de.danoeh.antennapod.service.prepareImmediately";

    public static final String ACTION_PLAYER_STATUS_CHANGED = "action.de.danoeh.antennapod.service.playerStatusChanged";
    private static final String AVRCP_ACTION_PLAYER_STATUS_CHANGED = "com.android.music.playstatechanged";

    public static final String ACTION_PLAYER_NOTIFICATION = "action.de.danoeh.antennapod.service.playerNotification";
    public static final String EXTRA_NOTIFICATION_CODE = "extra.de.danoeh.antennapod.service.notificationCode";
    public static final String EXTRA_NOTIFICATION_TYPE = "extra.de.danoeh.antennapod.service.notificationType";

    /**
     * If the PlaybackService receives this action, it will stop playback and
     * try to shutdown.
     */
    public static final String ACTION_SHUTDOWN_PLAYBACK_SERVICE = "action.de.danoeh.antennapod.service.actionShutdownPlaybackService";

    /**
     * If the PlaybackService receives this action, it will end playback of the
     * current episode and load the next episode if there is one available.
     */
    public static final String ACTION_SKIP_CURRENT_EPISODE = "action.de.danoeh.antennapod.service.skipCurrentEpisode";

    /**
     * Used in NOTIFICATION_TYPE_RELOAD.
     */
    public static final int EXTRA_CODE_AUDIO = 1;
    public static final int EXTRA_CODE_VIDEO = 2;

    public static final int NOTIFICATION_TYPE_ERROR = 0;
    public static final int NOTIFICATION_TYPE_INFO = 1;
    public static final int NOTIFICATION_TYPE_BUFFER_UPDATE = 2;

    /**
     * Receivers of this intent should update their information about the curently playing media
     */
    public static final int NOTIFICATION_TYPE_RELOAD = 3;
    /**
     * The state of the sleeptimer changed.
     */
    public static final int NOTIFICATION_TYPE_SLEEPTIMER_UPDATE = 4;
    public static final int NOTIFICATION_TYPE_BUFFER_START = 5;
    public static final int NOTIFICATION_TYPE_BUFFER_END = 6;
    /**
     * No more episodes are going to be played.
     */
    public static final int NOTIFICATION_TYPE_PLAYBACK_END = 7;

    /** 
     * Playback speed has changed
     * */
    public static final int NOTIFICATION_TYPE_PLAYBACK_SPEED_CHANGE = 8;
    
     /**
     * Returned by getPositionSafe() or getDurationSafe() if the playbackService
     * is in an invalid state.
     */
    public static final int INVALID_TIME = -1;

    /**
     * Is true if service is running.
     */
    public static boolean isRunning = false;

    private static final int NOTIFICATION_ID = 1;

	private volatile IPlayer player;
	private RemoteControlClient remoteControlClient;
    private AudioManager audioManager;
    private ComponentName mediaButtonReceiver;

    private volatile Playable media;

    /**
     * True if media should be streamed (Extracted from Intent Extra) .
     */
    private boolean shouldStream;

    private boolean startWhenPrepared;
    private PlayerStatus status;

    private PositionSaver positionSaver;
    private ScheduledFuture positionSaverFuture;

    private WidgetUpdateWorker widgetUpdater;
    private ScheduledFuture widgetUpdaterFuture;

    private SleepTimer sleepTimer;
    private Future sleepTimerFuture;

    private static final int SCHED_EX_POOL_SIZE = 3;
    private ScheduledThreadPoolExecutor schedExecutor;
    private ExecutorService dbLoaderExecutor;

    private volatile PlayerStatus statusBeforeSeek;

    private static boolean playingVideo;

    /**
     * True if mediaplayer was paused because it lost audio focus temporarily
     */
    private boolean pausedBecauseOfTransientAudiofocusLoss;

    private Thread chapterLoader;

    private final IBinder mBinder = new LocalBinder();

    private volatile List<FeedItem> queue;

    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Received onUnbind event");
        return super.onUnbind(intent);
    }

    /**
     * Returns an intent which starts an audio- or videoplayer, depending on the
     * type of media that is being played. If the playbackservice is not
     * running, the type of the last played media will be looked up.
     */
    public static Intent getPlayerActivityIntent(Context context) {
        if (isRunning) {
            if (playingVideo) {
                return new Intent(context, VideoplayerActivity.class);
            } else {
                return new Intent(context, AudioplayerActivity.class);
            }
        } else {
            if (PlaybackPreferences.getCurrentEpisodeIsVideo()) {
                return new Intent(context, VideoplayerActivity.class);
            } else {
                return new Intent(context, AudioplayerActivity.class);
            }
        }
    }

    /**
     * Same as getPlayerActivityIntent(context), but here the type of activity
     * depends on the FeedMedia that is provided as an argument.
     */
    public static Intent getPlayerActivityIntent(Context context, Playable media) {
        MediaType mt = media.getMediaType();
        if (mt == MediaType.VIDEO) {
            return new Intent(context, VideoplayerActivity.class);
        } else {
            return new Intent(context, AudioplayerActivity.class);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        super.onCreate();
        if (AppConfig.DEBUG)
            Log.d(TAG, "Service created.");
        isRunning = true;
        pausedBecauseOfTransientAudiofocusLoss = false;
        status = PlayerStatus.STOPPED;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        schedExecutor = new ScheduledThreadPoolExecutor(SCHED_EX_POOL_SIZE,
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
                Log.w(TAG, "SchedEx rejected submission of new task");
            }
        }
        );
        dbLoaderExecutor = Executors.newSingleThreadExecutor();

        mediaButtonReceiver = new ComponentName(getPackageName(),
                MediaButtonReceiver.class.getName());
        audioManager.registerMediaButtonEventReceiver(mediaButtonReceiver);
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            audioManager
                    .registerRemoteControlClient(setupRemoteControlClient());
        }
        registerReceiver(headsetDisconnected, new IntentFilter(
                Intent.ACTION_HEADSET_PLUG));
        registerReceiver(shutdownReceiver, new IntentFilter(
                ACTION_SHUTDOWN_PLAYBACK_SERVICE));
        registerReceiver(audioBecomingNoisy, new IntentFilter(
                AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(skipCurrentEpisodeReceiver, new IntentFilter(
                ACTION_SKIP_CURRENT_EPISODE));
        EventDistributor.getInstance().register(eventDistributorListener);
        loadQueue();
    }

    private IPlayer createMediaPlayer() {
        IPlayer player;
        if (media == null || media.getMediaType() == MediaType.VIDEO) {
            player = new VideoPlayer();
        } else {
            player = new AudioPlayer(this);
        }
        return createMediaPlayer(player);
    }

	private IPlayer createMediaPlayer(IPlayer mp) {
		if (mp != null && media != null) {
			if (media.getMediaType() == MediaType.AUDIO) {
				((AudioPlayer) mp).setOnPreparedListener(audioPreparedListener);
				((AudioPlayer) mp)
						.setOnCompletionListener(audioCompletionListener);
				((AudioPlayer) mp)
						.setOnSeekCompleteListener(audioSeekCompleteListener);
				((AudioPlayer) mp).setOnErrorListener(audioErrorListener);
				((AudioPlayer) mp)
						.setOnBufferingUpdateListener(audioBufferingUpdateListener);
				((AudioPlayer) mp).setOnInfoListener(audioInfoListener);
			} else {
				((VideoPlayer) mp).setOnPreparedListener(videoPreparedListener);
				((VideoPlayer) mp)
						.setOnCompletionListener(videoCompletionListener);
				((VideoPlayer) mp)
						.setOnSeekCompleteListener(videoSeekCompleteListener);
				((VideoPlayer) mp).setOnErrorListener(videoErrorListener);
				((VideoPlayer) mp)
						.setOnBufferingUpdateListener(videoBufferingUpdateListener);
				((VideoPlayer) mp).setOnInfoListener(videoInfoListener);
			}
		}
		return mp;
	}

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (AppConfig.DEBUG)
            Log.d(TAG, "Service is about to be destroyed");
        isRunning = false;
        if (chapterLoader != null) {
            chapterLoader.interrupt();
        }
        disableSleepTimer();
        unregisterReceiver(headsetDisconnected);
        unregisterReceiver(shutdownReceiver);
        unregisterReceiver(audioBecomingNoisy);
        unregisterReceiver(skipCurrentEpisodeReceiver);
        EventDistributor.getInstance().unregister(eventDistributorListener);
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            audioManager.unregisterRemoteControlClient(remoteControlClient);
        }
        audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiver);
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        player.release();
        stopWidgetUpdater();
        updateWidget();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Received onBind event");
        return mBinder;
    }

    private final EventDistributor.EventListener eventDistributorListener = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((EventDistributor.QUEUE_UPDATE & arg) != 0) {
                loadQueue();
            }
        }
    };

    private final OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (AppConfig.DEBUG)
                        Log.d(TAG, "Lost audio focus");
                    pause(true, false);
                    stopSelf();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (AppConfig.DEBUG)
                        Log.d(TAG, "Gained audio focus");
                    if (pausedBecauseOfTransientAudiofocusLoss) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_RAISE, 0);
                        play();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (status == PlayerStatus.PLAYING) {
                        if (!UserPreferences.shouldPauseForFocusLoss()) {
                            if (AppConfig.DEBUG)
                                Log.d(TAG, "Lost audio focus temporarily. Ducking...");
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_LOWER, 0);
                            pausedBecauseOfTransientAudiofocusLoss = true;
                        } else {
                            if (AppConfig.DEBUG)
                                Log.d(TAG, "Lost audio focus temporarily. Could duck, but won't, pausing...");
                            pause(false, false);
                            pausedBecauseOfTransientAudiofocusLoss = true;
                        }
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (status == PlayerStatus.PLAYING) {
                        if (AppConfig.DEBUG)
                            Log.d(TAG, "Lost audio focus temporarily. Pausing...");
                        pause(false, false);
                        pausedBecauseOfTransientAudiofocusLoss = true;
                    }
            }
        }
    };

    /**
     * 1. Check type of intent
     * 1.1 Keycode -> handle keycode -> done
     * 1.2 Playable -> Step 2
     * 2. Handle playable
     * 2.1 Check current status
     * 2.1.1 Not playing -> play new playable
     * 2.1.2 Playing, new playable is the same -> play if playback is currently paused
     * 2.1.3 Playing, new playable different -> Stop playback of old media
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (AppConfig.DEBUG)
            Log.d(TAG, "OnStartCommand called");
        final int keycode = intent.getIntExtra(MediaButtonReceiver.EXTRA_KEYCODE, -1);
        final Playable playable = intent.getParcelableExtra(EXTRA_PLAYABLE);
        if (keycode == -1 && playable == null) {
            Log.e(TAG, "PlaybackService was started with no arguments");
            stopSelf();
        }

        if (keycode != -1) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Received media button event");
            handleKeycode(keycode);
        } else {
            boolean playbackType = intent.getBooleanExtra(EXTRA_SHOULD_STREAM,
                    true);
            if (media == null) {
                media = playable;
                shouldStream = playbackType;
                startWhenPrepared = intent.getBooleanExtra(
                        EXTRA_START_WHEN_PREPARED, false);
                initMediaplayer(intent.getBooleanExtra(EXTRA_PREPARE_IMMEDIATELY, false));
                sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
            }
            if (media != null) {
                if (!playable.getIdentifier().equals(media.getIdentifier())) {
                    // different media or different playback type
                    pause(true, false);
                    player.reset();
                    media = playable;
                    shouldStream = playbackType;
                    startWhenPrepared = intent.getBooleanExtra(EXTRA_START_WHEN_PREPARED, false);
                    initMediaplayer(intent.getBooleanExtra(EXTRA_PREPARE_IMMEDIATELY, false));
                    sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
                } else {
                    // same media and same playback type
                    if (status == PlayerStatus.PAUSED) {
                        play();
                    }
                }
            }
        }

        return Service.START_NOT_STICKY;
    }

	/** Handles media button events */
	private void handleKeycode(int keycode) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Handling keycode: " + keycode);
		switch (keycode) {
		case KeyEvent.KEYCODE_HEADSETHOOK:
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			if (status == PlayerStatus.PLAYING) {
				pause(true, true);
			} else if (status == PlayerStatus.PAUSED) {
				play();
			} else if (status == PlayerStatus.PREPARING) {
				setStartWhenPrepared(!startWhenPrepared);
			} else if (status == PlayerStatus.INITIALIZED) {
				startWhenPrepared = true;
				prepare();
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PLAY:
			if (status == PlayerStatus.PAUSED) {
				play();
			} else if (status == PlayerStatus.INITIALIZED) {
				startWhenPrepared = true;
				prepare();
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
			if (status == PlayerStatus.PLAYING) {
				pause(true, true);
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: {
			seekDelta(PlaybackController.DEFAULT_SEEK_DELTA);
			break;
		}
		case KeyEvent.KEYCODE_MEDIA_REWIND: {
			seekDelta(-PlaybackController.DEFAULT_SEEK_DELTA);
			break;
 		  }
		}
	}

    /**
     * Called by a mediaplayer Activity as soon as it has prepared its
     * mediaplayer.
     */
    public void setVideoSurface(SurfaceHolder sh) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Setting display");
        player.setDisplay(null);
        player.setDisplay(sh);
        if (status == PlayerStatus.STOPPED
                || status == PlayerStatus.AWAITING_VIDEO_SURFACE) {
            try {
                InitTask initTask = new InitTask() {

                    @Override
                    protected void onPostExecute(Playable result) {
                        if (status == PlayerStatus.INITIALIZING) {
                            if (result != null) {
                                try {
                                    if (shouldStream) {
                                        player.setDataSource(media
                                                .getStreamUrl());
                                        setStatus(PlayerStatus.PREPARING);
                                        player.prepareAsync();
                                    } else {
                                        player.setDataSource(media
                                                .getLocalMediaUrl());
                                        setStatus(PlayerStatus.PREPARING);
                                        player.prepareAsync();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                setStatus(PlayerStatus.ERROR);
                                sendBroadcast(new Intent(
                                        ACTION_SHUTDOWN_PLAYBACK_SERVICE));
                            }
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        setStatus(PlayerStatus.INITIALIZING);
                    }

                };
                initTask.executeAsync(media);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Called when the surface holder of the mediaplayer has to be changed.
     */
    private void resetVideoSurface() {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Resetting video surface");
        cancelPositionSaver();
        player.setDisplay(null);
        player.reset();
        player.release();
        player = createMediaPlayer();
        status = PlayerStatus.STOPPED;
    }

	public void notifyVideoSurfaceAbandoned() {
		resetVideoSurface();
        if (media != null) {
            initMediaplayer(true);
        }
	}

    /**
     * Called after service has extracted the media it is supposed to play.
     *
     * @param prepareImmediately True if service should prepare playback after it has been initialized
     */
    private void initMediaplayer(final boolean prepareImmediately) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Setting up media player");
        try {
            MediaType mediaType = media.getMediaType();
            player = createMediaPlayer();
            if (mediaType == MediaType.AUDIO) {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Mime type is audio");

                InitTask initTask = new InitTask() {

                    @Override
                    protected void onPostExecute(Playable result) {
                        // check if state of service has changed. If it has
                        // changed, assume that loaded metadata is not needed
                        // anymore.
                        if (status == PlayerStatus.INITIALIZING) {
                            if (result != null) {
                                playingVideo = false;
                                try {
                                    if (shouldStream) {
                                        player.setDataSource(media
                                                .getStreamUrl());
                                    } else if (media.localFileAvailable()) {
                                        player.setDataSource(media
                                                .getLocalMediaUrl());
                                    }

                                    if (prepareImmediately) {
                                        setStatus(PlayerStatus.PREPARING);
                                        player.prepareAsync();
                                    } else {
                                        setStatus(PlayerStatus.INITIALIZED);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    media = null;
                                    setStatus(PlayerStatus.ERROR);
                                    sendBroadcast(new Intent(
                                            ACTION_SHUTDOWN_PLAYBACK_SERVICE));
                                }
                            } else {
                                Log.e(TAG, "InitTask could not load metadata");
                                media = null;
                                setStatus(PlayerStatus.ERROR);
                                sendBroadcast(new Intent(
                                        ACTION_SHUTDOWN_PLAYBACK_SERVICE));
                            }
                        } else {
                            if (AppConfig.DEBUG)
                                Log.d(TAG,
                                        "Status of player has changed during initialization. Stopping init process.");
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        setStatus(PlayerStatus.INITIALIZING);
                    }

                };
                initTask.executeAsync(media);
            } else if (mediaType == MediaType.VIDEO) {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Mime type is video");
                playingVideo = true;
                setStatus(PlayerStatus.AWAITING_VIDEO_SURFACE);
                player.setScreenOnWhilePlaying(true);
            }

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

	private void setupPositionSaver() {
		if (positionSaverFuture == null
				|| (positionSaverFuture.isCancelled() || positionSaverFuture
						.isDone())) {

			positionSaver = new PositionSaver();
			positionSaverFuture = schedExecutor.scheduleAtFixedRate(
					positionSaver, PositionSaver.WAITING_INTERVALL,
					PositionSaver.WAITING_INTERVALL, TimeUnit.MILLISECONDS);
		}
	}

	private void cancelPositionSaver() {
		if (positionSaverFuture != null) {
			boolean result = positionSaverFuture.cancel(true);
			if (AppConfig.DEBUG)
				Log.d(TAG, "PositionSaver cancelled. Result: " + result);
		}
	}

    private final com.aocate.media.MediaPlayer.OnPreparedListener audioPreparedListener = new com.aocate.media.MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(com.aocate.media.MediaPlayer mp) {
            genericOnPrepared(mp);
        }
    };

    private final android.media.MediaPlayer.OnPreparedListener videoPreparedListener = new android.media.MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(android.media.MediaPlayer mp) {
            genericOnPrepared(mp);
        }
    };

    private final void genericOnPrepared(Object inObj) {
        IPlayer mp = DuckType.coerce(inObj).to(IPlayer.class);
        if (AppConfig.DEBUG)
            Log.d(TAG, "Resource prepared");
        mp.seekTo(media.getPosition());
        if (media.getDuration() == 0) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Setting duration of media");
            media.setDuration(mp.getDuration());
        }
        setStatus(PlayerStatus.PREPARED);
        if (chapterLoader != null) {
            chapterLoader.interrupt();
        }
        chapterLoader = new Thread() {
            @Override
            public void run() {
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Chapter loader started");
                if (media != null && media.getChapters() == null) {
                    media.loadChapterMarks();
                    if (!isInterrupted() && media.getChapters() != null) {
                        sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD,
                                0);
                    }
                }
                if (AppConfig.DEBUG)
                    Log.d(TAG, "Chapter loader stopped");
            }
        };
        chapterLoader.start();

        if (startWhenPrepared) {
            play();
        }
    }

    private final com.aocate.media.MediaPlayer.OnSeekCompleteListener audioSeekCompleteListener = new com.aocate.media.MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(com.aocate.media.MediaPlayer mp) {
            genericSeekCompleteListener();
        }
    };

    private final android.media.MediaPlayer.OnSeekCompleteListener videoSeekCompleteListener = new android.media.MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(android.media.MediaPlayer mp) {
            genericSeekCompleteListener();
        }
    };

    private final void genericSeekCompleteListener() {
        if (status == PlayerStatus.SEEKING) {
            setStatus(statusBeforeSeek);
        }
    }

    private final com.aocate.media.MediaPlayer.OnInfoListener audioInfoListener = new com.aocate.media.MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(com.aocate.media.MediaPlayer mp, int what,
                              int extra) {
            return genericInfoListener(what);
        }
    };

    private final android.media.MediaPlayer.OnInfoListener videoInfoListener = new android.media.MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(android.media.MediaPlayer mp, int what, int extra) {
            return genericInfoListener(what);
        }
    };

    private boolean genericInfoListener(int what) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_START, 0);
                return true;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_END, 0);
                return true;
            default:
                return false;
        }
    }

    private final com.aocate.media.MediaPlayer.OnErrorListener audioErrorListener = new com.aocate.media.MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(com.aocate.media.MediaPlayer mp, int what,
                               int extra) {
            return genericOnError(mp, what, extra);
        }
    };

    private final android.media.MediaPlayer.OnErrorListener videoErrorListener = new android.media.MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
            return genericOnError(mp, what, extra);
        }
    };

    private boolean genericOnError(Object inObj, int what, int extra) {
        final String TAG = "PlaybackService.onErrorListener";
        Log.w(TAG, "An error has occured: " + what + " " + extra);
        IPlayer mp = DuckType.coerce(inObj).to(IPlayer.class);
        if (mp.isPlaying()) {
            pause(true, true);
        }
        sendNotificationBroadcast(NOTIFICATION_TYPE_ERROR, what);
        setCurrentlyPlayingMedia(PlaybackPreferences.NO_MEDIA_PLAYING);
        stopSelf();
        return true;
    }

    private final com.aocate.media.MediaPlayer.OnCompletionListener audioCompletionListener = new com.aocate.media.MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(com.aocate.media.MediaPlayer mp) {
            genericOnCompletion();
        }
    };

    private final android.media.MediaPlayer.OnCompletionListener videoCompletionListener = new android.media.MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(android.media.MediaPlayer mp) {
            genericOnCompletion();
        }
    };

    private void genericOnCompletion() {
        endPlayback(true);
    }

    private final com.aocate.media.MediaPlayer.OnBufferingUpdateListener audioBufferingUpdateListener = new com.aocate.media.MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(com.aocate.media.MediaPlayer mp,
                                      int percent) {
            genericOnBufferingUpdate(percent);
        }
    };

    private final android.media.MediaPlayer.OnBufferingUpdateListener videoBufferingUpdateListener = new android.media.MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(android.media.MediaPlayer mp, int percent) {
            genericOnBufferingUpdate(percent);
        }
    };

    private void genericOnBufferingUpdate(int percent) {
        sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_UPDATE, percent);
    }

    private void endPlayback(boolean playNextEpisode) {
        if (AppConfig.DEBUG)
            Log.d(TAG, "Playback ended");
        audioManager.abandonAudioFocus(audioFocusChangeListener);

        // Save state
        cancelPositionSaver();

        boolean isInQueue = false;
        FeedItem nextItem = null;

        if (media instanceof FeedMedia) {
            FeedItem item = ((FeedMedia) media).getItem();
            DBWriter.markItemRead(PlaybackService.this, item, true, true);
            nextItem = DBTasks.getQueueSuccessorOfItem(this, item.getId(), queue);
            isInQueue = media instanceof FeedMedia
                    && QueueAccess.ItemListAccess(queue).contains(((FeedMedia) media).getItem().getId());
            if (isInQueue) {
                DBWriter.removeQueueItem(PlaybackService.this, item.getId(), true);
            }
            DBWriter.addItemToPlaybackHistory(PlaybackService.this, (FeedMedia) media);
            long autoDeleteMediaId = ((FeedComponent) media).getId();
            if (shouldStream) {
                autoDeleteMediaId = -1;
            }
        }

        // Load next episode if previous episode was in the queue and if there
        // is an episode in the queue left.
        // Start playback immediately if continuous playback is enabled
        boolean loadNextItem = isInQueue && nextItem != null;
        playNextEpisode = playNextEpisode && loadNextItem
                && UserPreferences.isFollowQueue();
        if (loadNextItem) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Loading next item in queue");
            media = nextItem.getMedia();
        }
        final boolean prepareImmediately;
        if (playNextEpisode) {
            if (AppConfig.DEBUG)
                Log.d(TAG, "Playback of next episode will start immediately.");
            prepareImmediately = startWhenPrepared = true;
        } else {
            if (AppConfig.DEBUG)
                Log.d(TAG, "No more episodes available to play");
            media = null;
            prepareImmediately = startWhenPrepared = false;
            stopForeground(true);
            stopWidgetUpdater();
        }

        int notificationCode = 0;
        if (media != null) {
            shouldStream = !media.localFileAvailable();
            if (media.getMediaType() == MediaType.AUDIO) {
                notificationCode = EXTRA_CODE_AUDIO;
                playingVideo = false;
            } else if (media.getMediaType() == MediaType.VIDEO) {
                notificationCode = EXTRA_CODE_VIDEO;
            }
        }
        writePlaybackPreferences();
        if (media != null) {
            resetVideoSurface();
            refreshRemoteControlClientState();
            initMediaplayer(prepareImmediately);

            sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD,
                    notificationCode);
        } else {
            sendNotificationBroadcast(NOTIFICATION_TYPE_PLAYBACK_END, 0);
            stopSelf();
        }
    }

	public void setSleepTimer(long waitingTime) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Setting sleep timer to " + Long.toString(waitingTime)
					+ " milliseconds");
		if (sleepTimerFuture != null) {
			sleepTimerFuture.cancel(true);
		}
		sleepTimer = new SleepTimer(waitingTime);
		sleepTimerFuture = schedExecutor.submit(sleepTimer);
		sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
	}

	public void disableSleepTimer() {
		if (sleepTimerFuture != null) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Disabling sleep timer");
			sleepTimerFuture.cancel(true);
			sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
		}
	}

	/**
	 * Saves the current position and pauses playback. Note that, if audiofocus
	 * is abandoned, the lockscreen controls will also disapear.
	 *
	 * @param abandonFocus
	 *            is true if the service should release audio focus
	 * @param reinit
	 *            is true if service should reinit after pausing if the media
	 *            file is being streamed
	 */
	public void pause(boolean abandonFocus, boolean reinit) {
		if (player.isPlaying()) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Pausing playback.");
			player.pause();
			cancelPositionSaver();
			saveCurrentPosition();
			setStatus(PlayerStatus.PAUSED);
			if (abandonFocus) {
				audioManager.abandonAudioFocus(audioFocusChangeListener);
				pausedBecauseOfTransientAudiofocusLoss = false;
				disableSleepTimer();
			}
			stopWidgetUpdater();
			stopForeground(true);
			if (shouldStream && reinit) {
				reinit();
			}
		}
	}

	/** Pauses playback and destroys service. Recommended for video playback. */
	public void stop() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Stopping playback");
		if (status == PlayerStatus.PREPARED || status == PlayerStatus.PAUSED
				|| status == PlayerStatus.STOPPED
				|| status == PlayerStatus.PLAYING) {
			player.stop();
		}
		setCurrentlyPlayingMedia(PlaybackPreferences.NO_MEDIA_PLAYING);
		stopSelf();
	}

	/**
	 * Prepared media player for playback if the service is in the INITALIZED
	 * state.
	 */
	public void prepare() {
		if (status == PlayerStatus.INITIALIZED) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Preparing media player");
			setStatus(PlayerStatus.PREPARING);
			player.prepareAsync();
		}
	}

	/** Resets the media player and moves into INITIALIZED state. */
	public void reinit() {
		player.reset();
		player = createMediaPlayer(player);
		initMediaplayer(false);
	}

	@SuppressLint("NewApi")
	public void play() {
		if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED
				|| status == PlayerStatus.STOPPED) {
			int focusGained = audioManager.requestAudioFocus(
					audioFocusChangeListener, AudioManager.STREAM_MUSIC,
					AudioManager.AUDIOFOCUS_GAIN);

			if (focusGained == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Audiofocus successfully requested");
				if (AppConfig.DEBUG)
					Log.d(TAG, "Resuming/Starting playback");
				writePlaybackPreferences();

				setSpeed(Float.parseFloat(UserPreferences.getPlaybackSpeed()));
				player.start();
				if (status != PlayerStatus.PAUSED) {
					player.seekTo((int) media.getPosition());
				}
				setStatus(PlayerStatus.PLAYING);
				setupPositionSaver();
				setupWidgetUpdater();
				setupNotification();
				pausedBecauseOfTransientAudiofocusLoss = false;
				if (android.os.Build.VERSION.SDK_INT >= 14) {
					audioManager
							.registerRemoteControlClient(remoteControlClient);
				}
				audioManager
						.registerMediaButtonEventReceiver(mediaButtonReceiver);
				media.onPlaybackStart();
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Failed to request Audiofocus");
			}
		}
	}

	private void writePlaybackPreferences() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Writing playback preferences");

		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext()).edit();
		if (media != null) {
			editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_MEDIA,
					media.getPlayableType());
			editor.putBoolean(
					PlaybackPreferences.PREF_CURRENT_EPISODE_IS_STREAM,
					shouldStream);
			editor.putBoolean(
					PlaybackPreferences.PREF_CURRENT_EPISODE_IS_VIDEO,
					playingVideo);
			if (media instanceof FeedMedia) {
				FeedMedia fMedia = (FeedMedia) media;
				editor.putLong(
						PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
						fMedia.getItem().getFeed().getId());
				editor.putLong(
						PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID,
						fMedia.getId());
			} else {
				editor.putLong(
						PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
						PlaybackPreferences.NO_MEDIA_PLAYING);
				editor.putLong(
						PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID,
						PlaybackPreferences.NO_MEDIA_PLAYING);
			}
			media.writeToPreferences(editor);
		} else {
			editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_MEDIA,
					PlaybackPreferences.NO_MEDIA_PLAYING);
			editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEED_ID,
					PlaybackPreferences.NO_MEDIA_PLAYING);
			editor.putLong(
					PlaybackPreferences.PREF_CURRENTLY_PLAYING_FEEDMEDIA_ID,
					PlaybackPreferences.NO_MEDIA_PLAYING);
		}

		editor.commit();
	}

	private void setStatus(PlayerStatus newStatus) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Setting status to " + newStatus);
		status = newStatus;
		sendBroadcast(new Intent(ACTION_PLAYER_STATUS_CHANGED));
		updateWidget();
		refreshRemoteControlClientState();
		bluetoothNotifyChange();
	}

	/** Send ACTION_PLAYER_STATUS_CHANGED without changing the status attribute. */
	private void postStatusUpdateIntent() {
		setStatus(status);
	}

	private void sendNotificationBroadcast(int type, int code) {
		Intent intent = new Intent(ACTION_PLAYER_NOTIFICATION);
		intent.putExtra(EXTRA_NOTIFICATION_TYPE, type);
		intent.putExtra(EXTRA_NOTIFICATION_CODE, code);
		sendBroadcast(intent);
	}

	/** Used by setupNotification to load notification data in another thread. */
	private AsyncTask<Void, Void, Void> notificationSetupTask;

	/** Prepares notification and starts the service in the foreground. */
	@SuppressLint("NewApi")
	private void setupNotification() {
		final PendingIntent pIntent = PendingIntent.getActivity(this, 0,
				PlaybackService.getPlayerActivityIntent(this),
				PendingIntent.FLAG_UPDATE_CURRENT);

		if (notificationSetupTask != null) {
			notificationSetupTask.cancel(true);
		}
		notificationSetupTask = new AsyncTask<Void, Void, Void>() {
			Bitmap icon = null;

			@Override
			protected Void doInBackground(Void... params) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Starting background work");
				if (android.os.Build.VERSION.SDK_INT >= 11) {
					if (media != null && media != null) {
						int iconSize = getResources().getDimensionPixelSize(
								android.R.dimen.notification_large_icon_width);
						icon = BitmapDecoder
								.decodeBitmapFromWorkerTaskResource(iconSize,
										media);
					}

				}
				if (icon == null) {
					icon = BitmapFactory.decodeResource(getResources(),
							R.drawable.ic_stat_antenna);
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				if (!isCancelled() && status == PlayerStatus.PLAYING
						&& media != null) {
					String contentText = media.getFeedTitle();
					String contentTitle = media.getEpisodeTitle();
					Notification notification = null;
					if (android.os.Build.VERSION.SDK_INT >= 16) {
						Intent pauseButtonIntent = new Intent(
								PlaybackService.this, PlaybackService.class);
						pauseButtonIntent.putExtra(
								MediaButtonReceiver.EXTRA_KEYCODE,
								KeyEvent.KEYCODE_MEDIA_PAUSE);
						PendingIntent pauseButtonPendingIntent = PendingIntent
								.getService(PlaybackService.this, 0,
										pauseButtonIntent,
										PendingIntent.FLAG_UPDATE_CURRENT);
						Notification.Builder notificationBuilder = new Notification.Builder(
								PlaybackService.this)
								.setContentTitle(contentTitle)
								.setContentText(contentText)
								.setOngoing(true)
								.setContentIntent(pIntent)
								.setLargeIcon(icon)
								.setSmallIcon(R.drawable.ic_stat_antenna)
								.addAction(android.R.drawable.ic_media_pause,
										getString(R.string.pause_label),
										pauseButtonPendingIntent);
						notification = notificationBuilder.build();
					} else {
						NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
								PlaybackService.this)
								.setContentTitle(contentTitle)
								.setContentText(contentText).setOngoing(true)
								.setContentIntent(pIntent).setLargeIcon(icon)
								.setSmallIcon(R.drawable.ic_stat_antenna);
						notification = notificationBuilder.getNotification();
					}
					startForeground(NOTIFICATION_ID, notification);
					if (AppConfig.DEBUG)
						Log.d(TAG, "Notification set up");
				}
			}

		};
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			notificationSetupTask
					.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			notificationSetupTask.execute();
		}

	}

	/**
	 * Seek a specific position from the current position
	 *
	 * @param delta
	 *            offset from current position (positive or negative)
	 * */
	public void seekDelta(int delta) {
		int position = getCurrentPositionSafe();
		if (position != INVALID_TIME) {
			seek(player.getCurrentPosition() + delta);
		}
	}

	public void seek(int i) {
		saveCurrentPosition();
		if (status == PlayerStatus.INITIALIZED
				|| status == PlayerStatus.INITIALIZING
				|| status == PlayerStatus.PREPARING) {
			media.setPosition(i);
			setStartWhenPrepared(true);
			prepare();
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Seeking position " + i);
			if (shouldStream) {
				if (status != PlayerStatus.SEEKING) {
					statusBeforeSeek = status;
				}
				setStatus(PlayerStatus.SEEKING);
			}
			player.seekTo(i);
		}
	}

	public void seekToChapter(Chapter chapter) {
		seek((int) chapter.getStart());
	}

	/** Saves the current position of the media file to the DB */
	private synchronized void saveCurrentPosition() {
		int position = getCurrentPositionSafe();
		if (position != INVALID_TIME) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Saving current position to " + position);
			media.saveCurrentPosition(PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext()),
					position);
		}
	}

	private void stopWidgetUpdater() {
		if (widgetUpdaterFuture != null) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Stopping widgetUpdateWorker");
			widgetUpdaterFuture.cancel(true);
		}
		sendBroadcast(new Intent(PlayerWidget.STOP_WIDGET_UPDATE));
	}

	@SuppressLint("NewApi")
	private void setupWidgetUpdater() {
		if (widgetUpdaterFuture == null
				|| (widgetUpdaterFuture.isCancelled() || widgetUpdaterFuture
						.isDone())) {
			widgetUpdater = new WidgetUpdateWorker();
			widgetUpdaterFuture = schedExecutor.scheduleAtFixedRate(
					widgetUpdater, WidgetUpdateWorker.NOTIFICATION_INTERVALL,
					WidgetUpdateWorker.NOTIFICATION_INTERVALL,
					TimeUnit.MILLISECONDS);
		}
	}

	private void updateWidget() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Sending widget update request");
		PlaybackService.this.sendBroadcast(new Intent(
				PlayerWidget.FORCE_WIDGET_UPDATE));
	}

	public boolean sleepTimerActive() {
		return sleepTimer != null && sleepTimer.isWaiting();
	}

	public long getSleepTimerTimeLeft() {
		if (sleepTimerActive()) {
			return sleepTimer.getWaitingTime();
		} else {
			return 0;
		}
	}

	@SuppressLint("NewApi")
	private RemoteControlClient setupRemoteControlClient() {
		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(mediaButtonReceiver);
		PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(
				getApplicationContext(), 0, mediaButtonIntent, 0);
		remoteControlClient = new RemoteControlClient(mediaPendingIntent);
		int controlFlags;
		if (android.os.Build.VERSION.SDK_INT < 16) {
			controlFlags = RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
					| RemoteControlClient.FLAG_KEY_MEDIA_NEXT;
		} else {
			controlFlags = RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE;
		}
		remoteControlClient.setTransportControlFlags(controlFlags);
		return remoteControlClient;
	}

	/** Refresh player status and metadata. */
	@SuppressLint("NewApi")
	private void refreshRemoteControlClientState() {
		if (android.os.Build.VERSION.SDK_INT >= 14) {
			if (remoteControlClient != null) {
				switch (status) {
				case PLAYING:
					remoteControlClient
							.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
					break;
				case PAUSED:
				case INITIALIZED:
					remoteControlClient
							.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
					break;
				case STOPPED:
					remoteControlClient
							.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
					break;
				case ERROR:
					remoteControlClient
							.setPlaybackState(RemoteControlClient.PLAYSTATE_ERROR);
					break;
				default:
					remoteControlClient
							.setPlaybackState(RemoteControlClient.PLAYSTATE_BUFFERING);
				}
				if (media != null) {
					MetadataEditor editor = remoteControlClient
							.editMetadata(false);
					editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
							media.getEpisodeTitle());

					editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
							media.getFeedTitle());

					editor.apply();
				}
				if (AppConfig.DEBUG)
					Log.d(TAG, "RemoteControlClient state was refreshed");
			}
		}
	}

	private void bluetoothNotifyChange() {
		boolean isPlaying = false;

		if (status == PlayerStatus.PLAYING) {
			isPlaying = true;
		}

        if (media != null) {
		    Intent i = new Intent(AVRCP_ACTION_PLAYER_STATUS_CHANGED);
		    i.putExtra("id", 1);
		    i.putExtra("artist", "");
		    i.putExtra("album", media.getFeedTitle());
		    i.putExtra("track", media.getEpisodeTitle());
		    i.putExtra("playing", isPlaying);
            if (queue != null) {
                i.putExtra("ListSize", queue.size());
            }
            i.putExtra("duration", media.getDuration());
		    i.putExtra("position", media.getPosition());
		    sendBroadcast(i);
        }
	}

	/**
	 * Pauses playback when the headset is disconnected and the preference is
	 * set
	 */
	private BroadcastReceiver headsetDisconnected = new BroadcastReceiver() {
		private static final String TAG = "headsetDisconnected";
		private static final int UNPLUGGED = 0;

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
				int state = intent.getIntExtra("state", -1);
				if (state != -1) {
					if (AppConfig.DEBUG)
						Log.d(TAG, "Headset plug event. State is " + state);
					if (state == UNPLUGGED && status == PlayerStatus.PLAYING) {
						if (AppConfig.DEBUG)
							Log.d(TAG, "Headset was unplugged during playback.");
						pauseIfPauseOnDisconnect();
					}
				} else {
					Log.e(TAG, "Received invalid ACTION_HEADSET_PLUG intent");
				}
			}
		}
	};

	private BroadcastReceiver audioBecomingNoisy = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// sound is about to change, eg. bluetooth -> speaker
			if (AppConfig.DEBUG)
				Log.d(TAG, "Pausing playback because audio is becoming noisy");
			pauseIfPauseOnDisconnect();
		}
		// android.media.AUDIO_BECOMING_NOISY
	};

	/** Pauses playback if PREF_PAUSE_ON_HEADSET_DISCONNECT was set to true. */
	private void pauseIfPauseOnDisconnect() {
		if (UserPreferences.isPauseOnHeadsetDisconnect()
				&& status == PlayerStatus.PLAYING) {
			pause(true, true);
		}
	}

	private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_SHUTDOWN_PLAYBACK_SERVICE)) {
				schedExecutor.shutdownNow();
				stop();
				media = null;
			}
		}

	};

	private BroadcastReceiver skipCurrentEpisodeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_SKIP_CURRENT_EPISODE)) {

				if (AppConfig.DEBUG)
					Log.d(TAG, "Received SKIP_CURRENT_EPISODE intent");
				if (media != null) {
					setStatus(PlayerStatus.STOPPED);
					endPlayback(true);
				}
			}
		}
    };

	/** Periodically saves the position of the media file */
	class PositionSaver implements Runnable {
		public static final int WAITING_INTERVALL = 5000;

		@Override
		public void run() {
			if (player != null && player.isPlaying()) {
				try {
					saveCurrentPosition();
				} catch (IllegalStateException e) {
					Log.w(TAG,
							"saveCurrentPosition was called in illegal state");
				}
			}
		}
	}

	/** Notifies the player widget in the specified intervall */
	class WidgetUpdateWorker implements Runnable {
		private static final int NOTIFICATION_INTERVALL = 1000;

		@Override
		public void run() {
			if (PlaybackService.isRunning) {
				updateWidget();
			}
		}
	}

	/** Sleeps for a given time and then pauses playback. */
	class SleepTimer implements Runnable {
		private static final String TAG = "SleepTimer";
		private static final long UPDATE_INTERVALL = 1000L;
		private volatile long waitingTime;
		private boolean isWaiting;

		public SleepTimer(long waitingTime) {
			super();
			this.waitingTime = waitingTime;
		}

		@Override
		public void run() {
			isWaiting = true;
			if (AppConfig.DEBUG)
				Log.d(TAG, "Starting");
			while (waitingTime > 0) {
				try {
					Thread.sleep(UPDATE_INTERVALL);
					waitingTime -= UPDATE_INTERVALL;

					if (waitingTime <= 0) {
						if (AppConfig.DEBUG)
							Log.d(TAG, "Waiting completed");
						if (status == PlayerStatus.PLAYING) {
							if (AppConfig.DEBUG)
								Log.d(TAG, "Pausing playback");
							pause(true, true);
						}
						postExecute();
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
			sendNotificationBroadcast(NOTIFICATION_TYPE_SLEEPTIMER_UPDATE, 0);
		}

		public long getWaitingTime() {
			return waitingTime;
		}

		public boolean isWaiting() {
			return isWaiting;
		}

	}

	public static boolean isPlayingVideo() {
		return playingVideo;
	}

	public boolean isShouldStream() {
		return shouldStream;
	}

	public PlayerStatus getStatus() {
		return status;
	}

	public Playable getMedia() {
		return media;
	}

	public IPlayer getPlayer() {
		return player;
	}

	public boolean isStartWhenPrepared() {
		return startWhenPrepared;
	}

	public void setStartWhenPrepared(boolean startWhenPrepared) {
		this.startWhenPrepared = startWhenPrepared;
		postStatusUpdateIntent();
	}

	public boolean canSetSpeed() {
		if (player != null && media != null && media.getMediaType() == MediaType.AUDIO) {
			return ((AudioPlayer) player).canSetSpeed();
		}
		return false;
	}

	public boolean canSetPitch() {
		if (player != null && media != null && media.getMediaType() == MediaType.AUDIO) {
			return ((AudioPlayer) player).canSetPitch();
		}
		return false;
	}

	public void setSpeed(float speed) {
		if (media != null && media.getMediaType() == MediaType.AUDIO) {
			AudioPlayer audioPlayer = (AudioPlayer) player;
			if (audioPlayer.canSetSpeed()) {
				audioPlayer.setPlaybackSpeed((float) speed);
				if (AppConfig.DEBUG)
					Log.d(TAG, "Playback speed was set to " + speed);
				sendNotificationBroadcast(
						NOTIFICATION_TYPE_PLAYBACK_SPEED_CHANGE, 0);
			}
		}
	}

	public void setPitch(float pitch) {
		if (media != null && media.getMediaType() == MediaType.AUDIO) {
			AudioPlayer audioPlayer = (AudioPlayer) player;
			if (audioPlayer.canSetPitch()) {
				audioPlayer.setPlaybackPitch((float) pitch);
			}
		}
	}

	public float getCurrentPlaybackSpeed() {
		if (media.getMediaType() == MediaType.AUDIO
				&& player instanceof AudioPlayer) {
			AudioPlayer audioPlayer = (AudioPlayer) player;
			if (audioPlayer.canSetSpeed()) {
				return audioPlayer.getCurrentSpeedMultiplier();
			}
		}
		return -1;
	}

	/**
	 * call getDuration() on mediaplayer or return INVALID_TIME if player is in
	 * an invalid state. This method should be used instead of calling
	 * getDuration() directly to avoid an error.
	 */
	public int getDurationSafe() {
		if (status != null && player != null) {
			switch (status) {
			case PREPARED:
			case PLAYING:
			case PAUSED:
			case SEEKING:
				try {
					return player.getDuration();
				} catch (IllegalStateException e) {
					e.printStackTrace();
					return INVALID_TIME;
				}
			default:
				return INVALID_TIME;
			}
		} else {
			return INVALID_TIME;
		}
	}

	/**
	 * call getCurrentPosition() on mediaplayer or return INVALID_TIME if player
	 * is in an invalid state. This method should be used instead of calling
	 * getCurrentPosition() directly to avoid an error.
	 */
	public int getCurrentPositionSafe() {
		if (status != null && player != null) {
			switch (status) {
			case PREPARED:
			case PLAYING:
			case PAUSED:
			case SEEKING:
				return player.getCurrentPosition();
			default:
				return INVALID_TIME;
			}
		} else {
			return INVALID_TIME;
		}
	}

	private void setCurrentlyPlayingMedia(long id) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext()).edit();
		editor.putLong(PlaybackPreferences.PREF_CURRENTLY_PLAYING_MEDIA, id);
		editor.commit();
	}

	private static class InitTask extends AsyncTask<Playable, Void, Playable> {
		private Playable playable;
		public PlayableException exception;

		@Override
		protected Playable doInBackground(Playable... params) {
			if (params[0] == null) {
				throw new IllegalArgumentException("Playable must not be null");
			}
			playable = params[0];

			try {
				playable.loadMetadata();
			} catch (PlayableException e) {
				e.printStackTrace();
				exception = e;
				return null;
			}
			return playable;
		}

		@SuppressLint("NewApi")
		public void executeAsync(Playable playable) {
			FlattrUtils.hasToken();
			if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
				executeOnExecutor(THREAD_POOL_EXECUTOR, playable);
			} else {
				execute(playable);
			}
		}

	}

    private void loadQueue() {
        dbLoaderExecutor.submit(new QueueLoaderTask());
    }

    private class QueueLoaderTask implements Runnable {
        @Override
        public void run() {
            List<FeedItem> queueRef = DBReader.getQueue(PlaybackService.this);
            queue = queueRef;
        }
    }
}
