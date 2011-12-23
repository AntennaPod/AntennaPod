package de.podfetcher.storage;

import de.podfetcher.PodcastApp;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
		DownloadRequester requester = DownloadRequester.getInstance();
		Intent item_intent = requester.getItemIntent(id);
		String action = item_intent.getAction();
		if(action.equals(DownloadRequester.ACTION_FEED_DOWNLOAD_COMPLETED)) {
			requester.removeFeedByID(item_intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		} else if(action.equals(DownloadRequester.ACTION_MEDIA_DOWNLOAD_COMPLETED)) {
			requester.removeMediaByID(item_intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		} else if(action.equals(DownloadRequester.ACTION_IMAGE_DOWNLOAD_COMPLETED)) {
			requester.removeImageByID(item_intent.getLongExtra(DownloadRequester.EXTRA_ITEM_ID, -1));
		}
		PodcastApp.getInstance().getApplicationContext().sendBroadcast(item_intent);		
	}

}
