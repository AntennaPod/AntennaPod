package de.danoeh.antennapod.service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.FeedComponent;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.feed.MediaType;
import de.danoeh.antennapod.preferences.PlaybackPreferences;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.receiver.PlayerWidget;
import de.danoeh.antennapod.util.BitmapDecoder;
import de.danoeh.antennapod.util.flattr.FlattrUtils;
import de.danoeh.antennapod.util.playback.Playable;
import de.danoeh.antennapod.util.playback.Playable.PlayableException;

/** Controls the MediaPlayer that plays a FeedMedia-file */
public class PlaybackService extends Service {
	/** Logging tag */
	private static final String TAG = "PlaybackService";

	/** Parcelable of type Playable. */
	public static final String EXTRA_PLAYABLE = "PlaybackService.PlayableExtra";
	/** True if media should be streamed. */
	public static final String EXTRA_SHOULD_STREAM = "extra.de.danoeh.antennapod.service.shouldStream";
	/**
	 * True if playback should be started immediately after media has been
	 * prepared.
	 */
	public static final String EXTRA_START_WHEN_PREPARED = "extra.de.danoeh.antennapod.service.startWhenPrepared";

	public static final String EXTRA_PREPARE_IMMEDIATELY = "extra.de.danoeh.antennapod.service.prepareImmediately";

	public static final String ACTION_PLAYER_STATUS_CHANGED = "action.de.danoeh.antennapod.service.playerStatusChanged";
	private static final String AVRCP_ACTION_PLAYER_STATUS_CHANGED= "com.android.music.playstatechanged";
	
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
	 * */
	public static final String ACTION_SKIP_CURRENT_EPISODE = "action.de.danoeh.antennapod.service.skipCurrentEpisode";

	/** Used in NOTIFICATION_TYPE_RELOAD. */
	public static final int EXTRA_CODE_AUDIO = 1;
	public static final int EXTRA_CODE_VIDEO = 2;

	public static final int NOTIFICATION_TYPE_ERROR = 0;
	public static final int NOTIFICATION_TYPE_INFO = 1;
	public static final int NOTIFICATION_TYPE_BUFFER_UPDATE = 2;
	public static final int NOTIFICATION_TYPE_RELOAD = 3;
	/** The state of the sleeptimer changed. */
	public static final int NOTIFICATION_TYPE_SLEEPTIMER_UPDATE = 4;
	public static final int NOTIFICATION_TYPE_BUFFER_START = 5;
	public static final int NOTIFICATION_TYPE_BUFFER_END = 6;
	/** No more episodes are going to be played. */
	public static final int NOTIFICATION_TYPE_PLAYBACK_END = 7;

	/**
	 * Returned by getPositionSafe() or getDurationSafe() if the playbackService
	 * is in an invalid state.
	 */
	public static final int INVALID_TIME = -1;

	/** Is true if service is running. */
	public static boolean isRunning = false;

	private static final int NOTIFICATION_ID = 1;

	private AudioManager audioManager;
	private ComponentName mediaButtonReceiver;

	private MediaPlayer player;
	private RemoteControlClient remoteControlClient;

	private Playable media;

	/** True if media should be streamed (Extracted from Intent Extra) . */
	private boolean shouldStream;

	/** True if service should prepare playback after it has been initialized */
	private boolean prepareImmediately;
	private boolean startWhenPrepared;
	private FeedManager manager;
	private PlayerStatus status;

	private PositionSaver positionSaver;
	private ScheduledFuture positionSaverFuture;

	private WidgetUpdateWorker widgetUpdater;
	private ScheduledFuture widgetUpdaterFuture;

	private SleepTimer sleepTimer;
	private Future sleepTimerFuture;

	private static final int SCHED_EX_POOL_SIZE = 3;
	private ScheduledThreadPoolExecutor schedExecutor;

	private volatile PlayerStatus statusBeforeSeek;

	private static boolean playingVideo;

	/** True if mediaplayer was paused because it lost audio focus temporarily */
	private boolean pausedBecauseOfTransientAudiofocusLoss;

	private Thread chapterLoader;

