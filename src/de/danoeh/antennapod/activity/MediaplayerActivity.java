package de.danoeh.antennapod.activity;

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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TabPageIndicator;

import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.fragment.CoverFragment;
import de.danoeh.antennapod.fragment.ItemDescriptionFragment;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.service.PlayerStatus;
import de.danoeh.antennapod.util.Converter;
import de.danoeh.antennapod.util.FeedItemMenuHandler;
import de.danoeh.antennapod.util.MediaPlayerError;
import de.danoeh.antennapod.util.StorageUtils;

public class MediaplayerActivity extends SherlockFragmentActivity implements
		SurfaceHolder.Callback {

	private final String TAG = "MediaplayerActivity";

	private static final int DEFAULT_SEEK_DELTA = 30000; // Seek-Delta to use
															// when using FF or
															// Rev Buttons
	/** Current screen orientation. */
	private int orientation;

	/** True if video controls are currently visible. */
	private boolean videoControlsShowing = true;
	/** True if media information was loaded. */
	private boolean mediaInfoLoaded = false;

	private PlaybackService playbackService;
	private MediaPositionObserver positionObserver;
	private VideoControlsHider videoControlsToggler;

	private FeedMedia media;
	private PlayerStatus status;
	private FeedManager manager;

	// Widgets
	private CoverFragment coverFragment;
	private ItemDescriptionFragment descriptionFragment;
	private ViewPager viewpager;
	private TabPageIndicator tabs;
	private MediaPlayerPagerAdapter pagerAdapter;
	private VideoView videoview;
	private TextView txtvStatus;
	private TextView txtvPosition;
	private TextView txtvLength;
	private SeekBar sbPosition;
	private ImageButton butPlay;
	private ImageButton butRev;
	private ImageButton butFF;
	private LinearLayout videoOverlay;

	@Override
	protected void onStop() {
		super.onStop();
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
		
		menu.findItem(R.id.support_item).setVisible(media != null && media.getItem().getPaymentLink() != null);
		menu.findItem(R.id.share_link_item).setVisible(media != null && media.getItem().getLink() != null);
		menu.findItem(R.id.visit_website_item).setVisible(media != null && media.getItem().getLink() != null);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		default:
			return FeedItemMenuHandler.onMenuItemClicked(this, item, media.getItem());
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "Resuming Activity");
		StorageUtils.checkStorageAvailability(this);
		bindToService();

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "Configuration changed");
		orientation = newConfig.orientation;
		if (positionObserver != null) {
			positionObserver.cancel(true);
		}
		setupGUI();
		handleStatus();

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (PlaybackService.isRunning && playbackService != null
				&& playbackService.isPlayingVideo()) {
			playbackService.stop();
		}
		if (videoControlsToggler != null) {
			videoControlsToggler.cancel(true);
		}
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Creating Activity");
		StorageUtils.checkStorageAvailability(this);

		orientation = getResources().getConfiguration().orientation;
		manager = FeedManager.getInstance();
		getWindow().setFormat(PixelFormat.TRANSPARENT);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		bindToService();
	}

	private void bindToService() {
		Intent serviceIntent = new Intent(this, PlaybackService.class);
		boolean bound = false;
		if (!PlaybackService.isRunning) {
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
				Log.d(TAG, "No last played media found");
				status = PlayerStatus.STOPPED;
				setupGUI();
				handleStatus();
			}
		} else {
			bound = bindService(serviceIntent, mConnection, 0);
		}
		Log.d(TAG, "Result for service binding: " + bound);
	}

	private void handleStatus() {
		switch (status) {

		case ERROR:
			setStatusMsg(R.string.player_error_msg, View.VISIBLE);
			break;
		case PAUSED:
			setStatusMsg(R.string.player_paused_msg, View.VISIBLE);
			loadMediaInfo();
			if (positionObserver != null) {
				positionObserver.cancel(true);
				positionObserver = null;
			}
			butPlay.setImageResource(R.drawable.av_play);
			break;
		case PLAYING:
			setStatusMsg(R.string.player_playing_msg, View.INVISIBLE);
			loadMediaInfo();
			setupPositionObserver();
			butPlay.setImageResource(R.drawable.av_pause);
			break;
		case PREPARING:
			setStatusMsg(R.string.player_preparing_msg, View.VISIBLE);
			loadMediaInfo();
			break;
		case STOPPED:
			setStatusMsg(R.string.player_stopped_msg, View.VISIBLE);
			break;
		case PREPARED:
			loadMediaInfo();
			setStatusMsg(R.string.player_ready_msg, View.VISIBLE);
			butPlay.setImageResource(R.drawable.av_play);
			break;
		case SEEKING:
			setStatusMsg(R.string.player_seeking_msg, View.VISIBLE);
			break;
		case AWAITING_VIDEO_SURFACE:
			Log.d(TAG, "Preparing video playback");
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
	}

	private void setStatusMsg(int resId, int visibility) {
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			if (visibility == View.VISIBLE) {
				txtvStatus.setText(resId);
			}
			txtvStatus.setVisibility(visibility);
		}
	}

	private void setupPositionObserver() {
		if (positionObserver == null || positionObserver.isCancelled()) {
			positionObserver = new MediaPositionObserver() {

				@Override
				protected void onProgressUpdate(Void... v) {
					super.onProgressUpdate();
					txtvPosition.setText(Converter
							.getDurationStringLong(playbackService.getPlayer()
									.getCurrentPosition()));
					txtvLength.setText(Converter
							.getDurationStringLong(playbackService.getPlayer()
									.getDuration()));
					updateProgressbarPosition();
				}

			};
			positionObserver.execute(playbackService.getPlayer());
		}
	}

	private void updateProgressbarPosition() {
		Log.d(TAG, "Updating progressbar info");
		MediaPlayer player = playbackService.getPlayer();
		float progress = ((float) player.getCurrentPosition())
				/ player.getDuration();
		sbPosition.setProgress((int) (progress * sbPosition.getMax()));
	}

	private void loadMediaInfo() {
		if (!mediaInfoLoaded) {
			Log.d(TAG, "Loading media info");
			if (media != null) {

				if (orientation == Configuration.ORIENTATION_PORTRAIT) {
					getSupportActionBar().setSubtitle(
							media.getItem().getTitle());
					getSupportActionBar().setTitle(
							media.getItem().getFeed().getTitle());
					pagerAdapter.notifyDataSetChanged();

				}

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

	private void setupGUI() {
		setContentView(R.layout.mediaplayer_activity);
		sbPosition = (SeekBar) findViewById(R.id.sbPosition);
		txtvPosition = (TextView) findViewById(R.id.txtvPosition);
		txtvLength = (TextView) findViewById(R.id.txtvLength);
		butPlay = (ImageButton) findViewById(R.id.butPlay);
		butRev = (ImageButton) findViewById(R.id.butRev);
		butFF = (ImageButton) findViewById(R.id.butFF);

		// SEEKBAR SETUP

		sbPosition.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			int duration;
			float prog;

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
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
				playbackService.seek((int) (prog * duration));
				setupPositionObserver();
			}
		});

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

		// PORTRAIT ORIENTATION SETUP

		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			txtvStatus = (TextView) findViewById(R.id.txtvStatus);
			viewpager = (ViewPager) findViewById(R.id.viewpager);
			tabs = (TabPageIndicator) findViewById(R.id.tabs);
			pagerAdapter = new MediaPlayerPagerAdapter(
					getSupportFragmentManager(), 2, this);
			viewpager.setAdapter(pagerAdapter);
			tabs.setViewPager(viewpager);
		} else {
			videoOverlay = (LinearLayout) findViewById(R.id.overlay);
			videoview = (VideoView) findViewById(R.id.videoview);
			videoview.getHolder().addCallback(this);
			videoview.setOnClickListener(playbuttonListener);
			videoview.setOnTouchListener(onVideoviewTouched);
			setupVideoControlsToggler();
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	private OnClickListener playbuttonListener = new OnClickListener() {
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

	private View.OnTouchListener onVideoviewTouched = new View.OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if (videoControlsToggler != null) {
					videoControlsToggler.cancel(true);
				}
				toggleVideoControlsVisibility();
				if (videoControlsShowing) {
					setupVideoControlsToggler();
				}

				return true;
			} else {
				return false;
			}
		}
	};

	private void setupVideoControlsToggler() {
		if (videoControlsToggler != null) {
			videoControlsToggler.cancel(true);
		}
		videoControlsToggler = new VideoControlsHider();
		videoControlsToggler.execute();
	}

	private void toggleVideoControlsVisibility() {
		if (videoControlsShowing) {
			getSupportActionBar().hide();
			videoOverlay.setVisibility(View.GONE);
		} else {
			getSupportActionBar().show();
			videoOverlay.setVisibility(View.VISIBLE);
		}
		videoControlsShowing = !videoControlsShowing;
	}

	private void handleError(int errorCode) {
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

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			playbackService = ((PlaybackService.LocalBinder) service)
					.getService();
			int requestedOrientation;
			status = playbackService.getStatus();
			media = playbackService.getMedia();
			invalidateOptionsMenu();
			registerReceiver(statusUpdate, new IntentFilter(
					PlaybackService.ACTION_PLAYER_STATUS_CHANGED));

			registerReceiver(notificationReceiver, new IntentFilter(
					PlaybackService.ACTION_PLAYER_NOTIFICATION));

			if (playbackService.isPlayingVideo()) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			} else {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			}
			// check if orientation is correct
			if ((requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && orientation == Configuration.ORIENTATION_LANDSCAPE)
					|| (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && orientation == Configuration.ORIENTATION_PORTRAIT)) {
				Log.d(TAG, "Orientation correct");
				setupGUI();
				handleStatus();
			} else {
				Log.d(TAG,
						"Orientation incorrect, waiting for orientation change");
			}

			Log.d(TAG, "Connection to Service established");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			playbackService = null;
			Log.d(TAG, "Disconnected from Service");

		}
	};

	private BroadcastReceiver statusUpdate = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Received statusUpdate Intent.");
			status = playbackService.getStatus();
			handleStatus();
		}
	};

	private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {

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
					try {
						unbindService(mConnection);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
					if (positionObserver != null) {
						positionObserver.cancel(true);
						positionObserver = null;
					}
					mediaInfoLoaded = false;
					bindToService();
					break;
				}

			} else {
				Log.d(TAG, "Bad arguments. Won't handle intent");
			}

		}

	};

	/** Refreshes the current position of the media file that is playing. */
	public class MediaPositionObserver extends
			AsyncTask<MediaPlayer, Void, Void> {

		private static final String TAG = "MediaPositionObserver";
		private static final int WAITING_INTERVALL = 1000;
		private MediaPlayer player;

		@Override
		protected void onCancelled() {
			Log.d(TAG, "Task was cancelled");
		}

		@Override
		protected Void doInBackground(MediaPlayer... p) {
			Log.d(TAG, "Background Task started");
			player = p[0];
			try {
				while (player.isPlaying() && !isCancelled()) {
					try {
						Thread.sleep(WAITING_INTERVALL);
					} catch (InterruptedException e) {
						Log.d(TAG,
								"Thread was interrupted while waiting. Finishing now");
						return null;
					}
					publishProgress();

				}
			} catch (IllegalStateException e) {
				Log.d(TAG, "player is in illegal state, exiting now");
			}
			Log.d(TAG, "Background Task finished");
			return null;
		}
	}

	/** Hides the videocontrols after a certain period of time. */
	public class VideoControlsHider extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onCancelled() {
			videoControlsToggler = null;
		}

		@Override
		protected void onPostExecute(Void result) {
			videoControlsToggler = null;
		}

		private static final int WAITING_INTERVALL = 5000;
		private static final String TAG = "VideoControlsToggler";

		@Override
		protected void onProgressUpdate(Void... values) {
			if (videoControlsShowing) {
				Log.d(TAG, "Hiding video controls");
				getSupportActionBar().hide();
				videoOverlay.setVisibility(View.GONE);
				videoControlsShowing = false;
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Thread.sleep(WAITING_INTERVALL);
			} catch (InterruptedException e) {
				return null;
			}
			publishProgress();
			return null;
		}

	}

	private boolean holderCreated;

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		holder.setFixedSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		holderCreated = true;
		Log.d(TAG, "Videoview holder created");
		if (status == PlayerStatus.AWAITING_VIDEO_SURFACE) {
			playbackService.setVideoSurface(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		holderCreated = false;
	}

	public static class MediaPlayerPagerAdapter extends FragmentPagerAdapter {
		private int numItems;
		private MediaplayerActivity activity;

		private static final int POS_COVER = 0;
		private static final int POS_DESCR = 1;
		private static final int POS_CHAPTERS = 2;

		public MediaPlayerPagerAdapter(FragmentManager fm, int numItems,
				MediaplayerActivity activity) {
			super(fm);
			this.numItems = numItems;
			this.activity = activity;
		}

		@Override
		public Fragment getItem(int position) {
			if (activity.media != null) {
				switch (position) {
				case POS_COVER:
					activity.coverFragment = CoverFragment
							.newInstance(activity.media.getItem());
					return activity.coverFragment;
				case POS_DESCR:
					activity.descriptionFragment = ItemDescriptionFragment
							.newInstance(activity.media.getItem(), true);
					return activity.descriptionFragment;
				default:
					return CoverFragment.newInstance(null);
				}
			} else {
				return CoverFragment.newInstance(null);
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case POS_COVER:
				return activity.getString(R.string.cover_label);
			case POS_DESCR:
				return activity.getString(R.string.description_label);
			default:
				return super.getPageTitle(position);
			}
		}

		@Override
		public int getCount() {
			return numItems;
		}

		@Override
		public int getItemPosition(Object object) {
			return POSITION_UNCHANGED;
		}

	}

}
