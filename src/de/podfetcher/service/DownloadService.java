/** 
 * Registers a DownloadReceiver and waits for all Downloads 
 * to complete, then stops
 * */


package de.podfetcher.service;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import de.podfetcher.feed.*;
import de.podfetcher.storage.DownloadRequester;
import android.app.Service;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.util.Log;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";

	public static String ACTION_ALL_FEED_DOWNLOADS_COMPLETED = "action.de.podfetcher.storage.all_feed_downloads_completed";
	public static final String ACTION_FEED_SYNC_COMPLETED = "action.de.podfetcher.service.feed_sync_completed";

	private ExecutorService syncExecutor;
	private DownloadRequester requester;
	private FeedManager manager;

	@Override
	public void onCreate() {
		Log.d(TAG, "Service started");
		registerReceiver(downloadReceiver, createIntentFilter());
		syncExecutor = Executors.newSingleThreadExecutor();
		manager = FeedManager.getInstance();
		requester = DownloadRequester.getInstance();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Service shutting down");
		sendBroadcast(new Intent(ACTION_FEED_SYNC_COMPLETED));
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
			long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			Feed feed = requester.getFeed(downloadId);
			if(feed != null) {
				handleCompletedFeedDownload(context, feed);
			}else {
				FeedImage image = requester.getFeedImage(downloadId);
				if(image != null) {
					handleCompletedImageDownload(context, image);
				}
			}

			// Check if there's something else to download, otherwise stop
			if(requester.getNumberOfDownloads() == 0) {
				unregisterReceiver(downloadReceiver);
				initiateShutdown();
			}
		}
	};


	/** Is called whenever a Feed is downloaded */
	private void handleCompletedFeedDownload(Context context, Feed feed) {
	    Log.d(TAG, "Handling completed Feed Download");
		// Get Feed Information
		//feed.setFile_url((new File(requester.getFeedfilePath(context), requester.getFeedfileName(feed.getId()))).toString());
		
		syncExecutor.execute(new FeedSyncThread(feed, this, requester));

	}

	/** Is called whenever a Feed-Image is downloaded */
	private void handleCompletedImageDownload(Context context, FeedImage image) {
	        Log.d(TAG, "Handling completed Image Download");
			requester.removeFeedImage(image);
			//image.setFile_url(requester.getImagefilePath(context) + requester.getImagefileName(image.getId()));
            manager.setFeedImage(this, image);
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
