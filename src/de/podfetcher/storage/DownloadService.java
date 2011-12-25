/** 
 * Registers a DownloadReceiver and waits for all Downloads 
 * to complete, then stops
 * */


package de.podfetcher.storage;

import de.podfetcher.feed.*;
import android.app.Service;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.content.BroadcastReceiver;
import android.content.Context;

public class DownloadService extends Service {

	public static String ACTION_ALL_FEED_DOWNLOADS_COMPLETED = "action.de.podfetcher.storage.all_feed_downloads_completed";
	
	private DownloadRequester requester;
	private FeedManager manager;

	@Override
	public void onCreate() {
		registerReceiver(receiver, createIntentFilter());
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(receiver);
	}

	private IntentFilter createIntentFilter() {
		IntentFilter filter = new IntentFilter();

		filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

		return filter;
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			requester = DownloadRequester.getInstance();
			manager = FeedManager.getInstance();
			Intent item_intent = requester.getItemIntent(id);
			String action = item_intent.getAction();
			if(action.equals(DownloadRequester.ACTION_FEED_DOWNLOAD_COMPLETED)) {
				handleCompletedFeedDownload(context, intent);
				// Notify FeedSyncService about the new Feed
				sendBroadcast(item_intent);
				if(requester.getNumberOfFeedDownloads() == 0) {
					sendBroadcast(new Intent(ACTION_ALL_FEED_DOWNLOADS_COMPLETED));
				}
			} else if(action.equals(DownloadRequester.ACTION_MEDIA_DOWNLOAD_COMPLETED)) {
				requester.removeMediaByID(item_intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
			} else if(action.equals(DownloadRequester.ACTION_IMAGE_DOWNLOAD_COMPLETED)) {
				requester.removeImageByID(item_intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
			}

			// Check if there's something else to download, otherwise stop
			if(requester.getNumberOfDownloads() == 0) {
				stopSelf();
			}
		}
	};


	/** Is called whenever a Feed is Downloaded */
	private void handleCompletedFeedDownload(Context context, Intent intent) {
		FeedHandler handler = new FeedHandler();
		
		requester.removeFeedByID(intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		// Get Feed Information
		Feed feed = manager.getFeed(intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		feed.setFile_url(requester.getFeedfilePath(context) + requester.getFeedfileName(feed.getId()));
		feed = handler.parseFeed(feed);
		// Download Feed Image if provided
		if(feed.getImage() != null) {
			requester.downloadImage(context, feed.getImage());
		}
		// Update Information in Database
		manager.setFeed(context, feed);
	}
}
