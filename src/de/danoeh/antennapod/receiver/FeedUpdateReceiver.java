package de.danoeh.antennapod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.feed.FeedManager;

/** Refreshes all feeds when it receives an intent */
public class FeedUpdateReceiver extends BroadcastReceiver {
	private static final String TAG = "FeedUpdateReceiver";
	public static final String ACTION_REFRESH_FEEDS = "de.danoeh.antennapod.feedupdatereceiver.refreshFeeds";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(ACTION_REFRESH_FEEDS)) {
			if (AppConfig.DEBUG) Log.d(TAG, "Received intent");
			boolean mobileUpdate = PreferenceManager
					.getDefaultSharedPreferences(
							context.getApplicationContext()).getBoolean(
							PodcastApp.PREF_MOBILE_UPDATE, false);
			if (mobileUpdate || connectedToWifi(context)) {
				FeedManager.getInstance().refreshAllFeeds(context);
			} else {
				if (AppConfig.DEBUG) Log.d(TAG,
						"Blocking automatic update: no wifi available / no mobile updates allowed");
			}
		}
	}

	private boolean connectedToWifi(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return mWifi.isConnected();
	}

}
