package de.podfetcher.service;

import java.io.File;

import android.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

import de.podfetcher.activity.MediaplayerActivity;
import de.podfetcher.feed.FeedMedia;
import de.podfetcher.feed.Feed;
import de.podfetcher.feed.FeedManager;

/** Controls the MediaPlayer that plays a FeedMedia-file */
public class PlaybackService extends Service {
	/** Logging tag */
	private static final String TAG = "PlaybackService";
	/** Contains the id of the FeedMedia object. */
	public static final String EXTRA_MEDIA_ID = "extra.de.podfetcher.service.mediaId";
	/** Contains the id of the Feed object of the FeedMedia. */
	public static final String EXTRA_FEED_ID = "extra.de.podfetcher.service.feedId";

	public static final String ACTION_PLAYER_STATUS_CHANGED =
			"action.de.podfetcher.service.playerStatusChanged";
	
	private static final int NOTIFICATION_ID = 1;
	private NotificationCompat.Builder notificationBuilder;
	
	private Notification notification;
	private MediaPlayer player;
	private FeedMedia media;
	private Feed feed;
	private FeedManager manager;
	private PlayerStatus status;
	private PositionSaver positionSaver;

	private final IBinder mBinder = new LocalBinder();
	
	public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Service created.");

		manager = FeedManager.getInstance();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (player != null) {
			player.stop();
		}
		long mediaId = intent.getLongExtra(EXTRA_MEDIA_ID, -1);
		long feedId = intent.getLongExtra(EXTRA_FEED_ID, -1);
		if (mediaId == -1 || feedId == -1) {
			Log.e(TAG, "Media ID or Feed ID wasn't provided to the Service.");
		} else {
			feed = manager.getFeed(feedId);
			media = manager.getFeedMedia(mediaId, feed);
			
			player = MediaPlayer.create(this, Uri.fromFile(new File(media.getFile_url())));
			setStatus(PlayerStatus.PREPARING);
			player.setOnPreparedListener(preparedListener);
			Log.d(TAG, "Preparing to play file");
		}
		setupNotification();
		return Service.START_STICKY;
	}
	
	private void setupPositionSaver() {
		if (positionSaver == null) {
			positionSaver = new PositionSaver() {
				@Override
				protected void onCancelled(Void result) {
					super.onCancelled(result);
					positionSaver = null;
				}

				@Override
				protected void onPostExecute(Void result) {
					super.onPostExecute(result);
					positionSaver = null;
				}				
			};
			positionSaver.execute();
		}
	}

	private MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
		@Override
		public void onPrepared(MediaPlayer mp) {
			Log.d(TAG, "Resource prepared");
			setStatus(PlayerStatus.PREPARED);
			play();
		}
	};
	
	public void pause() {
		if (player.isPlaying()) {
			Log.d(TAG, "Pausing playback.");
			player.pause();
			saveCurrentPosition();
			setStatus(PlayerStatus.PAUSED);
		}
	}
	
	public void play() {
		if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
			Log.d(TAG, "Resuming/Starting playback");
			player.start();
			setStatus(PlayerStatus.PLAYING);
			setupPositionSaver();
		} else if (status == PlayerStatus.STOPPED) {
			
		}
	}
	
	private void setStatus(PlayerStatus newStatus) {
		status = newStatus;
		sendBroadcast(new Intent(ACTION_PLAYER_STATUS_CHANGED));
	}
	
	private void setupNotification() {
		PendingIntent pIntent = PendingIntent.getActivity(
				this, 0, new Intent(this, MediaplayerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		
		Bitmap icon = BitmapFactory.decodeResource(null, R.drawable.stat_notify_sdcard);
		notificationBuilder = new NotificationCompat.Builder(this)
			.setContentTitle("Mediaplayer Service")
			.setContentInfo("Click here for more info")
			.setOngoing(true)
			.setContentIntent(pIntent)
			.setLargeIcon(icon)
			.setSmallIcon(R.drawable.stat_notify_sdcard);
			
					
		startForeground(NOTIFICATION_ID, notificationBuilder.getNotification());
		Log.d(TAG, "Notification set up");
	}
	
	/** Seek a specific position from the current position
	 * 	@param delta offset from current position (positive or negative)
	 * */
	public void seekDelta(int delta) {
		seek(player.getCurrentPosition() + delta);
	}
	
	public void seek(int i) {
		Log.d(TAG, "Seeking position " + i);
		player.seekTo(i);
		saveCurrentPosition();
	}
	
	/** Saves the current position of the media file to the DB */
	private synchronized void saveCurrentPosition() {
		Log.d(TAG, "Saving current position to " + player.getCurrentPosition());
		media.setPosition(player.getCurrentPosition());
		manager.setFeedMedia(this, media);
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
	
	/** Periodically saves the position of the media file */
	class PositionSaver extends AsyncTask<Void, Void, Void> {
		private static final int WAITING_INTERVALL = 5000;
		
		@Override
		protected Void doInBackground(Void... params) {
			while (!isCancelled() && player.isPlaying()) {
				try {
					Thread.sleep(WAITING_INTERVALL);
				} catch (InterruptedException e) {
					Log.d(TAG, "Thread was interrupted while waiting. Finishing now...");
					return null;
				}
				saveCurrentPosition();
			}
			return null;
		}
		
	}

}
