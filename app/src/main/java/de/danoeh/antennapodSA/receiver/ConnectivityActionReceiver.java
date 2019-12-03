package de.danoeh.antennapodSA.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import de.danoeh.antennapodSA.core.ClientConfig;
import de.danoeh.antennapodSA.core.storage.DBTasks;
import de.danoeh.antennapodSA.core.storage.DownloadRequester;
import de.danoeh.antennapodSA.core.util.NetworkUtils;

public class ConnectivityActionReceiver extends BroadcastReceiver {
	private static final String TAG = "ConnectivityActionRecvr";

	@Override
	public void onReceive(final Context context, Intent intent) {
		if (TextUtils.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
			Log.d(TAG, "Received intent");

            ClientConfig.initialize(context);
            if (NetworkUtils.autodownloadNetworkAvailable()) {
				Log.d(TAG, "auto-dl network available, starting auto-download");
					DBTasks.autodownloadUndownloadedItems(context);
			} else { // if new network is Wi-Fi, finish ongoing downloads,
						// otherwise cancel all downloads
				ConnectivityManager cm = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo ni = cm.getActiveNetworkInfo();
				if (ni == null || ni.getType() != ConnectivityManager.TYPE_WIFI) {
					Log.i(TAG, "Device is no longer connected to Wi-Fi. Cancelling ongoing downloads");
					DownloadRequester.getInstance().cancelAllDownloads(context);
				}
			}
		}
	}
}
