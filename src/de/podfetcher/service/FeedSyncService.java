/*
 * Syncs downloaded Feedfiles with Feeds in the database 
 * 
 * 
 * */

package de.podfetcher.service;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.lang.Runtime;

import de.podfetcher.feed.*;
import de.podfetcher.storage.DownloadRequester;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.content.Context;

public class FeedSyncService extends Service {
	
	private volatile ScheduledThreadPoolExecutor executor;
	private FeedManager manager;
	private DownloadRequester requester;

	@Override
	public void onCreate() {
		executor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() + 2);
		manager = FeedManager.getInstance();
		requester = DownloadRequester.getInstance();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	} 

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		executor.submit(new FeedSyncThread(handleIntent(intent), this));	
		return START_STICKY;
	}
	
	/** Extracts a Feed object from the given Intent */
	private Feed handleIntent(Intent intent) {
		Feed feed = manager.getFeed(intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		feed.file_url = requester.getFeedfilePath(this) + requester.getFeedfileName(feed.id);
		return feed;
	}

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
			// Add Feeditems to the database
			for(FeedItem item : feed.items) {
				manager.addFeedItem(service, item);
			}
		}
		
	}
}
