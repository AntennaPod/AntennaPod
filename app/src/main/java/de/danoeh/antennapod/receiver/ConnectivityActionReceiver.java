package de.danoeh.antennapod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;

import de.danoeh.antennapod.core.ClientConfigurator;
import de.danoeh.antennapod.core.util.download.NetworkConnectionChangeHandler;

public class ConnectivityActionReceiver extends BroadcastReceiver {
    private static final String TAG = "ConnectivityActionRecvr";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
            Log.d(TAG, "Received intent");

            ClientConfigurator.initialize(context);
            NetworkConnectionChangeHandler.networkChangedDetected();
        }
    }
}
