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
import android.preference.PreferenceActivity;
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
	public static final String PREF_THEME = "prefTheme";
	public static final String PREF_DATA_FOLDER = "prefDataFolder";

	private static float LOGICAL_DENSITY;

	private static PodcastApp singleton;

	private boolean displayOnlyEpisodes;

	private static long currentlyPlayingMediaId;

	/** Resource id of the currently selected theme. */
	private static int theme;

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
		readThemeValue();
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
		File importDir = getDataFolder(this, OpmlImportActivity.IMPORT_DIR);
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
		} else if (key.equals(PREF_THEME)) {
			readThemeValue();
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

	public static int getThemeResourceId() {
		return theme;
	}

	/** Read value of prefTheme and determine the correct resource id. */
	private void readThemeValue() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		int prefTheme = Integer.parseInt(prefs.getString(PREF_THEME, "0"));
		switch (prefTheme) {
		case 0:
			theme = R.style.Theme_AntennaPod_Light;
			break;
		case 1:
			theme = R.style.Theme_AntennaPod_Dark;
			break;
		}
	}

	/**
	 * Return the folder where the app stores all of its data. This method will
	 * return the standard data folder if none has been set by the user.
	 * 
	 * @param type
	 *            The name of the folder inside the data folder. May be null
	 *            when accessing the root of the data folder.
	 * @return The data folder that has been requested or null if the folder
	 *         could not be created.
	 */
	public static File getDataFolder(Context context, String type) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		String strDir = prefs.getString(PREF_DATA_FOLDER, null);
		if (strDir == null) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Using default data folder");
			return context.getExternalFilesDir(type);
		} else {
			File dataDir = new File(strDir);
			if (!dataDir.exists()) {
				if (!dataDir.mkdir()) {
					Log.w(TAG, "Could not create data folder");
					return null;
				}
			}

			if (type == null) {
				return dataDir;
			} else {
				// handle path separators
				String[] dirs = type.split("/");
				for (int i = 0; i < dirs.length; i++) {
					if (dirs.length > 0) {
						if (i < dirs.length - 1) {
							dataDir = getDataFolder(context, dirs[i]);
							if (dataDir == null) {
								return null;
							}
						}
						type = dirs[i];
					}
				}
				File typeDir = new File(dataDir, type);
				if (!typeDir.exists()) {
					if (dataDir.canWrite()) {
						if (!typeDir.mkdir()) {
							Log.e(TAG, "Could not create data folder named "
									+ type);
							return null;
						}
					}
				}
				return typeDir;
			}

		}
	}

	public void setDataFolder(String dir) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Result from DirectoryChooser: " + dir);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PodcastApp.PREF_DATA_FOLDER, dir);
		editor.commit();
		createImportDirectory();
	}
}
