/*
 * Syncs downloaded Feedfiles with Feeds in the database 
 * 
 * 
 * */

package de.podfetcher.service;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.lang.Runtime;
import java.util.concurrent.TimeUnit;

import de.podfetcher.feed.*;
import de.podfetcher.storage.DownloadRequester;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.IBinder;
import android.content.Context;
import android.util.Log;


public class FeedSyncService extends Service {

	public static final String ACTION_FEED_SYNC_COMPLETED = "action.de.podfetcher.service.feed_sync_completed";
	
	private volatile ScheduledThreadPoolExecutor executor;
	private FeedManager manager;
	private DownloadRequester requester;

	@Override
	public void onCreate() {
		Log.d(this.toString(), "Service started");
		executor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() + 2);
		manager = FeedManager.getInstance();
		requester = DownloadRequester.getInstance();
		registerReceiver(allFeedsDownloaded, new IntentFilter(DownloadService.ACTION_ALL_FEED_DOWNLOADS_COMPLETED));
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	} 

	@Override
	public void onDestroy() {
		sendBroadcast(new Intent(ACTION_FEED_SYNC_COMPLETED));
		unregisterReceiver(allFeedsDownloaded);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		executor.submit(new FeedSyncThread(handleIntent(intent), this));	
		return START_STICKY;
	}
	
	/** Extracts a Feed object from the given Intent */
	private Feed handleIntent(Intent intent) {
		Feed feed = manager.getFeed(intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		feed.setFile_url(requester.getFeedfilePath(this) + requester.getFeedfileName(feed.getId()));
		return feed;
	}

	/** Prepares itself for stopping */
	private void initiateShutdown() {
		Log.d(this.toString(), "Initiating shutdown");
		// Wait until PoolExecutor is done
		Thread waiter = new Thread() {
			@Override
			public void run() {
				executor.shutdown();
				try {
					executor.awaitTermination(20, TimeUnit.SECONDS);
					stopSelf();
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
	}

	BroadcastReceiver allFeedsDownloaded = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			initiateShutdown();	
		}
	};

	/** Takes a single Feed, parses the corresponding file and refreshes information in the manager */
	class FeedSyncThread implements Runnable {

		private Feed feed;
		private FeedSyncService service;

		public FeedSyncThread(Feed feed, FeedSyncService service) {
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
