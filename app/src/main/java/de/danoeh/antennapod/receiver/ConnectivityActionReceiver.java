package de.danoeh.antennapod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.NetworkUtils;

public class ConnectivityActionReceiver extends BroadcastReceiver {
	private static final String TAG = "ConnectivityActionReceiver";

	@Override
	public void onReceive(final Context context, Intent intent) {
		if (StringUtils.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (BuildConfig.DEBUG)
				Log.d(TAG, "Received intent");

			if (NetworkUtils.autodownloadNetworkAvailable(context)) {
				if (BuildConfig.DEBUG)
					Log.d(TAG,
							"auto-dl network available, starting auto-download");
					DBTasks.autodownloadUndownloadedItems(context);
			} else { // if new network is Wi-Fi, finish ongoing downloads,
						// otherwise cancel all downloads
				ConnectivityManager cm = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo ni = cm.getActiveNetworkInfo();
				if (ni == null || ni.getType() != ConnectivityManager.TYPE_WIFI) {
					if (BuildConfig.DEBUG)
						Log.i(TAG,
								"Device is no longer connected to Wi-Fi. Cancelling ongoing downloads");
					DownloadRequester.getInstance().cancelAllDownloads(context);
				}

			}
		}
	}
}
