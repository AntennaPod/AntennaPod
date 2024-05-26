package de.danoeh.antennapod.net.download.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;

import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;

public class ConnectivityActionReceiver extends BroadcastReceiver {
    private static final String TAG = "ConnectivityActionRecvr";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
            Log.d(TAG, "Received intent");

            if (NetworkUtils.isAutoDownloadAllowed()) {
                Log.d(TAG, "auto-dl network available, starting auto-download");
                AutoDownloadManager.getInstance().autodownloadUndownloadedItems(context);
            } else { // if new network is Wi-Fi, finish ongoing downloads,
                // otherwise cancel all downloads
                if (NetworkUtils.isNetworkRestricted()) {
                    Log.i(TAG, "Device is no longer connected to Wi-Fi. Cancelling ongoing downloads");
                    DownloadServiceInterface.get().cancelAll(context);
                }
            }
        }
    }
}
