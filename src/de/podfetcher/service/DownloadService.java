/** 
 * Registers a DownloadReceiver and waits for all Downloads 
 * to complete, then stops
 * */


package de.podfetcher.service;

import java.io.File;
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
	
	private DownloadRequester requester;
	private FeedManager manager;

	@Override
	public void onCreate() {
		Log.d(this.toString(), "Service started");
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
				handleCompletedFeedDownload(context, item_intent);
				if(requester.getNumberOfFeedDownloads() == 0) {
					sendBroadcast(new Intent(ACTION_ALL_FEED_DOWNLOADS_COMPLETED));
				}
			} else if(action.equals(DownloadRequester.ACTION_MEDIA_DOWNLOAD_COMPLETED)) {
				requester.removeMediaByID(item_intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
			} else if(action.equals(DownloadRequester.ACTION_IMAGE_DOWNLOAD_COMPLETED)) {
				handleCompletedImageDownload(context, item_intent); 
			}

			// Check if there's something else to download, otherwise stop
			if(requester.getNumberOfDownloads() == 0) {
				stopSelf();
			}
		}
	};


	/** Is called whenever a Feed is downloaded */
	private void handleCompletedFeedDownload(Context context, Intent intent) {
		requester.removeFeedByID(intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		// Get Feed Information
		Feed feed = manager.getFeed(intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		feed.setFile_url((new File(requester.getFeedfilePath(context), requester.getFeedfileName(feed.getId()))).toString());
		// Update Information in Database
		manager.setFeed(context, feed);
		// Download Feed Image if provided
		if(feed.getImage() != null) {
			requester.downloadImage(context, feed.getImage());
		}
		context.startService(intent);

	}

	/** Is called whenever a Feed-Image is downloaded */
	private void handleCompletedImageDownload(Context context, Intent intent) {
			requester.removeImageByID(intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
			FeedImage image = manager.getFeedImage(intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
			image.setFile_url(requester.getImagefilePath(context) + requester.getImagefileName(image.getId()));

	}
}
