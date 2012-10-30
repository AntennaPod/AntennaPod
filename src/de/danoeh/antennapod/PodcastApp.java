package de.danoeh.antennapod;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.activity.OpmlImportActivity;
import de.danoeh.antennapod.asynctask.FeedImageLoader;
import de.danoeh.antennapod.feed.FeedManager;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.receiver.FeedUpdateReceiver;
import de.danoeh.antennapod.service.PlaybackService;

/** Main application class. */
public class PodcastApp extends Application implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "PodcastApp";
	public static final String EXPORT_DIR = "export/";

	public static final String PREF_PAUSE_ON_HEADSET_DISCONNECT = "prefPauseOnHeadsetDisconnect";
	public static final String PREF_FOLLOW_QUEUE = "prefFollowQueue";
	public static final String PREF_DOWNLOAD_MEDIA_ON_WIFI_ONLY = "prefDownloadMediaOnWifiOnly";
	public static final String PREF_UPDATE_INTERVALL = "prefAutoUpdateIntervall";
	public static final String PREF_MOBILE_UPDATE = "prefMobileUpdate";
	public static final String PREF_AUTO_QUEUE = "prefAutoQueue";
	public static final String PREF_DISPLAY_ONLY_EPISODES = "prefDisplayOnlyEpisodes";
	public static final String PREF_AUTO_DELETE = "prefAutoDelete";

	private static float LOGICAL_DENSITY;

	private static PodcastApp singleton;

	private boolean displayOnlyEpisodes;

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
		displayOnlyEpisodes = prefs.getBoolean(PREF_DISPLAY_ONLY_EPISODES,
				false);
		currentlyPlayingMediaId = prefs.getLong(
				PlaybackService.PREF_CURRENTLY_PLAYING_MEDIA,
				PlaybackService.NO_MEDIA_PLAYING);
		createImportDirectory();
		createNoMediaFile();
		prefs.registerOnSharedPreferenceChangeListener(this);
		FeedManager manager = FeedManager.getInstance();
		manager.loadDBData(getApplicationContext());
	}

	/** Create a .nomedia file to prevent scanning by the media scanner. */
	private void createNoMediaFile() {
		File f = new File(getExternalFilesDir(null), ".nomedia");
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				Log.e(TAG, "Could not create .nomedia file");
				e.printStackTrace();
			}
			if (AppConfig.DEBUG)
				Log.d(TAG, ".nomedia file created");
		}
	}

	/**
	 * Creates the import directory if it doesn't exist and if storage is
	 * available
	 */
	private void createImportDirectory() {
		File importDir = getExternalFilesDir(OpmlImportActivity.IMPORT_DIR);
		if (importDir != null) {
			if (importDir.exists()) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Import directory already exists");
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Creating import directory");
				importDir.mkdir();
			}
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Could not access external storage.");
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.w(TAG, "Received onLowOnMemory warning. Cleaning image cache...");
		FeedImageLoader.getInstance().wipeImageCache();
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
		if (key.equals(PREF_UPDATE_INTERVALL)) {
			AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			int hours = Integer.parseInt(sharedPreferences.getString(
					PREF_UPDATE_INTERVALL, "0"));
			PendingIntent updateIntent = PendingIntent.getBroadcast(this, 0,
					new Intent(FeedUpdateReceiver.ACTION_REFRESH_FEEDS), 0);
			alarmManager.cancel(updateIntent);
			if (hours != 0) {
				long newIntervall = TimeUnit.HOURS.toMillis(hours);
				alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
						newIntervall, newIntervall, updateIntent);
				if (AppConfig.DEBUG)
					Log.d(TAG, "Changed alarm to new intervall");
			} else {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Automatic update was deactivated");
			}
		} else if (key.equals(PREF_DISPLAY_ONLY_EPISODES)) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "PREF_DISPLAY_ONLY_EPISODES changed");
			displayOnlyEpisodes = sharedPreferences.getBoolean(
					PREF_DISPLAY_ONLY_EPISODES, false);
		} else if (key.equals(PlaybackService.PREF_LAST_PLAYED_ID)) {
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

	public boolean displayOnlyEpisodes() {
		return displayOnlyEpisodes;
	}

	public static long getCurrentlyPlayingMediaId() {
		return currentlyPlayingMediaId;
	}

	public boolean isLargeScreen() {
		return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE
				|| (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;

	}
}
