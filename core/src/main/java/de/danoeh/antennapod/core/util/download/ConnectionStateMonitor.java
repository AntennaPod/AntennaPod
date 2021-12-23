package de.danoeh.antennapod.core.util.download;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import de.danoeh.antennapod.core.util.NetworkUtils;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ConnectionStateMonitor extends ConnectivityManager.NetworkCallback {
    private static final String TAG = "ConnectionStateMonitor";
    final NetworkRequest networkRequest;

    public ConnectionStateMonitor() {
            networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
    }

    public void enable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerNetworkCallback(networkRequest, this);
        connectivityManager.addDefaultNetworkActiveListener(() -> {
            Log.d(TAG, "ConnectionStateMonitor::onNetworkActive network connection changed");
            NetworkUtils.networkChangedDetected(context);
        });
        Log.d(TAG, "ConnectionStateMonitor::enable " + connectivityManager.toString());
    }
}