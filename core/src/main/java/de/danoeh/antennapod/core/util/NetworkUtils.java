package de.danoeh.antennapod.core.util;

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

public class NetworkUtils {
    private static final String REGEX_PATTERN_IP_ADDRESS = "([0-9]{1,3}[\\.]){3}[0-9]{1,3}";

    private NetworkUtils(){}

    private static Context context;

    public static void init(Context context) {
        NetworkUtils.context = context;
    }

    public static boolean isAutoDownloadAllowed() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }
        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            if (UserPreferences.isEnableAutodownloadWifiFilter()) {
                return isInAllowedWifiNetwork();
            } else {
                return !isNetworkMetered();
            }
        } else if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
            return true;
        } else {
            return UserPreferences.isAllowMobileAutoDownload() || !NetworkUtils.isNetworkRestricted();
        }
    }

    public static boolean networkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public static boolean isEpisodeDownloadAllowed() {
        return UserPreferences.isAllowMobileEpisodeDownload() || !NetworkUtils.isNetworkRestricted();
    }

    public static boolean isEpisodeHeadDownloadAllowed() {
        // It is not an image but it is a similarly tiny request
        // that is probably not even considered a download by most users
        return isImageAllowed();
    }

    public static boolean isImageAllowed() {
        return UserPreferences.isAllowMobileImages() || !NetworkUtils.isNetworkRestricted();
    }

    public static boolean isStreamingAllowed() {
        return UserPreferences.isAllowMobileStreaming() || !NetworkUtils.isNetworkRestricted();
    }

    public static boolean isFeedRefreshAllowed() {
        return UserPreferences.isAllowMobileFeedRefresh() || !NetworkUtils.isNetworkRestricted();
    }

    public static boolean isNetworkRestricted() {
        return isNetworkMetered() || isNetworkCellular();
    }

    private static boolean isNetworkMetered() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.isActiveNetworkMetered();
    }

    public static boolean isVpnOverWifi() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities capabilities = connManager.getNetworkCapabilities(connManager.getActiveNetwork());
        return capabilities != null
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
    }

    private static boolean isNetworkCellular() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            Network network = connManager.getActiveNetwork();
            if (network == null) {
                return false; // Nothing connected
            }
            NetworkInfo info = connManager.getNetworkInfo(network);
            if (info == null) {
                return true; // Better be safe than sorry
            }
            NetworkCapabilities capabilities = connManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return true; // Better be safe than sorry
            }
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            // if the default network is a VPN,
            // this method will return the NetworkInfo for one of its underlying networks
            NetworkInfo info = connManager.getActiveNetworkInfo();
            if (info == null) {
                return false; // Nothing connected
            }
            //noinspection deprecation
            return info.getType() == ConnectivityManager.TYPE_MOBILE;
        }
    }

    private static boolean isInAllowedWifiNetwork() {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<String> selectedNetworks = Arrays.asList(UserPreferences.getAutodownloadSelectedNetworks());
        return selectedNetworks.contains(Integer.toString(wm.getConnectionInfo().getNetworkId()));
    }

    public static boolean wasDownloadBlocked(Throwable throwable) {
        String message = throwable.getMessage();
        if (message != null) {
            Pattern pattern = Pattern.compile(REGEX_PATTERN_IP_ADDRESS);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String ip = matcher.group();
                return ip.startsWith("127.") || ip.startsWith("0.");
            }
        }
        if (throwable.getCause() != null) {
            return wasDownloadBlocked(throwable.getCause());
        }
        return false;
    }
}
