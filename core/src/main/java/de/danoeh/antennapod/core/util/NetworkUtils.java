package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.storage.DBWriter;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NetworkUtils {

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
						WifiManager wm = (WifiManager) context
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

	public static boolean isDownloadAllowed() {
		return UserPreferences.isAllowMobileUpdate() || NetworkUtils.connectedToWifi();
	}

	public static boolean connectedToWifi() {
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return mWifi.isConnected();
	}

	public static Observable<Long> getFeedMediaSizeObservable(FeedMedia media) {
        return Observable.create(new Observable.OnSubscribe<Long>() {
            @Override
            public void call(Subscriber<? super Long> subscriber) {
                if (false == NetworkUtils.isDownloadAllowed()) {
                    subscriber.onNext(0L);
                    subscriber.onCompleted();
                    return;
                }
                long size = Integer.MIN_VALUE;
                if (media.isDownloaded()) {
                    File mediaFile = new File(media.getLocalMediaUrl());
                    if (mediaFile.exists()) {
                        size = mediaFile.length();
                    }
                } else if (false == media.checkedOnSizeButUnknown()) {
                    // only query the network if we haven't already checked
                    OkHttpClient client = AntennapodHttpClient.getHttpClient();
                    Request.Builder httpReq = new Request.Builder()
                            .url(media.getDownload_url())
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
                        subscriber.onNext(0L);
                        subscriber.onCompleted();
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
                subscriber.onNext(size);
                subscriber.onCompleted();
                DBWriter.setFeedMedia(context, media);
            }
        })
                .subscribeOn(Schedulers.newThread())
				.observeOn(AndroidSchedulers.mainThread());
    }

}
