package de.danoeh.antennapod.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import de.danoeh.antennapod.AppConfig;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

/** Tests a connection before downloading something. */
public class ConnectionTester implements Runnable {
	private static final String TAG = "ConnectionTester";
	private String strUrl;
	private Callback callback;
	private int reason;

	private Handler handler;

	public ConnectionTester(String url, Callback callback) {
		super();
		this.strUrl = url;
		this.callback = callback;
		this.handler = new Handler();
	}

	@Override
	public void run() {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Testing connection");
		try {
			URL url = new URL(strUrl);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.connect();
			handler.post(new Runnable() {
				@Override
				public void run() {
					callback.onConnectionSuccessful();
				}	
			});			if (AppConfig.DEBUG)
				Log.d(TAG, "Connection seems to work");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			reason = DownloadError.ERROR_CONNECTION_ERROR;
			if (AppConfig.DEBUG)
				Log.d(TAG, "Connection failed");
			handler.post(new Runnable() {
				@Override
				public void run() {
					callback.onConnectionFailure(reason);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			reason = DownloadError.ERROR_CONNECTION_ERROR;
			if (AppConfig.DEBUG)
				Log.d(TAG, "Connection failed");
			handler.post(new Runnable() {
				@Override
				public void run() {
					callback.onConnectionFailure(reason);
				}
			});
		}
	}

	public static abstract class Callback {
		public abstract void onConnectionSuccessful();

		public abstract void onConnectionFailure(int reason);
	}

	public int getReason() {
		return reason;
	}

}
