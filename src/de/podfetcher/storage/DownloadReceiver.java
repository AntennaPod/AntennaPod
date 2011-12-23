package de.podfetcher.storage;

import de.podfetcher.PodcastApp;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadReceiver extends BroadcastReceiver {
	private DownloadRequester requester;
	private FeedManager manager;

	@Override
	public void onReceive(Context context, Intent intent) {
		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
		requester = DownloadRequester.getInstance();
		manager = FeedManager.getInstance();
		Intent item_intent = requester.getItemIntent(id);
		String action = item_intent.getAction();
		if(action.equals(DownloadRequester.ACTION_FEED_DOWNLOAD_COMPLETED)) {
			handleCompletedFeedDownload(context, intent);
		} else if(action.equals(DownloadRequester.ACTION_MEDIA_DOWNLOAD_COMPLETED)) {
			requester.removeMediaByID(item_intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		} else if(action.equals(DownloadRequester.ACTION_IMAGE_DOWNLOAD_COMPLETED)) {
			requester.removeImageByID(item_intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		}
		PodcastApp.getInstance().getApplicationContext().sendBroadcast(item_intent);		
	}

	/** Is called whenever a Feed is Downloaded */
	private void handleCompletedFeedDownload(Context context, Intent intent) {
		RSSHandler handler = new RSSHandler();
		
		requester.removeFeedByID(item_intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		// Get Feed Information
		Feed feed = manager.getFeed(intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		feed.file_url = DownloadRequester.getFeedfilePath() + DownloadRequester.getFeedfileName(feed.id);
		feed = handler.parseFeed(feed);
		// Download Feed Image if provided
		if(feed.image != null) {
			requester.downloadImage(context, feed.image);
		}
		// Update Information in Database
		manager.setFeed(feed);
	}

}
