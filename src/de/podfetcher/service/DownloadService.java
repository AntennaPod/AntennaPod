/** 
 * Registers a DownloadReceiver and waits for all Downloads 
 * to complete, then stops
 * */


package de.podfetcher.service;

import java.io.File;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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

	public static String ACTION_ALL_FEED_DOWNLOADS_COMPLETED = "action.de.podfetcher.storage.all_feed_downloads_completed";
	public static final String ACTION_FEED_SYNC_COMPLETED = "action.de.podfetcher.service.feed_sync_completed";

	private volatile ScheduledThreadPoolExecutor syncExecutor;
	private DownloadRequester requester;
	private FeedManager manager;

	@Override
	public void onCreate() {
		Log.d(this.toString(), "Service started");
		registerReceiver(downloadReceiver, createIntentFilter());
		syncExecutor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() + 2);
		manager = FeedManager.getInstance();
		requester = DownloadRequester.getInstance();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(this.toString(), "Service shutting down");
		sendBroadcast(new Intent(ACTION_FEED_SYNC_COMPLETED));
	}

	private IntentFilter createIntentFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		return filter;
	}

	/** Shuts down Executor service and prepares for shutdown */
	private void initiateShutdown() {
		Log.d(this.toString(), "Initiating shutdown");
		// Wait until PoolExecutor is done
		Thread waiter = new Thread() {
			@Override
			public void run() {
				syncExecutor.shutdown();
				try {
					syncExecutor.awaitTermination(20, TimeUnit.SECONDS);
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
		requester.removeFeed(feed);
		// Get Feed Information
		feed.setFile_url((new File(requester.getFeedfilePath(context), requester.getFeedfileName(feed.getId()))).toString());
		// Update Information in Database
		manager.setFeed(context, feed);
		// Download Feed Image if provided
		if(feed.getImage() != null) {
			requester.downloadImage(context, feed.getImage());
		}
		syncExecutor.submit(new FeedSyncThread(feed, this));

	}

	/** Is called whenever a Feed-Image is downloaded */
	private void handleCompletedImageDownload(Context context, FeedImage image) {
			requester.removeFeedImage(image);
			image.setFile_url(requester.getImagefilePath(context) + requester.getImagefileName(image.getId()));

	}

	/** Takes a single Feed, parses the corresponding file and refreshes information in the manager */
	class FeedSyncThread implements Runnable {

		private Feed feed;
		private DownloadService service;

		public FeedSyncThread(Feed feed, DownloadService service) {
			this.feed = feed;
			this.service = service;
		}

		public void run() {
			FeedManager manager = FeedManager.getInstance();
			FeedHandler handler = new FeedHandler();
			
			feed = handler.parseFeed(feed);
			Log.d(this.toString(), feed.getTitle() + " parsed");
			// Add Feeditems to the database
			for(FeedItem item : feed.getItems()) {
				manager.addFeedItem(service, item);
			}
		}
		
	}
}
