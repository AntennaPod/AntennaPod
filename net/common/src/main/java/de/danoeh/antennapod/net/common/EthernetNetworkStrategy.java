package de.danoeh.antennapod.net.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.danoeh.antennapod.storage.preferences.UserPreferences;

import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class WifiNetworkStrategy extends NetworkStrategy {

    @Override
    public boolean isAutoDownloadAllowed() {
        if (UserPreferences.isEnableAutodownloadWifiFilter()) {
            return NetworkUtils.isInAllowedWifiNetwork();
        } else {
            return !NetworkUtils.isNetworkMetered();
        }
    }
}