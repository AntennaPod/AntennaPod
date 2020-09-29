package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import androidx.core.net.ConnectivityManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.storage.DBWriter;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkUtils {
    private NetworkUtils(){}

    private static final String TAG = NetworkUtils.class.getSimpleName();

    private static Context context;

    public static void init(Context context) {
        NetworkUtils.context = context;
    }

    /**
     * Returns true if the device is connected to Wi-Fi and the Wi-Fi filter for
     * automatic downloads is disabled or the device is connected to a Wi-Fi
     * network that is on the 'selected networks' list of the Wi-Fi filter for
     * automatic downloads and false otherwise.
     * */
    public static boolean autodownloadNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(TAG, "Device is connected to Wi-Fi");
                if (networkInfo.isConnected()) {
                    if (!UserPreferences.isEnableAutodownloadWifiFilter()) {
                        Log.d(TAG, "Auto-dl filter is disabled");
                        return true;
                    } else {
                        WifiManager wm = (WifiManager) context.getApplicationContext()
                                .getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wm.getConnectionInfo();
                        List<String> selectedNetworks = Arrays
                                .asList(UserPreferences
                                        .getAutodownloadSelectedNetworks());
                        if (selectedNetworks.contains(Integer.toString(wifiInfo
                                .getNetworkId()))) {
                            Log.d(TAG, "Current network is on the selected networks list");
                            return true;
                        }
                    }
                }
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                Log.d(TAG, "Device is connected to Ethernet");
                if (networkInfo.isConnected()) {
                    return true;
                }
            } else {
                if (!UserPreferences.isAllowMobileAutoDownload()) {
                    Log.d(TAG, "Auto Download not enabled on Mobile");
                    return false;
                }
                if (networkInfo.isRoaming()) {
                    Log.d(TAG, "Roaming on foreign network");
                    return false;
                }
                return true;
            }
        }
        Log.d(TAG, "Network for auto-dl is not available");
        return false;
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
        return ConnectivityManagerCompat.isActiveNetworkMetered(connManager);
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

    /**
     * Returns the SSID of the wifi connection, or <code>null</code> if there is no wifi.
     */
    public static String getWifiSsid() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            return wifiInfo.getSSID();
        }
        return null;
    }

    public static Single<Long> getFeedMediaSizeObservable(FeedMedia media) {
        return Single.create((SingleOnSubscribe<Long>) emitter -> {
            if (!NetworkUtils.isEpisodeHeadDownloadAllowed()) {
                emitter.onSuccess(0L);
                return;
            }
            long size = Integer.MIN_VALUE;
            if (media.isDownloaded()) {
                File mediaFile = new File(media.getLocalMediaUrl());
                if (mediaFile.exists()) {
                    size = mediaFile.length();
                }
            } else if (!media.checkedOnSizeButUnknown()) {
                // only query the network if we haven't already checked

                String url = media.getDownload_url();
                if(TextUtils.isEmpty(url)) {
                    emitter.onSuccess(0L);
                    return;
                }

                OkHttpClient client = AntennapodHttpClient.getHttpClient();
                Request.Builder httpReq = new Request.Builder()
                        .url(url)
                        .header("Accept-Encoding", "identity")
                        .head();
                try {
                    Response response = client.newCall(httpReq.build()).execute();
                    if (response.isSuccessful()) {
                        String contentLength = response.header("Content-Length");
                        try {
                            size = Integer.parseInt(contentLength);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                    }
                } catch (IOException e) {
                    emitter.onSuccess(0L);
                    Log.e(TAG, Log.getStackTraceString(e));
                    return; // better luck next time
                }
            }
            Log.d(TAG, "new size: " + size);
            if (size <= 0) {
                // they didn't tell us the size, but we don't want to keep querying on it
                media.setCheckedOnSizeButUnknown();
            } else {
                media.setSize(size);
            }
            emitter.onSuccess(size);
            DBWriter.setFeedMedia(media);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