	private final IBinder mBinder = new LocalBinder();

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
	 * */
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
		manager = FeedManager.getInstance();
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
				});
		player = createMediaPlayer();

		mediaButtonReceiver = new ComponentName(getPackageName(),
				MediaButtonReceiver.class.getName());
		audioManager.registerMediaButtonEventReceiver(mediaButtonReceiver);
		if (android.os.Build.VERSION.SDK_INT >= 14) {
			if (UserPreferences.isNoLockscreenControl()){
			audioManager
					.registerRemoteControlClient(setupRemoteControlClient());
			}
		}
		registerReceiver(headsetDisconnected, new IntentFilter(
				Intent.ACTION_HEADSET_PLUG));
		registerReceiver(shutdownReceiver, new IntentFilter(
				ACTION_SHUTDOWN_PLAYBACK_SERVICE));
		registerReceiver(audioBecomingNoisy, new IntentFilter(
				AudioManager.ACTION_AUDIO_BECOMING_NOISY));
		registerReceiver(skipCurrentEpisodeReceiver, new IntentFilter(
				ACTION_SKIP_CURRENT_EPISODE));

	}

	private MediaPlayer createMediaPlayer() {
		return createMediaPlayer(new MediaPlayer());
	}

	private MediaPlayer createMediaPlayer(MediaPlayer mp) {
		if (mp != null) {
			mp.setOnPreparedListener(preparedListener);
			mp.setOnCompletionListener(completionListener);
			mp.setOnSeekCompleteListener(onSeekCompleteListener);
			mp.setOnErrorListener(onErrorListener);
			mp.setOnBufferingUpdateListener(onBufferingUpdateListener);
			mp.setOnInfoListener(onInfoListener);
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
					if (AppConfig.DEBUG)
						Log.d(TAG, "Lost audio focus temporarily. Ducking...");
					audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
							AudioManager.ADJUST_LOWER, 0);
					pausedBecauseOfTransientAudiofocusLoss = true;
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

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		if (AppConfig.DEBUG)
			Log.d(TAG, "OnStartCommand called");
		int keycode = intent.getIntExtra(MediaButtonReceiver.EXTRA_KEYCODE, -1);
		if (keycode != -1) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Received media button event");
			handleKeycode(keycode);
		} else {

			Playable playable = intent.getParcelableExtra(EXTRA_PLAYABLE);
			boolean playbackType = intent.getBooleanExtra(EXTRA_SHOULD_STREAM,
					true);
			if (playable == null) {
				Log.e(TAG, "Playable extra wasn't sent to the service");
				if (media == null) {
					stopSelf();
				}
				// Intent values appear to be valid
				// check if already playing and playbackType is the same
			} else if (media == null || playable != media
					|| playbackType != shouldStream) {
				pause(true, false);
				player.reset();
				sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
				if (media == null
						|| playable.getIdentifier() != media.getIdentifier()) {
					media = playable;
				}

				if (media != null) {
					shouldStream = playbackType;
					startWhenPrepared = intent.getBooleanExtra(
							EXTRA_START_WHEN_PREPARED, false);
					prepareImmediately = intent.getBooleanExtra(
							EXTRA_PREPARE_IMMEDIATELY, false);
					initMediaplayer();

				} else {
					Log.e(TAG, "Media is null");
					stopSelf();
				}

			} else if (media != null) {
				if (status == PlayerStatus.PAUSED) {
					play();
				}

			} else {
				Log.w(TAG, "Something went wrong. Shutting down...");
				stopSelf();
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

	/** Called when the surface holder of the mediaplayer has to be changed. */
	private void resetVideoSurface() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Resetting video surface");
		cancelPositionSaver();
		player.setDisplay(null);
		player.reset();
		player.release();
		player = createMediaPlayer();
		status = PlayerStatus.STOPPED;
		if (media != null) {
			initMediaplayer();
		}
	}

	public void notifyVideoSurfaceAbandoned() {
		resetVideoSurface();
	}

	/** Called after service has extracted the media it is supposed to play. */
	private void initMediaplayer() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Setting up media player");
		try {
			MediaType mediaType = media.getMediaType();
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

	private MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
		@Override
		public void onPrepared(MediaPlayer mp) {
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
	};

	private MediaPlayer.OnSeekCompleteListener onSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {

		@Override
		public void onSeekComplete(MediaPlayer mp) {
			if (status == PlayerStatus.SEEKING) {
				setStatus(statusBeforeSeek);
			}

		}
	};

	private MediaPlayer.OnInfoListener onInfoListener = new MediaPlayer.OnInfoListener() {

		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
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
	};

	private MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
		private static final String TAG = "PlaybackService.onErrorListener";

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			Log.w(TAG, "An error has occured: " + what);
			if (mp.isPlaying()) {
				pause(true, true);
			}
			sendNotificationBroadcast(NOTIFICATION_TYPE_ERROR, what);
			setCurrentlyPlayingMedia(PlaybackPreferences.NO_MEDIA_PLAYING);
			stopSelf();
			return true;
		}
	};

	private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			endPlayback(true);
		}
	};

	private MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {

		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_UPDATE, percent);

		}
	};

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
			((FeedMedia) media).setPlaybackCompletionDate(new Date());
			manager.markItemRead(PlaybackService.this, item, true, true);
			nextItem = manager.getQueueSuccessorOfItem(item);
			isInQueue = media instanceof FeedMedia
					&& manager.isInQueue(((FeedMedia) media).getItem());
			if (isInQueue) {
				manager.removeQueueItem(PlaybackService.this, item);
			}
			manager.addItemToPlaybackHistory(PlaybackService.this, item);
			manager.setFeedMedia(PlaybackService.this, (FeedMedia) media);
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
	 * @param reset
	 *            is true if service should reinit after pausing if the media
	 *            file is being streamed
	 */
	public void pause(boolean abandonFocus, boolean reinit) {
		if (player.isPlaying()) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Pausing playback.");
			player.pause();
			if (abandonFocus) {
				audioManager.abandonAudioFocus(audioFocusChangeListener);
				pausedBecauseOfTransientAudiofocusLoss = false;
				disableSleepTimer();
			}
			cancelPositionSaver();
			saveCurrentPosition();
			setStatus(PlayerStatus.PAUSED);
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
		prepareImmediately = false;
		initMediaplayer();
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
		
	    Intent i = new Intent(AVRCP_ACTION_PLAYER_STATUS_CHANGED);
	    i.putExtra("id", 1);
	    i.putExtra("artist", "");
	    i.putExtra("album", media.getFeedTitle());
	    i.putExtra("track", media.getEpisodeTitle());
	    i.putExtra("playing", isPlaying);        
	    i.putExtra("ListSize", manager.getQueueSize(false));
	    i.putExtra("duration", media.getDuration());
	    i.putExtra("position", media.getPosition());
	    sendBroadcast(i);
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
					player.reset();
					endPlayback(false);
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

	public MediaPlayer getPlayer() {
		return player;
	}

	public boolean isStartWhenPrepared() {
		return startWhenPrepared;
	}

	public void setStartWhenPrepared(boolean startWhenPrepared) {
		this.startWhenPrepared = startWhenPrepared;
		postStatusUpdateIntent();
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
}
