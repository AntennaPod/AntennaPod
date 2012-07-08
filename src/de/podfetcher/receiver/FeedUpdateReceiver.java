package de.podfetcher.receiver;

import de.podfetcher.feed.FeedManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Refreshes all feeds when it receives an intent */
public class FeedUpdateReceiver extends BroadcastReceiver {
	private static final String TAG = "FeedUpdateReceiver";
	public static final String ACTION_REFRESH_FEEDS = "de.podfetcher.feedupdatereceiver.refreshFeeds";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(ACTION_REFRESH_FEEDS)) {
			Log.d(TAG, "Received intent");
			FeedManager.getInstance().refreshAllFeeds(context);
		}
	}

}
