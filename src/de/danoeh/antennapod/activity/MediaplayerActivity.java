package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.dialog.TimeDialog;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.service.PlayerStatus;
import de.danoeh.antennapod.util.Converter;
import de.danoeh.antennapod.util.MediaPlayerError;
import de.danoeh.antennapod.util.StorageUtils;
import de.danoeh.antennapod.util.menuhandler.FeedItemMenuHandler;

public abstract class MediaplayerActivity extends SherlockFragmentActivity implements OnSeekBarChangeListener{
	private static final String TAG = "MediaplayerActivity";

	static final int DEFAULT_SEEK_DELTA = 30000;

	/** True if media information was loaded. */
	protected boolean mediaInfoLoaded = false;
	protected PlaybackService playbackService;
	protected MediaPositionObserver positionObserver;
	protected FeedMedia media;
	protected PlayerStatus status;
	protected FeedManager manager;

	protected TextView txtvPosition;
	protected TextView txtvLength;
	protected SeekBar sbPosition;
	protected ImageButton butPlay;
	protected ImageButton butRev;
	protected ImageButton butFF;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (AppConfig.DEBUG)
			Log.d(TAG, "Creating Activity");
		StorageUtils.checkStorageAvailability(this);

		orientation = getResources().getConfiguration().orientation;
		manager = FeedManager.getInstance();
		getWindow().setFormat(PixelFormat.TRANSPARENT);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		bindToService();
	}

	protected OnClickListener playbuttonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (status == PlayerStatus.PLAYING) {
				playbackService.pause(true);
			} else if (status == PlayerStatus.PAUSED
					|| status == PlayerStatus.PREPARED) {
				playbackService.play();
			}
		}
	};
	protected ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			playbackService = ((PlaybackService.LocalBinder) service)
					.getService();

			registerReceiver(statusUpdate, new IntentFilter(
					PlaybackService.ACTION_PLAYER_STATUS_CHANGED));

			registerReceiver(notificationReceiver, new IntentFilter(
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
			status = playbackService.getStatus();
			handleStatus();
		}
	};

	/**
	 * Should be used to switch to another player activity if the mime type is
	 * not the correct one for the current activity.
	 */
	protected abstract void onReloadNotification(int notificationCode);
	protected abstract void onBufferStart();
	protected abstract void onBufferEnd();

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
					if (sbPosition != null) {
						float progress = ((float) code) / 100;
						sbPosition.setSecondaryProgress((int) progress
								* sbPosition.getMax());
					}
					break;
				case PlaybackService.NOTIFICATION_TYPE_RELOAD:
					if (positionObserver != null) {
						positionObserver.cancel(true);
						positionObserver = null;
					}
					mediaInfoLoaded = false;
					onReloadNotification(intent.getIntExtra(
							PlaybackService.EXTRA_NOTIFICATION_CODE, -1));
					queryService();

					break;
				case PlaybackService.NOTIFICATION_TYPE_SLEEPTIMER_UPDATE:
					invalidateOptionsMenu();
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

	/** Current screen orientation. */
	protected int orientation;

	@Override
	protected void onStop() {
		super.onStop();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Activity stopped");
		try {
			unregisterReceiver(statusUpdate);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			unregisterReceiver(notificationReceiver);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		try {
			unbindService(mConnection);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		if (positionObserver != null) {
			positionObserver.cancel(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.mediaplayer, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		menu.findItem(R.id.support_item).setVisible(
				media != null && media.getItem().getPaymentLink() != null);
		menu.findItem(R.id.share_link_item).setVisible(
				media != null && media.getItem().getLink() != null);
		menu.findItem(R.id.visit_website_item).setVisible(
				media != null && media.getItem().getLink() != null);

		boolean sleepTimerSet = playbackService != null
				&& playbackService.sleepTimerActive();
		boolean sleepTimerNotSet = playbackService != null
				&& !playbackService.sleepTimerActive();
		menu.findItem(R.id.set_sleeptimer_item).setVisible(sleepTimerNotSet);
		menu.findItem(R.id.disable_sleeptimer_item).setVisible(sleepTimerSet);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			startActivity(new Intent(MediaplayerActivity.this,
					MainActivity.class));
			break;
		case R.id.disable_sleeptimer_item:
			if (playbackService != null) {
				AlertDialog.Builder stDialog = new AlertDialog.Builder(this);
				stDialog.setTitle(R.string.sleep_timer_label);
				stDialog.setMessage(getString(R.string.time_left_label)
						+ Converter.getDurationStringLong((int) playbackService
								.getSleepTimerTimeLeft()));
				stDialog.setPositiveButton(R.string.disable_sleeptimer_label,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
								if (playbackService != null) {
									playbackService.disableSleepTimer();
								}
							}
						});
				stDialog.setNegativeButton(R.string.cancel_label,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
				stDialog.create().show();
			}
			break;
		case R.id.set_sleeptimer_item:
			if (playbackService != null) {
				TimeDialog td = new TimeDialog(this,
						R.string.set_sleeptimer_label,
						R.string.set_sleeptimer_label) {

					@Override
					public void onTimeEntered(long millis) {
						if (playbackService != null) {
							playbackService.setSleepTimer(millis);
						}
					}
				};
				td.show();
				break;

			}
		default:
			return FeedItemMenuHandler.onMenuItemClicked(this, item,
					media.getItem());
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Resuming Activity");
		StorageUtils.checkStorageAvailability(this);
		bindToService();

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// ignore orientation change

	}

	protected void bindToService() {
		Intent serviceIntent = new Intent(this, PlaybackService.class);
		boolean bound = false;
		if (!PlaybackService.isRunning) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Trying to restore last played media");
			SharedPreferences prefs = getApplicationContext()
					.getSharedPreferences(PodcastApp.PREF_NAME, 0);
			long mediaId = prefs.getLong(PlaybackService.PREF_LAST_PLAYED_ID,
					-1);
			long feedId = prefs.getLong(
					PlaybackService.PREF_LAST_PLAYED_FEED_ID, -1);
			if (mediaId != -1 && feedId != -1) {
				serviceIntent.putExtra(PlaybackService.EXTRA_FEED_ID, feedId);
				serviceIntent.putExtra(PlaybackService.EXTRA_MEDIA_ID, mediaId);
				serviceIntent.putExtra(
						PlaybackService.EXTRA_START_WHEN_PREPARED, false);
				serviceIntent.putExtra(PlaybackService.EXTRA_SHOULD_STREAM,
						prefs.getBoolean(PlaybackService.PREF_LAST_IS_STREAM,
								true));
				startService(serviceIntent);
				bound = bindService(serviceIntent, mConnection,
						Context.BIND_AUTO_CREATE);
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "No last played media found");
				status = PlayerStatus.STOPPED;
				setupGUI();
				handleStatus();
			}
		} else {
			bound = bindService(serviceIntent, mConnection, 0);
		}
		if (AppConfig.DEBUG)
			Log.d(TAG, "Result for service binding: " + bound);
	}

	private void handleStatus() {
		switch (status) {

		case ERROR:
			postStatusMsg(R.string.player_error_msg);
			break;
		case PAUSED:
			postStatusMsg(R.string.player_paused_msg);
			loadMediaInfo();
			if (positionObserver != null) {
				positionObserver.cancel(true);
				positionObserver = null;
			}
			butPlay.setImageResource(R.drawable.av_play);
			break;
		case PLAYING:
			clearStatusMsg();
			loadMediaInfo();
			setupPositionObserver();
			butPlay.setImageResource(R.drawable.av_pause);
			break;
		case PREPARING:
			postStatusMsg(R.string.player_preparing_msg);
			loadMediaInfo();
			break;
		case STOPPED:
			postStatusMsg(R.string.player_stopped_msg);
			break;
		case PREPARED:
			loadMediaInfo();
			postStatusMsg(R.string.player_ready_msg);
			butPlay.setImageResource(R.drawable.av_play);
			break;
		case SEEKING:
			postStatusMsg(R.string.player_seeking_msg);
			break;
		case AWAITING_VIDEO_SURFACE:
			onAwaitingVideoSurface();
			break;
		}
	}

	protected abstract void onAwaitingVideoSurface();

	protected abstract void postStatusMsg(int resId);

	protected abstract void clearStatusMsg();

	protected void onPositionObserverUpdate() {
		int currentPosition = playbackService.getPlayer().getCurrentPosition();
		media.setPosition(currentPosition);
		txtvPosition.setText(Converter.getDurationStringLong(currentPosition));
		txtvLength.setText(Converter.getDurationStringLong(playbackService
				.getPlayer().getDuration()));
		updateProgressbarPosition();
	}

	@SuppressLint("NewApi")
	private void setupPositionObserver() {
		if (positionObserver == null || positionObserver.isCancelled()) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Setting up position observer");
			positionObserver = new MediaPositionObserver() {

				@Override
				protected void onProgressUpdate(Void... v) {
					super.onProgressUpdate();
					onPositionObserverUpdate();
				}

			};
			if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
				positionObserver.executeOnExecutor(
						AsyncTask.THREAD_POOL_EXECUTOR,
						playbackService.getPlayer());
			} else {
				positionObserver.execute(playbackService.getPlayer());
			}

		}
	}

	private void updateProgressbarPosition() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Updating progressbar info");
		MediaPlayer player = playbackService.getPlayer();
		float progress = ((float) player.getCurrentPosition())
				/ player.getDuration();
		sbPosition.setProgress((int) (progress * sbPosition.getMax()));
	}

	protected void loadMediaInfo() {
		if (!mediaInfoLoaded) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Loading media info");
			if (media != null) {
				getSupportActionBar().setSubtitle(media.getItem().getTitle());
				getSupportActionBar().setTitle(
						media.getItem().getFeed().getTitle());
				txtvPosition.setText(Converter.getDurationStringLong((media
						.getPosition())));

				if (!playbackService.isShouldStream()) {
					txtvLength.setText(Converter.getDurationStringLong(media
							.getDuration()));
					float progress = ((float) media.getPosition())
							/ media.getDuration();
					sbPosition.setProgress((int) (progress * sbPosition
							.getMax()));
				}
			}
			mediaInfoLoaded = true;
		}
	}

	protected void setupGUI() {
		setContentView(R.layout.mediaplayer_activity);
		sbPosition = (SeekBar) findViewById(R.id.sbPosition);
		txtvPosition = (TextView) findViewById(R.id.txtvPosition);
		txtvLength = (TextView) findViewById(R.id.txtvLength);
		butPlay = (ImageButton) findViewById(R.id.butPlay);
		butRev = (ImageButton) findViewById(R.id.butRev);
		butFF = (ImageButton) findViewById(R.id.butFF);

		// SEEKBAR SETUP

		sbPosition.setOnSeekBarChangeListener(this);

		// BUTTON SETUP

		butPlay.setOnClickListener(playbuttonListener);

		butFF.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (status == PlayerStatus.PLAYING) {
					playbackService.seekDelta(DEFAULT_SEEK_DELTA);
				}
			}
		});

		butRev.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (status == PlayerStatus.PLAYING) {
					playbackService.seekDelta(-DEFAULT_SEEK_DELTA);
				}
			}
		});
	}

	void handleError(int errorCode) {
		final AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
		errorDialog.setTitle(R.string.error_label);
		errorDialog
				.setMessage(MediaPlayerError.getErrorString(this, errorCode));
		errorDialog.setNeutralButton("OK",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						finish();
					}
				});
		errorDialog.create().show();
	}

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
			invalidateOptionsMenu();

			setupGUI();
			handleStatus();

		} else {
			Log.e(TAG,
					"queryService() was called without an existing connection to playbackservice");
		}
	}

	/** Refreshes the current position of the media file that is playing. */
	public class MediaPositionObserver extends
			AsyncTask<MediaPlayer, Void, Void> {

		private static final String TAG = "MediaPositionObserver";
		private static final int WAITING_INTERVALL = 1000;
		private MediaPlayer player;

		@Override
		protected void onCancelled() {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Task was cancelled");
		}

		@Override
		protected Void doInBackground(MediaPlayer... p) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Background Task started");
			player = p[0];
			try {
				while (player.isPlaying() && !isCancelled()) {
					try {
						Thread.sleep(WAITING_INTERVALL);
					} catch (InterruptedException e) {
						if (AppConfig.DEBUG)
							Log.d(TAG,
									"Thread was interrupted while waiting. Finishing now");
						return null;
					}
					publishProgress();

				}
			} catch (IllegalStateException e) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "player is in illegal state, exiting now");
			}
			if (AppConfig.DEBUG)
				Log.d(TAG, "Background Task finished");
			return null;
		}
	}
	
	// OnSeekbarChangeListener
	private int duration;
	private float prog;

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (fromUser && PlaybackService.isRunning) {
			prog = progress / ((float) seekBar.getMax());
			duration = playbackService.getPlayer().getDuration();
			txtvPosition.setText(Converter
					.getDurationStringLong((int) (prog * duration)));
		}

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// interrupt position Observer, restart later
		if (positionObserver != null) {
			positionObserver.cancel(true);
			positionObserver = null;
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if (PlaybackService.isRunning) {
			playbackService.seek((int) (prog * duration));
			setupPositionObserver();
		}
	}

}
