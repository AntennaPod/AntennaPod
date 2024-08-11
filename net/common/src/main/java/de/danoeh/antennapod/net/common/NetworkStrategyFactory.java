package de.danoeh.antennapod.net.common;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkStrategyFactory {
    public static NetworkStrategy getNetworkStrategy(NetworkInfo networkInfo) {
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return new WifiNetworkStrategy();
            case ConnectivityManager.TYPE_ETHERNET:
                return new EthernetNetworkStrategy();
            default:
                return new MobileNetworkStrategy();
        }
    }
}