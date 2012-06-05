package de.podfetcher.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;

import de.podfetcher.R;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.service.PlaybackService;
import de.podfetcher.service.PlayerStatus;
import de.podfetcher.util.Converter;

public class MediaplayerActivity extends SherlockActivity {
	private final String TAG = "MediaplayerActivity";
	
	private PlaybackService playbackService;
	private MediaPositionObserver positionObserver;
	
	private FeedMedia media;
	private PlayerStatus status;
	
	private boolean guiSetup;
	
	
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
		unregisterReceiver(statusUpdate);
		unbindService(mConnection);
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.mediaplayer_activity);
		
		guiSetup = false;
		setupGUI();
		if(!bindService(new Intent(this, PlaybackService.class), mConnection, 0)) {
			status = PlayerStatus.STOPPED;
			handleStatus();
		}
		IntentFilter filter = new IntentFilter(PlaybackService.ACTION_PLAYER_STATUS_CHANGED);
		registerReceiver(statusUpdate, filter);
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
			break;
		case PLAYING:
			setStatusMsg(0, View.INVISIBLE);
			loadMediaInfo();
			setupPositionObserver();
			break;
		case PREPARING:
			setStatusMsg(R.string.player_preparing_msg, View.VISIBLE);
			break;
		}
	}
	
	private void setStatusMsg(int resId, int visibility) {
		if(visibility == View.VISIBLE) {
			txtvStatus.setText(resId);
		}
		txtvStatus.setVisibility(visibility);
	}
	
	private void setupPositionObserver() {
		if (positionObserver == null) {
			positionObserver = new MediaPositionObserver() {

				@Override
				protected void onProgressUpdate(Long... values) {
					super.onProgressUpdate(values);
					txtvPosition.setText(
							Converter.getDurationStringLong(playbackService.getPlayer().getCurrentPosition()));
				}
				
			};
			positionObserver.execute(playbackService);
		}
	}
	
	private void loadMediaInfo() {
		if (media != null) {
			MediaPlayer player = playbackService.getPlayer();
			
			getSupportActionBar().setTitle(media.getItem().getTitle());
			getSupportActionBar().setSubtitle(
					media.getItem().getFeed().getTitle());
			
			imgvCover.setImageBitmap(
					media.getItem().getFeed().getImage().getImageBitmap());
			
			txtvPosition.setText(Converter.getDurationStringLong((player.getCurrentPosition())));
			txtvLength.setText(Converter.getDurationStringLong(player.getDuration()));
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
		
		this.guiSetup = true;
	}
	
	
	private void handleError() {
		// TODO implement
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			playbackService = ((PlaybackService.LocalBinder)service).getService();
			status = playbackService.getStatus();
			media = playbackService.getMedia();
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
		}
	};
	
	
	public class MediaPositionObserver extends AsyncTask<PlaybackService, Long, Long> {
		
		private static final int WAITING_INTERVALL = 1000;
		private long position;
		private long length;
		private PlaybackService service;
		
		@Override
		protected void onCancelled(Long result) {
			Log.d(TAG, "Task was cancelled");
		}
				
		protected Long doInBackground(PlaybackService... services) {
			Log.d(TAG, "Background Task started");
			service = services[0];
			getProgress();
			while(!isCancelled()) {
				try {
					Thread.sleep(WAITING_INTERVALL);
				} catch(InterruptedException e) {
					Log.d(TAG, "Thread was interrupted while waiting");
				}
				
				getProgress();
				publishProgress(position);
			}
			Log.d(TAG, "Background Task finished");
			return Long.valueOf(position);
		}
		
		private void getProgress() {
			FeedMedia media = service.getMedia();
			position = media.getPosition();
			length = media.getDuration();
		}
	}
	
	
	
}
