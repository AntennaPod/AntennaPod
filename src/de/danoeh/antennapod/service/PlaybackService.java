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
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AudioplayerActivity;
import de.danoeh.antennapod.activity.VideoplayerActivity;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.feed.MediaType;
import de.danoeh.antennapod.receiver.MediaButtonReceiver;
import de.danoeh.antennapod.receiver.PlayerWidget;
import de.danoeh.antennapod.util.ChapterUtils;

/** Controls the MediaPlayer that plays a FeedMedia-file */
public class PlaybackService extends Service {
	/** Logging tag */
	private static final String TAG = "PlaybackService";

	/** Contains the id of the media that was played last. */
	public static final String PREF_LAST_PLAYED_ID = "de.danoeh.antennapod.preferences.lastPlayedId";
	/** Contains the feed id of the last played item. */
	public static final String PREF_LAST_PLAYED_FEED_ID = "de.danoeh.antennapod.preferences.lastPlayedFeedId";
	/** True if last played media was streamed. */
	public static final String PREF_LAST_IS_STREAM = "de.danoeh.antennapod.preferences.lastIsStream";
	/** True if last played media was a video. */
	public static final String PREF_LAST_IS_VIDEO = "de.danoeh.antennapod.preferences.lastIsVideo";
	/** True if playback of last played media has been completed. */
	public static final String PREF_AUTO_DELETE_MEDIA_PLAYBACK_COMPLETED = "de.danoeh.antennapod.preferences.lastPlaybackCompleted";
	/**
	 * ID of the last played media which should be auto-deleted as soon as
	 * PREF_LAST_PLAYED_ID changes.
	 */
	public static final String PREF_AUTODELETE_MEDIA_ID = "de.danoeh.antennapod.preferences.autoDeleteMediaId";

	/** Contains the id of the FeedMedia object. */
	public static final String EXTRA_MEDIA_ID = "extra.de.danoeh.antennapod.service.mediaId";
	/** Contains the id of the Feed object of the FeedMedia. */
	public static final String EXTRA_FEED_ID = "extra.de.danoeh.antennapod.service.feedId";
	/** True if media should be streamed. */
	public static final String EXTRA_SHOULD_STREAM = "extra.de.danoeh.antennapod.service.shouldStream";
	/**
	 * True if playback should be started immediately after media has been
	 * prepared.
	 */
	public static final String EXTRA_START_WHEN_PREPARED = "extra.de.danoeh.antennapod.service.startWhenPrepared";

	public static final String EXTRA_PREPARE_IMMEDIATELY = "extra.de.danoeh.antennapod.service.prepareImmediately";

	public static final String ACTION_PLAYER_STATUS_CHANGED = "action.de.danoeh.antennapod.service.playerStatusChanged";

	public static final String ACTION_PLAYER_NOTIFICATION = "action.de.danoeh.antennapod.service.playerNotification";
	public static final String EXTRA_NOTIFICATION_CODE = "extra.de.danoeh.antennapod.service.notificationCode";
	public static final String EXTRA_NOTIFICATION_TYPE = "extra.de.danoeh.antennapod.service.notificationType";

	/**
	 * If the PlaybackService receives this action, it will stop playback and
	 * try to shutdown.
	 */
	public static final String ACTION_SHUTDOWN_PLAYBACK_SERVICE = "action.de.danoeh.antennapod.service.actionShutdownPlaybackService";

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

	/**
	 * Returned by getPositionSafe() or getDurationSafe() if the playbackService
	 * is in an invalid state.
	 */
	public static final int INVALID_TIME = -1;

	/** Is true if service is running. */
	public static boolean isRunning = false;

	private static final int NOTIFICATION_ID = 1;
	private NotificationCompat.Builder notificationBuilder;

	private AudioManager audioManager;
	private ComponentName mediaButtonReceiver;

	private MediaPlayer player;
	private RemoteControlClient remoteControlClient;

	private FeedMedia media;
	private Feed feed;
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

	private Thread chapterLoader;

	private static final int SCHED_EX_POOL_SIZE = 3;
	private ScheduledThreadPoolExecutor schedExecutor;

