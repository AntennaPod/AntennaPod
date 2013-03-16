package de.danoeh.antennapod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.NetworkUtils;

public class ConnectivityActionReceiver extends BroadcastReceiver {
	private static final String TAG = "ConnectivityActionReceiver";

	@Override
	public void onReceive(final Context context, Intent intent) {
		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Received intent");

			if (NetworkUtils.autodownloadNetworkAvailable(context)) {
				if (AppConfig.DEBUG)
					Log.d(TAG,
							"auto-dl network available, starting auto-download");
				new Thread() {
					@Override
					public void run() {
						FeedManager.getInstance()
								.autodownloadUndownloadedItems(context);
					}
				}.start();
			} else { // if new network is Wi-Fi, finish ongoing downloads,
						// otherwise cancel all downloads
				ConnectivityManager cm = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo ni = cm.getActiveNetworkInfo();
				if (ni == null || ni.getType() != ConnectivityManager.TYPE_WIFI) {
					if (AppConfig.DEBUG)
						Log.i(TAG,
								"Device is no longer connected to Wi-Fi. Cancelling ongoing downloads");
					DownloadRequester.getInstance().cancelAllDownloads(context);
				}

			}
		}
	}
}
