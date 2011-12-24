/*
 * Syncs downloaded Feedfiles with Feeds in the database 
 * 
 * 
 * */

package de.podfetcher.service

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.lang.Runtime;

import de.podfetcher.FeedManager;
import de.podfetcher.Feed;
import de.podfetcher.FeedHandler;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.content.Context;

public class FeedSyncService extends Service {
	
	private ScheduledThreadPoolExecutor executor;
	private FeedManager manager;

	@Override
	public void onCreate() {
		executor = new ScheduledThreadPoolExecutor(Runtime.availableProcessors() + 2);
		manager = FeedManager.getInstance();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	} 

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		executor.submit(new FeedSyncThread(handleIntent(intent)));	
	}
	
	/** Extracts a Feed object from the given Intent */
	private Feed handleIntent(Intent intent) {
		Feed feed = manager.getFeed(intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		feed.file_url = requester.getFeedfilePath(context) + requester.getFeedfileName(feed.id);
		return feed;
	}

	/** Takes a single Feed, parses the corresponding file and refreshes information in the manager */
	class FeedSyncThread implements Runnable {

		private Feed feed;

		public FeedSyncThread(Feed feed) {
			this.feed = feed;
		}

		public void run() {
			FeedManager manager = FeedManager.getInstance();
			FeedHandler handler = new FeedHandler();
			
			feed = handler.parseFeed(feed);
		}
		
	}
}
