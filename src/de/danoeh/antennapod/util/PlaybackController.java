package de.danoeh.antennapod.util;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.feed.SimpleChapter;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.service.PlayerStatus;

/**
 * Communicates with the playback service. GUI classes should use this class to
 * control playback instead of communicating with the PlaybackService directly.
 */
public abstract class PlaybackController {
	private static final String TAG = "PlaybackController";

	/**
	 * Returned by getPosition() or getDuration() if the playbackService is in
	 * an invalid state.
	 */
	public static final int INVALID_TIME = -1;

	static final int DEFAULT_SEEK_DELTA = 30000;

	private Activity activity;

	private PlaybackService playbackService;
	private FeedMedia media;
	private PlayerStatus status;

	private ScheduledThreadPoolExecutor schedExecutor;
	private static final int SCHED_EX_POOLSIZE = 1;

	protected MediaPositionObserver positionObserver;
	protected ScheduledFuture positionObserverFuture;

	private boolean mediaInfoLoaded = false;

	public PlaybackController(Activity activity) {
		this.activity = activity;
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
				});
	}

	/**
	 * Creates a new connection to the playbackService. Should be called in the
	 * activity's onResume() method.
	 */
	public void init() {
		bindToService();
	}

	/**
	 * Should be called if the PlaybackController is no longer needed, for
	 * example in the activity's onStop() method.
	 */
	public void release() {
		if (AppConfig.DEBUG)
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
		cancelPositionObserver();
	}

	/** Should be called in the activity's onPause() method. */
	public void pause() {
		mediaInfoLoaded = false;
		if (playbackService != null && playbackService.isPlayingVideo()) {
			playbackService.pause(true);
		}
	}

	/**
	 * Tries to establish a connection to the PlaybackService. If it isn't
	 * running, the PlaybackService will be started with the last played media
	 * as the arguments of the launch intent.
	 */
	private void bindToService() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Trying to connect to service");
		Intent serviceIntent = getPlayLastPlayedMediaIntent();
		boolean bound = false;
		if (!PlaybackService.isRunning) {
			if (serviceIntent != null) {
				activity.startService(serviceIntent);
				bound = activity.bindService(serviceIntent, mConnection, 0);
			} else {
				status = PlayerStatus.STOPPED;
				setupGUI();
				handleStatus();
			}
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG,
						"PlaybackService is running, trying to connect without start command.");
			bound = activity.bindService(new Intent(activity,
					PlaybackService.class), mConnection, 0);
		}
		if (AppConfig.DEBUG)
			Log.d(TAG, "Result for service binding: " + bound);
	}

	/**
	 * Returns an intent that starts the PlaybackService and plays the last
	 * played media or null if no last played media could be found.
	 */
	private Intent getPlayLastPlayedMediaIntent() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Trying to restore last played media");
		SharedPreferences prefs = activity.getApplicationContext()
				.getSharedPreferences(PodcastApp.PREF_NAME, 0);
		long mediaId = prefs.getLong(PlaybackService.PREF_LAST_PLAYED_ID, -1);
		long feedId = prefs.getLong(PlaybackService.PREF_LAST_PLAYED_FEED_ID,
				-1);
		if (mediaId != -1 && feedId != -1) {
			Intent serviceIntent = new Intent(activity, PlaybackService.class);
			serviceIntent.putExtra(PlaybackService.EXTRA_FEED_ID, feedId);
			serviceIntent.putExtra(PlaybackService.EXTRA_MEDIA_ID, mediaId);
			serviceIntent.putExtra(PlaybackService.EXTRA_START_WHEN_PREPARED,
					false);
			serviceIntent
					.putExtra(PlaybackService.EXTRA_SHOULD_STREAM, prefs
							.getBoolean(PlaybackService.PREF_LAST_IS_STREAM,
									true));
			return serviceIntent;
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "No last played media found");
			return null;
		}
	}

	public abstract void setupGUI();

	private void setupPositionObserver() {
		if ((positionObserverFuture != null && positionObserverFuture
				.isCancelled())
				|| (positionObserverFuture != null && positionObserverFuture
						.isDone()) || positionObserverFuture == null) {

			if (AppConfig.DEBUG)
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
			if (AppConfig.DEBUG)
				Log.d(TAG, "PositionObserver cancelled. Result: " + result);
		}
	}

	public abstract void onPositionObserverUpdate();

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			playbackService = ((PlaybackService.LocalBinder) service)
					.getService();

			activity.registerReceiver(statusUpdate, new IntentFilter(
					PlaybackService.ACTION_PLAYER_STATUS_CHANGED));

			activity.registerReceiver(notificationReceiver, new IntentFilter(
					PlaybackService.ACTION_PLAYER_NOTIFICATION));

			queryService();
			if (AppConfig.DEBUG)
				Log.d(TAG, "Connection to Service established");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			playbackService = null;
			if (AppConfig.DEBUG)
				Log.d(TAG, "Disconnected from Service");

		}
	};

	protected BroadcastReceiver statusUpdate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Received statusUpdate Intent.");
			if (playbackService != null) {
				status = playbackService.getStatus();
				handleStatus();
			} else {
				Log.w(TAG,
						"Couldn't receive status update: playbackService was null");
			}
		}
	};

	protected BroadcastReceiver notificationReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
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
					onReloadNotification(intent.getIntExtra(
							PlaybackService.EXTRA_NOTIFICATION_CODE, -1));
					queryService();

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
				}

			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Bad arguments. Won't handle intent");
			}

		}

	};

	/** Called when the currently displayed information should be refreshed. */
	public abstract void onReloadNotification(int code);

	public abstract void onBufferStart();

	public abstract void onBufferEnd();

	public abstract void onBufferUpdate(float progress);

	public abstract void onSleepTimerUpdate();

	public abstract void handleError(int code);

	/**
	 * Is called whenever the PlaybackService changes it's status. This method
	 * should be used to update the GUI or start/cancel background threads.
	 */
	private void handleStatus() {
		switch (status) {

		case ERROR:
			postStatusMsg(R.string.player_error_msg);
			break;
		case PAUSED:
			clearStatusMsg();
			checkMediaInfoLoaded();
			cancelPositionObserver();
			updatePlayButtonAppearance(R.drawable.av_play);
			break;
		case PLAYING:
			clearStatusMsg();
			checkMediaInfoLoaded();
			setupPositionObserver();
			updatePlayButtonAppearance(R.drawable.av_pause);
			break;
		case PREPARING:
			postStatusMsg(R.string.player_preparing_msg);
			checkMediaInfoLoaded();
			if (playbackService != null) {
				if (playbackService.isStartWhenPrepared()) {
					updatePlayButtonAppearance(R.drawable.av_pause);
				} else {
					updatePlayButtonAppearance(R.drawable.av_play);
				}
			}
			break;
		case STOPPED:
			postStatusMsg(R.string.player_stopped_msg);
			break;
		case PREPARED:
			checkMediaInfoLoaded();
			postStatusMsg(R.string.player_ready_msg);
			updatePlayButtonAppearance(R.drawable.av_play);
			break;
		case SEEKING:
			postStatusMsg(R.string.player_seeking_msg);
			break;
		case AWAITING_VIDEO_SURFACE:
			onAwaitingVideoSurface();
			break;
		}
	}

	private void checkMediaInfoLoaded() {
		if (!mediaInfoLoaded) {
			loadMediaInfo();
		}
		mediaInfoLoaded = true;
	}

	private void updatePlayButtonAppearance(int resource) {
		ImageButton butPlay = getPlayButton();
		butPlay.setImageResource(resource);
	}

	public abstract ImageButton getPlayButton();

	public abstract void postStatusMsg(int msg);

	public abstract void clearStatusMsg();

	public abstract void loadMediaInfo();

	public abstract void onAwaitingVideoSurface();

	/**
	 * Called when connection to playback service has been established or
	 * information has to be refreshed
	 */
	void queryService() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Querying service info");
		if (playbackService != null) {
			status = playbackService.getStatus();
			media = playbackService.getMedia();
			if (media == null) {
				Log.w(TAG,
						"PlaybackService has no media object. Trying to restore last played media.");
				Intent serviceIntent = getPlayLastPlayedMediaIntent();
				if (serviceIntent != null) {
					activity.startService(serviceIntent);
				}
			}
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
		if (fromUser && playbackService != null) {
			float prog = progress / ((float) seekBar.getMax());
			int duration = playbackService.getPlayer().getDuration();
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
			playbackService.seek((int) (prog * playbackService.getPlayer()
					.getDuration()));
			setupPositionObserver();
		}
	}

	public OnClickListener newOnPlayButtonClickListener() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (playbackService != null) {
					switch (status) {
					case PLAYING:
						playbackService.pause(true);
						break;
					case PAUSED:
					case PREPARED:
						playbackService.play();
						break;
					case PREPARING:
						playbackService.setStartWhenPrepared(!playbackService
								.isStartWhenPrepared());
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
					playbackService.seekDelta(-DEFAULT_SEEK_DELTA);
				}
			}
		};
	}

	public OnClickListener newOnFFButtonClickListener() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (status == PlayerStatus.PLAYING) {
					playbackService.seekDelta(DEFAULT_SEEK_DELTA);
				}
			}
		};
	}

	public boolean serviceAvailable() {
		return playbackService != null;
	}

	public int getPosition() {
		if (playbackService != null) {
			return playbackService.getPlayer().getCurrentPosition();
		} else {
			return INVALID_TIME;
		}
	}

	public int getDuration() {
		if (playbackService != null) {
			return playbackService.getPlayer().getDuration();
		} else {
			return INVALID_TIME;
		}
	}

	public FeedMedia getMedia() {
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

	public void setVideoSurface(SurfaceHolder holder) {
		if (playbackService != null) {
			playbackService.setVideoSurface(holder);
		}
	}

	public PlayerStatus getStatus() {
		return status;
	}

	public boolean isPlayingVideo() {
		if (playbackService != null) {
			return PlaybackService.isPlayingVideo();
		}
		return false;
	}

	public void notifyVideoSurfaceAbandoned() {
		if (playbackService != null) {
			playbackService.notifyVideoSurfaceAbandoned();
		}
	}

	/** Refreshes the current position of the media file that is playing. */
	public class MediaPositionObserver implements Runnable {

		public static final int WAITING_INTERVALL = 1000;

		@Override
		public void run() {
			if (playbackService != null && playbackService.getPlayer() != null
					&& playbackService.getPlayer().isPlaying()) {
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
