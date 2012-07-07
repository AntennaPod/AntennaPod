package de.podfetcher;

import android.app.Application;
import android.util.Log;
import de.podfetcher.asynctask.FeedImageLoader;
import de.podfetcher.feed.FeedManager;

public class PodcastApp extends Application {
	private static final String TAG = "PodcastApp";
	public static final String PREF_NAME = "PodfetcherPrefs";

	public static final String PREF_PAUSE_ON_HEADSET_DISCONNECT = "prefPauseOnHeadsetDisconnect";
	public static final String PREF_FOLLOW_QUEUE = "prefFollowQueue";
	public static final String PREF_DOWNLOAD_MEDIA_ON_WIFI_ONLY = "prefDownloadMediaOnWifiOnly";
	public static final String PREF_UPDATE_INTERVALL = "prefAutoUpdateIntervall";
	
	private static PodcastApp singleton;

	public static PodcastApp getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;

		FeedManager manager = FeedManager.getInstance();
		manager.loadDBData(getApplicationContext());
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.w(TAG, "Received onLowOnMemory warning. Cleaning image cache...");
		FeedImageLoader.getInstance().wipeImageCache();
	}

}
