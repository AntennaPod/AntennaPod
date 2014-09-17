package de.danoeh.antennapod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DBTasks;

/** Refreshes all feeds when it receives an intent */
public class FeedUpdateReceiver extends BroadcastReceiver {
	private static final String TAG = "FeedUpdateReceiver";
	public static final String ACTION_REFRESH_FEEDS = "de.danoeh.antennapod.feedupdatereceiver.refreshFeeds";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (StringUtils.equals(intent.getAction(), ACTION_REFRESH_FEEDS)) {
			if (BuildConfig.DEBUG)
				Log.d(TAG, "Received intent");
			boolean mobileUpdate = UserPreferences.isAllowMobileUpdate();
			if (mobileUpdate || connectedToWifi(context)) {
				DBTasks.refreshExpiredFeeds(context);
			} else {
				if (BuildConfig.DEBUG)
					Log.d(TAG,
							"Blocking automatic update: no wifi available / no mobile updates allowed");
			}
		}
	}

	private boolean connectedToWifi(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return mWifi.isConnected();
	}

}
