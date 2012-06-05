package de.podfetcher.service;

import java.io.File;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

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
	
	private MediaPlayer player;
	private FeedMedia media;
	private Feed feed;
	private FeedManager manager;
	private PlayerStatus status;

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
			//player.prepareAsync();
		}
		return Service.START_NOT_STICKY;
	}

	private MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
		@Override
		public void onPrepared(MediaPlayer mp) {
			Log.d(TAG, "Resource prepared");
			mp.start();
			setStatus(PlayerStatus.PLAYING);
		}
	};
	
	private void setStatus(PlayerStatus newStatus) {
		status = newStatus;
		sendBroadcast(new Intent(ACTION_PLAYER_STATUS_CHANGED));
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

}
