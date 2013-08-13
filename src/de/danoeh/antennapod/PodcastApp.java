package de.danoeh.antennapod;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.preferences.PlaybackPreferences;
import de.danoeh.antennapod.preferences.UserPreferences;

/** Main application class. */
public class PodcastApp extends Application {

	private static final String TAG = "PodcastApp";
	public static final String EXPORT_DIR = "export/";

	private static float LOGICAL_DENSITY;

	private static PodcastApp singleton;

	public static PodcastApp getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		LOGICAL_DENSITY = getResources().getDisplayMetrics().density;

		UserPreferences.createInstance(this);
		PlaybackPreferences.createInstance(this);
		EventDistributor.getInstance();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.w(TAG, "Received onLowOnMemory warning. Cleaning image cache...");
		ImageLoader.getInstance().wipeImageCache();
	}

	public static float getLogicalDensity() {
		return LOGICAL_DENSITY;
	}

	public boolean isLargeScreen() {
		return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE
				|| (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;

	}
}
