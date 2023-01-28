package de.danoeh.antennapod.core.util.download;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

public class ConnectionStateMonitor
        extends ConnectivityManager.NetworkCallback
        implements ConnectivityManager.OnNetworkActiveListener {
    private static final String TAG = "ConnectionStateMonitor";
    final NetworkRequest networkRequest;

    public ConnectionStateMonitor() {
        networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
    }

    @Override
    public void onNetworkActive() {
        Log.d(TAG, "ConnectionStateMonitor::onNetworkActive network connection changed");
        NetworkConnectionChangeHandler.networkChangedDetected();
    }

    public void enable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerNetworkCallback(networkRequest, this);
        connectivityManager.addDefaultNetworkActiveListener(this);
    }

    public void disable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.unregisterNetworkCallback(this);
        connectivityManager.removeDefaultNetworkActiveListener(this);
    }
}