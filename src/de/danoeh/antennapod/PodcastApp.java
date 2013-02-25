package de.danoeh.antennapod;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.asynctask.ImageLoader;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.service.PlaybackService;

/** Main application class. */
public class PodcastApp extends Application implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "PodcastApp";
	public static final String EXPORT_DIR = "export/";

	private static float LOGICAL_DENSITY;

	private static PodcastApp singleton;

	private static long currentlyPlayingMediaId;

	public static PodcastApp getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		LOGICAL_DENSITY = getResources().getDisplayMetrics().density;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		currentlyPlayingMediaId = prefs.getLong(
				PlaybackService.PREF_CURRENTLY_PLAYING_MEDIA,
				PlaybackService.NO_MEDIA_PLAYING);
		prefs.registerOnSharedPreferenceChangeListener(this);
		UserPreferences.createInstance(this);
		EventDistributor.getInstance();
		FeedManager manager = FeedManager.getInstance();
		manager.loadDBData(getApplicationContext());
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.w(TAG, "Received onLowOnMemory warning. Cleaning image cache...");
		ImageLoader.getInstance().wipeImageCache();
	}

	/**
	 * Listens for changes in the 'update intervall'-preference and changes the
	 * alarm if necessary.
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Registered change of application preferences");

		if (key.equals(PlaybackService.PREF_LAST_PLAYED_ID)) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "PREF_LAST_PLAYED_ID changed");
			long mediaId = sharedPreferences.getLong(
					PlaybackService.PREF_AUTODELETE_MEDIA_ID, -1);
			if (mediaId != -1) {
				FeedManager manager = FeedManager.getInstance();
				FeedMedia media = manager.getFeedMedia(mediaId);
				if (media != null) {
					manager.autoDeleteIfPossible(this, media);
				}
			}
		} else if (key.equals(PlaybackService.PREF_CURRENTLY_PLAYING_MEDIA)) {
			long id = sharedPreferences.getLong(
					PlaybackService.PREF_CURRENTLY_PLAYING_MEDIA,
					PlaybackService.NO_MEDIA_PLAYING);
			if (AppConfig.DEBUG)
				Log.d(TAG, "Currently playing media set to " + id);
			if (id != currentlyPlayingMediaId) {
				currentlyPlayingMediaId = id;
			}
		}
	}

	public static float getLogicalDensity() {
		return LOGICAL_DENSITY;
	}

	public static long getCurrentlyPlayingMediaId() {
		return currentlyPlayingMediaId;
	}

	public boolean isLargeScreen() {
		return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE
				|| (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;

	}

}
