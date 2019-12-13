package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import androidx.core.net.ConnectivityManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;

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
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null) {
            Log.d(TAG, "Network for auto-dl is not available");
            return false;
        }
        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.d(TAG, "Device is connected to Wi-Fi");
            if (networkInfo.isConnected()) {
                if (!UserPreferences.isEnableAutodownloadWifiFilter()) {
                    Log.d(TAG, "Auto-dl filter is disabled");
                    return true;
                } else if (UserPreferences.getAutodownloadSelectedNetworks().contains(getWifiSsid())) {
                    Log.d(TAG, "Current network is on the selected networks list");
                    return true;
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
        return false;
    }

    public static boolean networkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public static boolean isEpisodeDownloadAllowed() {
        return UserPreferences.isAllowMobileEpisodeDownload() || !NetworkUtils.isNetworkMetered();
    }

    public static boolean isEpisodeHeadDownloadAllowed() {
        // It is not an image but it is a similarly tiny request
        // that is probably not even considered a download by most users
        return isImageAllowed();
    }

    public static boolean isImageAllowed() {
        return UserPreferences.isAllowMobileImages() || !NetworkUtils.isNetworkMetered();
    }

    public static boolean isStreamingAllowed() {
        return UserPreferences.isAllowMobileStreaming() || !NetworkUtils.isNetworkMetered();
    }

    public static boolean isFeedRefreshAllowed() {
        return UserPreferences.isAllowMobileFeedRefresh() || !NetworkUtils.isNetworkMetered();
    }

    private static boolean isNetworkMetered() {
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return ConnectivityManagerCompat.isActiveNetworkMetered(connManager);
    }

    /**
     * Returns the SSID of the wifi connection, or <code>null</code> if there is no wifi.
     */
    public static String getWifiSsid() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            return stripQuotes(wifiInfo.getSSID());
        }
        return null;
    }

    public static String stripQuotes(String ssid) {
        if (ssid != null && ssid.startsWith("\"")) {
            return ssid.substring(1, ssid.length() - 1);
        } else {
            return ssid;
        }
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
