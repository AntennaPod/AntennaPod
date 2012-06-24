package de.podfetcher.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;

import de.podfetcher.PodcastApp;
import de.podfetcher.R;
import de.podfetcher.feed.FeedManager;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.service.PlaybackService;
import de.podfetcher.service.PlayerStatus;
import de.podfetcher.util.Converter;

public class MediaplayerActivity extends SherlockActivity {
	private final String TAG = "MediaplayerActivity";

	private static final int DEFAULT_SEEK_DELTA = 30000; // Seek-Delta to use
															// when using FF or
															// Rev Buttons

	private PlaybackService playbackService;
	private MediaPositionObserver positionObserver;

	private FeedMedia media;
	private PlayerStatus status;
	private FeedManager manager;

	// Widgets
	private ImageView imgvCover;
	private TextView txtvStatus;
	private TextView txtvPosition;
	private TextView txtvLength;
	private SeekBar sbPosition;
	private ImageButton butPlay;
	private ImageButton butRev;
	private ImageButton butFF;

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
		// TODO Auto-generated method stub
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "Resuming Activity");
		bindToService();

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Creating Activity");
		this.setContentView(R.layout.mediaplayer_activity);
		manager = FeedManager.getInstance();
		setupGUI();
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
			handleError();
			break;
		case PAUSED:
			setStatusMsg(R.string.player_paused_msg, View.VISIBLE);
			loadMediaInfo();
			if (positionObserver != null) {
				positionObserver.cancel(true);
				positionObserver = null;
			}
			butPlay.setImageResource(android.R.drawable.ic_media_play);
			break;
		case PLAYING:
			setStatusMsg(R.string.player_playing_msg, View.INVISIBLE);
			loadMediaInfo();
			setupPositionObserver();
			butPlay.setImageResource(android.R.drawable.ic_media_pause);
			break;
		case PREPARING:
			setStatusMsg(R.string.player_preparing_msg, View.VISIBLE);
			break;
		case STOPPED:
			setStatusMsg(R.string.player_stopped_msg, View.VISIBLE);
			imgvCover.setImageBitmap(null);
			break;
		case PREPARED:
			loadMediaInfo();
			setStatusMsg(R.string.player_ready_msg, View.VISIBLE);
			butPlay.setImageResource(android.R.drawable.ic_media_play);
			break;
		case SEEKING:
			setStatusMsg(R.string.player_seeking_msg, View.VISIBLE);
		}
	}

	private void setStatusMsg(int resId, int visibility) {
		if (visibility == View.VISIBLE) {
			txtvStatus.setText(resId);
		}
		txtvStatus.setVisibility(visibility);
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

					updateProgressbarPosition();
				}

			};
			positionObserver.execute(playbackService.getPlayer());
		}
	}

	private void updateProgressbarPosition() {
		MediaPlayer player = playbackService.getPlayer();
		float progress = ((float) player.getCurrentPosition())
				/ player.getDuration();
		sbPosition.setProgress((int) (progress * sbPosition.getMax()));
	}

	private void loadMediaInfo() {
		if (media != null) {
			MediaPlayer player = playbackService.getPlayer();

			getSupportActionBar().setSubtitle(media.getItem().getTitle());
			getSupportActionBar()
					.setTitle(media.getItem().getFeed().getTitle());

			imgvCover.setImageBitmap(media.getItem().getFeed().getImage()
					.getImageBitmap());

			txtvPosition.setText(Converter.getDurationStringLong((player
					.getCurrentPosition())));
			txtvLength.setText(Converter.getDurationStringLong(player
					.getDuration()));
			if (playbackService != null) {
				updateProgressbarPosition();
			} else {
				sbPosition.setProgress(0);
			}
		}
	}

	private void setupGUI() {
		imgvCover = (ImageView) findViewById(R.id.imgvCover);
		txtvPosition = (TextView) findViewById(R.id.txtvPosition);
		txtvLength = (TextView) findViewById(R.id.txtvLength);
		txtvStatus = (TextView) findViewById(R.id.txtvStatus);
		sbPosition = (SeekBar) findViewById(R.id.sbPosition);
		butPlay = (ImageButton) findViewById(R.id.butPlay);
		butRev = (ImageButton) findViewById(R.id.butRev);
		butFF = (ImageButton) findViewById(R.id.butFF);

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

		butPlay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (status == PlayerStatus.PLAYING) {
					playbackService.pause();
				} else if (status == PlayerStatus.PAUSED
						|| status == PlayerStatus.PREPARED) {
					playbackService.play();
				}
			}
		});

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

	private void handleError() {
		// TODO implement
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			playbackService = ((PlaybackService.LocalBinder) service)
					.getService();
			status = playbackService.getStatus();
			media = playbackService.getMedia();
			registerReceiver(statusUpdate, new IntentFilter(
					PlaybackService.ACTION_PLAYER_STATUS_CHANGED));
			handleStatus();
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

	/** Refreshes the current position of the media file that is playing. */
	public class MediaPositionObserver extends
			AsyncTask<MediaPlayer, Void, Void> {

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

			while (player.isPlaying() && !isCancelled()) {
				try {
					Thread.sleep(WAITING_INTERVALL);
				} catch (InterruptedException e) {
					Log.d(TAG,
							"Thread was interrupted while waiting. Finishing now");
				}
				publishProgress();
			}
			Log.d(TAG, "Background Task finished");
			return null;
		}
	}

}
