package de.danoeh.antennapod.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.util.Log;

/** Tests a connection before downloading something. */
public class ConnectionTester implements Runnable {
	private static final String TAG = "ConnectionTester";
	private String strUrl;
	private Context context;
	private int connectTimeout;
	private int readTimeout;
	private Callback callback;
	private int reason;
	
	public ConnectionTester(String url, Context context, Callback callback) {
		super();
		this.strUrl = url;
		this.context = context;
		this.callback = callback;
		connectTimeout = 500;
		readTimeout = connectTimeout;
	}



	@Override
	public void run() {
		Log.d(TAG, "Testing connection");
		try {		
			URL url = new URL(strUrl);
	        HttpURLConnection con = (HttpURLConnection) url.openConnection();
	        con.connect();
	        callback.onConnectionSuccessful();
	        Log.d(TAG, "Connection seems to work");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			reason = DownloadError.ERROR_CONNECTION_ERROR;
			Log.d(TAG, "Connection failed");
			callback.onConnectionFailure();
		} catch (IOException e) {
			e.printStackTrace();
			reason = DownloadError.ERROR_CONNECTION_ERROR;
			Log.d(TAG, "Connection failed");
			callback.onConnectionFailure();
		}
	}
	
	
	public static abstract class Callback {
		public abstract void onConnectionSuccessful();
		public abstract void onConnectionFailure();
	}
	
	public int getReason() {
		return reason;
	}


}
