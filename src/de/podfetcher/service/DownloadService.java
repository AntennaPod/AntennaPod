/** 
 * Registers a DownloadReceiver and waits for all Downloads 
 * to complete, then stops
 * */


package de.podfetcher.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import de.podfetcher.feed.*;
import de.podfetcher.storage.DownloadRequester;
import android.app.Service;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.util.Log;
import android.app.NotificationManager;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

public class DownloadService extends Service {
	private static final String TAG = "DownloadService";

	public static String ACTION_ALL_FEED_DOWNLOADS_COMPLETED = "action.de.podfetcher.storage.all_feed_downloads_completed";
	public static final String ACTION_FEED_SYNC_COMPLETED = "action.de.podfetcher.service.feed_sync_completed";

	private ExecutorService syncExecutor;
	private DownloadRequester requester;
	private FeedManager manager;
	
	/** Needed to determine the duration of a media file */
	private MediaPlayer mediaplayer;


	// Objects for communication
	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	// Message codes
	public static final int MSG_QUERY_DOWNLOADS_LEFT = 1;

	@Override
	public void onCreate() {
		Log.d(TAG, "Service started");
		registerReceiver(downloadReceiver, createIntentFilter());
		syncExecutor = Executors.newSingleThreadExecutor();
		manager = FeedManager.getInstance();
		requester = DownloadRequester.getInstance();
		mediaplayer = new MediaPlayer();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Service shutting down");
		sendBroadcast(new Intent(ACTION_FEED_SYNC_COMPLETED));
		mediaplayer.release();
	}

	private IntentFilter createIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		return filter;
	}

	/** Shuts down Executor service and prepares for shutdown */
	private void initiateShutdown() {
		Log.d(TAG, "Initiating shutdown");
		// Wait until PoolExecutor is done
		Thread waiter = new Thread() {
			@Override
			public void run() {
				syncExecutor.shutdown();
				try {
					Log.d(TAG, "Starting to wait for termination");
					boolean b = syncExecutor.awaitTermination(20L, TimeUnit.SECONDS);
					Log.d(TAG, "Stopping waiting for termination; Result : "+ b);

					stopSelf();
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		waiter.start();
	}

	private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Received 'Download Complete' - message.");
			long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			Feed feed = requester.getFeed(downloadId);
			if(feed != null) {
				handleCompletedFeedDownload(context, feed);
			}else {
				FeedImage image = requester.getFeedImage(downloadId);
				if(image != null) {
					handleCompletedImageDownload(context, image);
				} else {
					FeedMedia media = requester.getFeedMedia(downloadId);
					if (media != null) {
						handleCompletedFeedMediaDownload(context, media);
					}
				}
			}
			queryDownloads();
		}
	};

	/** Check if there's something else to download, otherwise stop */
	private void queryDownloads() {
		if(requester.getNumberOfDownloads() == 0) {
			unregisterReceiver(downloadReceiver);
			initiateShutdown();
		}
	}


	/** Is called whenever a Feed is downloaded */
	private void handleCompletedFeedDownload(Context context, Feed feed) {
		Log.d(TAG, "Handling completed Feed Download");
		syncExecutor.execute(new FeedSyncThread(feed, this, requester));

	}

	/** Is called whenever a Feed-Image is downloaded */
	private void handleCompletedImageDownload(Context context, FeedImage image) {
		Log.d(TAG, "Handling completed Image Download");
		requester.removeFeedImage(image);
		manager.setFeedImage(this, image);
	}

	/** Is called whenever a FeedMedia is downloaded. */
	private void handleCompletedFeedMediaDownload(Context context, FeedMedia media) {
		Log.d(TAG, "Handling completed FeedMedia Download");
		requester.removeFeedMedia(media);
		// Get duration
		try {
			mediaplayer.setDataSource(media.getFile_url());
			mediaplayer.prepare();
		} catch (IOException e) {
			e.printStackTrace();
		}
		media.setDuration(mediaplayer.getDuration());
		Log.d(TAG, "Duration of file is " + media.getDuration());
		mediaplayer.reset();
		manager.setFeedMedia(this, media);
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "Received new Message.");
			switch(msg.what) {
				case MSG_QUERY_DOWNLOADS_LEFT:
					queryDownloads();
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	/** Takes a single Feed, parses the corresponding file and refreshes information in the manager */
	class FeedSyncThread implements Runnable {
		private static final String TAG = "FeedSyncThread";

		private Feed feed;
		private DownloadService service;
		private DownloadRequester requester;

		public FeedSyncThread(Feed feed, DownloadService service, DownloadRequester requester) {
			this.feed = feed;
			this.service = service;
			this.requester = requester;
		}

		public void run() {
			FeedManager manager = FeedManager.getInstance();
			FeedHandler handler = new FeedHandler();

			feed = handler.parseFeed(feed);
			Log.d(TAG, feed.getTitle() + " parsed");
			// Download Feed Image if provided
			if(feed.getImage() != null) {
				Log.d(TAG, "Feed has image; Downloading....");
				requester.downloadImage(service, feed.getImage());
			}
			requester.removeFeed(feed);

			cleanup();

			// Save information of feed in DB
			manager.updateFeed(service, feed);
		}

		/** Delete files that aren't needed anymore */
		private void cleanup() {
			if(new File(feed.getFile_url()).delete()) 
				Log.d(TAG, "Successfully deleted cache file."); else Log.e(TAG, "Failed to delete cache file.");	
			feed.setFile_url(null);
		}

	}
}
