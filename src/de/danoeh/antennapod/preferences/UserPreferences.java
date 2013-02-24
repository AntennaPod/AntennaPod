package de.danoeh.antennapod.preferences;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.R.style;
import de.danoeh.antennapod.activity.OpmlImportFromPathActivity;
import de.danoeh.antennapod.receiver.FeedUpdateReceiver;

/**
 * Provides access to preferences set by the user in the settings screen. A
 * private instance of this class must first be instantiated via
 * createInstance() or otherwise every public method will throw an Exception
 * when called.
 */
public class UserPreferences implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	private static final String TAG = "UserPreferences";

	public static final String PREF_PAUSE_ON_HEADSET_DISCONNECT = "prefPauseOnHeadsetDisconnect";
	public static final String PREF_FOLLOW_QUEUE = "prefFollowQueue";
	public static final String PREF_DOWNLOAD_MEDIA_ON_WIFI_ONLY = "prefDownloadMediaOnWifiOnly";
	public static final String PREF_UPDATE_INTERVAL = "prefAutoUpdateIntervall";
	public static final String PREF_MOBILE_UPDATE = "prefMobileUpdate";
	public static final String PREF_AUTO_QUEUE = "prefAutoQueue";
	public static final String PREF_DISPLAY_ONLY_EPISODES = "prefDisplayOnlyEpisodes";
	public static final String PREF_AUTO_DELETE = "prefAutoDelete";
	public static final String PREF_THEME = "prefTheme";
	public static final String PREF_DATA_FOLDER = "prefDataFolder";

	private static UserPreferences instance;
	private Context context;

	// Preferences
	private boolean pauseOnHeadsetDisconnect;
	private boolean followQueue;
	private boolean downloadMediaOnWifiOnly;
	private int updateInterval;
	private boolean allowMobileUpdate;
	private boolean autoQueue;
	private boolean displayOnlyEpisodes;
	private boolean autoDelete;
	private int theme;

	private UserPreferences(Context context) {
		this.context = context;
		loadPreferences();
		createImportDirectory();
		createNoMediaFile();
		PreferenceManager.getDefaultSharedPreferences(context)
				.registerOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Sets up the UserPreferences class.
	 * 
	 * @throws IllegalArgumentException
	 *             if context is null
	 * */
	public static void createInstance(Context context) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Creating new instance of UserPreferences");
		if (context == null)
			throw new IllegalArgumentException("Context must not be null");
		instance = new UserPreferences(context);
	}

	private void loadPreferences() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		pauseOnHeadsetDisconnect = sp.getBoolean(
				PREF_PAUSE_ON_HEADSET_DISCONNECT, true);
		followQueue = sp.getBoolean(PREF_FOLLOW_QUEUE, false);
		downloadMediaOnWifiOnly = sp.getBoolean(
				PREF_DOWNLOAD_MEDIA_ON_WIFI_ONLY, true);
		updateInterval = sp.getInt(PREF_UPDATE_INTERVAL, 0);
		allowMobileUpdate = sp.getBoolean(PREF_MOBILE_UPDATE, false);
		autoQueue = sp.getBoolean(PREF_AUTO_QUEUE, true);
		displayOnlyEpisodes = sp.getBoolean(PREF_DISPLAY_ONLY_EPISODES, false);
		autoDelete = sp.getBoolean(PREF_AUTO_DELETE, false);
		theme = readThemeValue(sp.getString(PREF_THEME, "0"));
	}

	private int readThemeValue(String valueFromPrefs) {
		switch (Integer.parseInt(valueFromPrefs)) {
		case 0:
			return R.style.Theme_AntennaPod_Light;
		case 1:
			return R.style.Theme_AntennaPod_Dark;
		default:
			return R.style.Theme_AntennaPod_Light;
		}
	}

	private static void instanceAvailable() {
		if (instance == null) {
			throw new IllegalStateException(
					"UserPreferences was used before being set up");
		}
	}

	public static boolean isPauseOnHeadsetDisconnect() {
		instanceAvailable();
		return instance.pauseOnHeadsetDisconnect;
	}

	public static boolean isFollowQueue() {
		instanceAvailable();
		return instance.followQueue;
	}

	public static boolean isDownloadMediaOnWifiOnly() {
		instanceAvailable();
		return instance.downloadMediaOnWifiOnly;
	}

	public static int getUpdateInterval() {
		instanceAvailable();
		return instance.updateInterval;
	}

	public static boolean isAllowMobileUpdate() {
		instanceAvailable();
		return instance.allowMobileUpdate;
	}

	public static boolean isAutoQueue() {
		instanceAvailable();
		return instance.autoQueue;
	}

	public static boolean isDisplayOnlyEpisodes() {
		instanceAvailable();
		return instance.displayOnlyEpisodes;
	}

	public static boolean isAutoDelete() {
		instanceAvailable();
		return instance.autoDelete;
	}

	public static int getTheme() {
		instanceAvailable();
		return instance.theme;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Registered change of user preferences. Key: " + key);

		if (key.equals(PREF_DOWNLOAD_MEDIA_ON_WIFI_ONLY)) {
			downloadMediaOnWifiOnly = sp.getBoolean(
					PREF_DOWNLOAD_MEDIA_ON_WIFI_ONLY, true);

		} else if (key.equals(PREF_MOBILE_UPDATE)) {
			allowMobileUpdate = sp.getBoolean(PREF_MOBILE_UPDATE, false);

		} else if (key.equals(PREF_FOLLOW_QUEUE)) {
			followQueue = sp.getBoolean(PREF_FOLLOW_QUEUE, false);

		} else if (key.equals(PREF_UPDATE_INTERVAL)) {
			updateInterval = sp.getInt(PREF_UPDATE_INTERVAL, 0);
			AlarmManager alarmManager = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			int hours = Integer.parseInt(sp
					.getString(PREF_UPDATE_INTERVAL, "0"));
			PendingIntent updateIntent = PendingIntent.getBroadcast(context, 0,
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

		} else if (key.equals(PREF_AUTO_DELETE)) {
			autoDelete = sp.getBoolean(PREF_AUTO_DELETE, false);

		} else if (key.equals(PREF_AUTO_QUEUE)) {
			autoQueue = sp.getBoolean(PREF_AUTO_QUEUE, true);

		} else if (key.equals(PREF_DISPLAY_ONLY_EPISODES)) {
			displayOnlyEpisodes = sp.getBoolean(PREF_DISPLAY_ONLY_EPISODES,
					false);
		} else if (key.equals(PREF_THEME)) {
			theme = readThemeValue(sp.getString(PREF_THEME, ""));
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
		instanceAvailable();
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

	public static void setDataFolder(String dir) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Result from DirectoryChooser: " + dir);
		instanceAvailable();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(instance.context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PREF_DATA_FOLDER, dir);
		editor.commit();
		createImportDirectory();
	}

	/** Create a .nomedia file to prevent scanning by the media scanner. */
	private static void createNoMediaFile() {
		File f = new File(instance.context.getExternalFilesDir(null),
				".nomedia");
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
	private static void createImportDirectory() {
		File importDir = getDataFolder(instance.context,
				OpmlImportFromPathActivity.IMPORT_DIR);
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

}
