package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.preferences.UserPreferences;

import java.util.Arrays;
import java.util.List;

public class NetworkUtils {
	private static final String TAG = "NetworkUtils";

	private NetworkUtils() {

	}

	/**
	 * Returns true if the device is connected to Wi-Fi and the Wi-Fi filter for
	 * automatic downloads is disabled or the device is connected to a Wi-Fi
	 * network that is on the 'selected networks' list of the Wi-Fi filter for
	 * automatic downloads and false otherwise.
	 * */
	public static boolean autodownloadNetworkAvailable(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if (networkInfo != null) {
			if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				if (BuildConfig.DEBUG)
					Log.d(TAG, "Device is connected to Wi-Fi");
				if (networkInfo.isConnected()) {
					if (!UserPreferences.isEnableAutodownloadWifiFilter()) {
						if (BuildConfig.DEBUG)
							Log.d(TAG, "Auto-dl filter is disabled");
						return true;
					} else {
						WifiManager wm = (WifiManager) context
								.getSystemService(Context.WIFI_SERVICE);
						WifiInfo wifiInfo = wm.getConnectionInfo();
						List<String> selectedNetworks = Arrays
								.asList(UserPreferences
										.getAutodownloadSelectedNetworks());
						if (selectedNetworks.contains(Integer.toString(wifiInfo
								.getNetworkId()))) {
							if (BuildConfig.DEBUG)
								Log.d(TAG,
										"Current network is on the selected networks list");
							return true;
						}
					}
				}
			}
		}
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Network for auto-dl is not available");
		return false;
	}

    public static boolean networkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