	private volatile PlayerStatus statusBeforeSeek;

	private static boolean playingVideo;

	/** True if mediaplayer was paused because it lost audio focus temporarily */
	private boolean pausedBecauseOfTransientAudiofocusLoss;

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
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(context
							.getApplicationContext());
			boolean isVideo = pref.getBoolean(PREF_LAST_IS_VIDEO, false);
			if (isVideo) {
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
	public static Intent getPlayerActivityIntent(Context context,
			FeedMedia media) {
		MediaType mt = media.getMediaType();
		if (mt == MediaType.VIDEO) {
			return new Intent(context, VideoplayerActivity.class);
		} else {
			return new Intent(context, AudioplayerActivity.class);
		}
	}

	/** Get last played FeedMedia object or null if it doesn't exist. */
	public static FeedMedia getLastPlayedMediaFromPreferences(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		long mediaId = prefs.getLong(PlaybackService.PREF_LAST_PLAYED_ID, -1);
		long feedId = prefs.getLong(PlaybackService.PREF_LAST_PLAYED_FEED_ID,
				-1);
		FeedManager manager = FeedManager.getInstance();
		if (mediaId != -1 && feedId != -1) {
			Feed feed = manager.getFeed(feedId);
			if (feed != null) {
				return manager.getFeedMedia(mediaId, feed);
			}
		}
		return null;
	}

	private void setLastPlayedMediaId(long mediaId) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		long autoDeleteId = prefs.getLong(PREF_AUTODELETE_MEDIA_ID, -1);
		SharedPreferences.Editor editor = prefs.edit();
		if (mediaId == autoDeleteId) {
			editor.putBoolean(PREF_AUTO_DELETE_MEDIA_PLAYBACK_COMPLETED, false);
		}
		editor.putLong(PREF_LAST_PLAYED_ID, mediaId);
		editor.commit();
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
			audioManager
					.registerRemoteControlClient(setupRemoteControlClient());
		}
		registerReceiver(headsetDisconnected, new IntentFilter(
				Intent.ACTION_HEADSET_PLUG));
		registerReceiver(shutdownReceiver, new IntentFilter(
				ACTION_SHUTDOWN_PLAYBACK_SERVICE));

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
		disableSleepTimer();
		unregisterReceiver(headsetDisconnected);
		unregisterReceiver(shutdownReceiver);
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
				pause(true);
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
					pause(false);
					pausedBecauseOfTransientAudiofocusLoss = true;
				}
			}
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "OnStartCommand called");
		int keycode = intent.getIntExtra(MediaButtonReceiver.EXTRA_KEYCODE, -1);
		if (keycode != -1) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Received media button event");
			handleKeycode(keycode);
		} else {

			long mediaId = intent.getLongExtra(EXTRA_MEDIA_ID, -1);
			long feedId = intent.getLongExtra(EXTRA_FEED_ID, -1);
			boolean playbackType = intent.getBooleanExtra(EXTRA_SHOULD_STREAM,
					true);
			if (mediaId == -1 || feedId == -1) {
				Log.e(TAG,
						"Media ID or Feed ID wasn't provided to the Service.");
				if (media == null || feed == null) {
					stopSelf();
				}
				// Intent values appear to be valid
				// check if already playing and playbackType is the same
			} else if (media == null || mediaId != media.getId()
					|| playbackType != shouldStream) {
				pause(true);
				player.reset();
				sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD, 0);
				if (media == null || mediaId != media.getId()) {
					feed = manager.getFeed(feedId);
					media = manager.getFeedMedia(mediaId, feed);
				}

				if (media != null) {
					shouldStream = playbackType;
					startWhenPrepared = intent.getBooleanExtra(
							EXTRA_START_WHEN_PREPARED, false);
					prepareImmediately = intent.getBooleanExtra(EXTRA_PREPARE_IMMEDIATELY, false);
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
				pause(false);
			} else if (status == PlayerStatus.PAUSED) {
				play();
			} else if (status == PlayerStatus.PREPARING) {
				setStartWhenPrepared(!startWhenPrepared);
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PLAY:
			if (status == PlayerStatus.PAUSED) {
				play();
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
			if (status == PlayerStatus.PLAYING) {
				pause(false);
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
				if (shouldStream) {
					player.setDataSource(media.getDownload_url());
					setStatus(PlayerStatus.PREPARING);
					player.prepareAsync();
				} else {
					player.setDataSource(media.getFile_url());
					setStatus(PlayerStatus.PREPARING);
					player.prepare();
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
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
		initMediaplayer();
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
				playingVideo = false;
				if (shouldStream) {
					player.setDataSource(media.getDownload_url());
				} else if (media.getFile_url() != null) {
					player.setDataSource(media.getFile_url());
				}
				if (prepareImmediately) {
					setStatus(PlayerStatus.PREPARING);
					player.prepareAsync();
				} else {
					setStatus(PlayerStatus.INITIALIZED);
				}
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
		} catch (IOException e) {
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
			if (startWhenPrepared) {
				play();
			}
			if (shouldStream && media.getItem().getChapters() == null) {
				// load chapters if available
				if (chapterLoader != null) {
					chapterLoader.interrupt();
				}
				chapterLoader = new Thread() {

					@Override
					public void run() {
						if (AppConfig.DEBUG)
							Log.d(TAG, "Starting chapterLoader thread");
						ChapterUtils
								.readID3ChaptersFromFeedMediaDownloadUrl(media
										.getItem());
						if (media.getItem().getChapters() != null
								&& !interrupted()) {
							sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD,
									0);
							manager.setFeedItem(PlaybackService.this,
									media.getItem());
						}
						if (AppConfig.DEBUG)
							Log.d(TAG, "ChapterLoaderThread has finished");
					}

				};
				chapterLoader.start();
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
				pause(true);
			}
			sendNotificationBroadcast(NOTIFICATION_TYPE_ERROR, what);
			stopSelf();
			return true;
		}
	};

	private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Playback completed");
			audioManager.abandonAudioFocus(audioFocusChangeListener);
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			// Save state
			cancelPositionSaver();
			media.setPosition(0);
			media.setPlaybackCompletionDate(new Date());
			manager.markItemRead(PlaybackService.this, media.getItem(), true);
			FeedItem nextItem = manager
					.getQueueSuccessorOfItem(media.getItem());
			boolean isInQueue = manager.isInQueue(media.getItem());
			if (isInQueue) {
				manager.removeQueueItem(PlaybackService.this, media.getItem());
			}
			manager.addItemToPlaybackHistory(PlaybackService.this,
					media.getItem());
			manager.setFeedMedia(PlaybackService.this, media);

			long autoDeleteMediaId = media.getId();

			if (shouldStream) {
				autoDeleteMediaId = -1;
			}
			SharedPreferences.Editor editor = prefs.edit();
			editor.putLong(PREF_AUTODELETE_MEDIA_ID, autoDeleteMediaId);
			editor.putBoolean(PREF_AUTO_DELETE_MEDIA_PLAYBACK_COMPLETED, true);
			editor.commit();

			// Prepare for playing next item
			boolean followQueue = prefs.getBoolean(
					PodcastApp.PREF_FOLLOW_QUEUE, false);
			boolean playNextItem = isInQueue && followQueue && nextItem != null;
			if (playNextItem) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Loading next item in queue");
				media = nextItem.getMedia();
				feed = nextItem.getFeed();
				shouldStream = !media.isDownloaded();
				startWhenPrepared = true;
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG,
							"No more episodes available to play; Reloading current episode");
				startWhenPrepared = false;
				stopForeground(true);
				stopWidgetUpdater();
			}
			int notificationCode = 0;
			if (media.getMediaType() == MediaType.AUDIO) {
				notificationCode = EXTRA_CODE_AUDIO;
				playingVideo = false;
			} else if (media.getMediaType() == MediaType.VIDEO) {
				notificationCode = EXTRA_CODE_VIDEO;
			}
			resetVideoSurface();
			refreshRemoteControlClientState();
			sendNotificationBroadcast(NOTIFICATION_TYPE_RELOAD,
					notificationCode);
		}
	};

	private MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {

		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			sendNotificationBroadcast(NOTIFICATION_TYPE_BUFFER_UPDATE, percent);

		}
	};

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
	 */
	public void pause(boolean abandonFocus) {
		if (player.isPlaying()) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Pausing playback.");
			player.pause();
			if (abandonFocus) {
				audioManager.abandonAudioFocus(audioFocusChangeListener);
				disableSleepTimer();
			}
			cancelPositionSaver();
			saveCurrentPosition();
			setStatus(PlayerStatus.PAUSED);
			stopWidgetUpdater();
			stopForeground(true);
		}
	}

	/** Pauses playback and destroys service. Recommended for video playback. */
	public void stop() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Stopping playback");
		player.stop();
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
				SharedPreferences.Editor editor = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext())
						.edit();
				editor.putLong(PREF_LAST_PLAYED_FEED_ID, feed.getId());
				editor.putBoolean(PREF_LAST_IS_STREAM, shouldStream);
				editor.putBoolean(PREF_LAST_IS_VIDEO, playingVideo);
				editor.commit();
				setLastPlayedMediaId(media.getId());
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
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Failed to request Audiofocus");
			}
		}
	}

	private void setStatus(PlayerStatus newStatus) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Setting status to " + newStatus);
		status = newStatus;
		sendBroadcast(new Intent(ACTION_PLAYER_STATUS_CHANGED));
		updateWidget();
		refreshRemoteControlClientState();
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

	/** Prepares notification and starts the service in the foreground. */
	private void setupNotification() {
		PendingIntent pIntent = PendingIntent.getActivity(this, 0,
				PlaybackService.getPlayerActivityIntent(this),
				PendingIntent.FLAG_UPDATE_CURRENT);

		Bitmap icon = BitmapFactory.decodeResource(null,
				R.drawable.ic_stat_antenna);
		notificationBuilder = new NotificationCompat.Builder(this)
				.setContentTitle(
						getString(R.string.playbackservice_notification_title))
				.setContentText(
						getString(R.string.playbackservice_notification_content))
				.setOngoing(true).setContentIntent(pIntent).setLargeIcon(icon)
				.setSmallIcon(R.drawable.ic_stat_antenna);

		startForeground(NOTIFICATION_ID, notificationBuilder.getNotification());
		if (AppConfig.DEBUG)
			Log.d(TAG, "Notification set up");
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
		if (AppConfig.DEBUG)
			Log.d(TAG, "Seeking position " + i);
		if (shouldStream) {
			if (status != PlayerStatus.SEEKING) {
				statusBeforeSeek = status;
			}
			setStatus(PlayerStatus.SEEKING);
		}
		player.seekTo(i);
		saveCurrentPosition();
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
			media.setPosition(position);
			manager.setFeedMedia(this, media);
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
							media.getItem().getTitle());

					editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
							media.getItem().getFeed().getTitle());

					editor.apply();
				}
				if (AppConfig.DEBUG)
					Log.d(TAG, "RemoteControlClient state was refreshed");
			}
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
					boolean pauseOnDisconnect = PreferenceManager
							.getDefaultSharedPreferences(
									getApplicationContext())
							.getBoolean(
									PodcastApp.PREF_PAUSE_ON_HEADSET_DISCONNECT,
									false);
					if (AppConfig.DEBUG)
						Log.d(TAG, "pauseOnDisconnect preference is "
								+ pauseOnDisconnect);
					if (state == UNPLUGGED && pauseOnDisconnect
							&& status == PlayerStatus.PLAYING) {
						if (AppConfig.DEBUG)
							Log.d(TAG,
									"Pausing playback because headset was disconnected");
						pause(false);
					}
				} else {
					Log.e(TAG, "Received invalid ACTION_HEADSET_PLUG intent");
				}
			}
		}
	};

	private BroadcastReceiver shutdownReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_SHUTDOWN_PLAYBACK_SERVICE)) {
				schedExecutor.shutdownNow();
				if (chapterLoader != null) {
					chapterLoader.interrupt();
				}
				stop();
				media = null;
				feed = null;
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
							pause(true);
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

	public FeedMedia getMedia() {
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
				return player.getDuration();
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

}
