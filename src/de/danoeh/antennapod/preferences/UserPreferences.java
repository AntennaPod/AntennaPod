package de.danoeh.antennapod.preferences;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.gpoddernet.GpodnetService;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
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
	public static final String PREF_DISPLAY_ONLY_EPISODES = "prefDisplayOnlyEpisodes";
	public static final String PREF_AUTO_DELETE = "prefAutoDelete";
	public static final String PREF_AUTO_FLATTR = "pref_auto_flattr";
	public static final String PREF_THEME = "prefTheme";
	public static final String PREF_DATA_FOLDER = "prefDataFolder";
	public static final String PREF_ENABLE_AUTODL = "prefEnableAutoDl";
	public static final String PREF_ENABLE_AUTODL_WIFI_FILTER = "prefEnableAutoDownloadWifiFilter";
	private static final String PREF_AUTODL_SELECTED_NETWORKS = "prefAutodownloadSelectedNetworks";
	public static final String PREF_EPISODE_CACHE_SIZE = "prefEpisodeCacheSize";
	private static final String PREF_PLAYBACK_SPEED = "prefPlaybackSpeed";
	private static final String PREF_PLAYBACK_SPEED_ARRAY = "prefPlaybackSpeedArray";
	public static final String PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS = "prefPauseForFocusLoss";

    // TODO: Make this value configurable
    private static final double PLAYED_DURATION_AUTOFLATTR_THRESHOLD = 0.8;

	private static int EPISODE_CACHE_SIZE_UNLIMITED = -1;

	private static UserPreferences instance;
	private final Context context;

	// Preferences
	private boolean pauseOnHeadsetDisconnect;
	private boolean followQueue;
	private boolean downloadMediaOnWifiOnly;
	private long updateInterval;
	private boolean allowMobileUpdate;
	private boolean displayOnlyEpisodes;
	private boolean autoDelete;
	private boolean autoFlattr;
	private int theme;
	private boolean enableAutodownload;
	private boolean enableAutodownloadWifiFilter;
	private String[] autodownloadSelectedNetworks;
	private int episodeCacheSize;
	private String playbackSpeed;
	private String[] playbackSpeedArray;
	private boolean pauseForFocusLoss;
	private boolean isFreshInstall;

	private UserPreferences(Context context) {
		this.context = context;
		loadPreferences();
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

		createImportDirectory();
		createNoMediaFile();
		PreferenceManager.getDefaultSharedPreferences(context)
				.registerOnSharedPreferenceChangeListener(instance);

	}

	private void loadPreferences() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		EPISODE_CACHE_SIZE_UNLIMITED = context.getResources().getInteger(
				R.integer.episode_cache_size_unlimited);
		pauseOnHeadsetDisconnect = sp.getBoolean(
				PREF_PAUSE_ON_HEADSET_DISCONNECT, true);
		followQueue = sp.getBoolean(PREF_FOLLOW_QUEUE, false);
		downloadMediaOnWifiOnly = sp.getBoolean(
				PREF_DOWNLOAD_MEDIA_ON_WIFI_ONLY, true);
		updateInterval = readUpdateInterval(sp.getString(PREF_UPDATE_INTERVAL,
				"0"));
		allowMobileUpdate = sp.getBoolean(PREF_MOBILE_UPDATE, false);
		displayOnlyEpisodes = sp.getBoolean(PREF_DISPLAY_ONLY_EPISODES, false);
		autoDelete = sp.getBoolean(PREF_AUTO_DELETE, false);
		autoFlattr = sp.getBoolean(PREF_AUTO_FLATTR, false);
		theme = readThemeValue(sp.getString(PREF_THEME, "0"));
		enableAutodownloadWifiFilter = sp.getBoolean(
				PREF_ENABLE_AUTODL_WIFI_FILTER, false);
		autodownloadSelectedNetworks = StringUtils.split(
				sp.getString(PREF_AUTODL_SELECTED_NETWORKS, ""), ',');
		episodeCacheSize = readEpisodeCacheSizeInternal(sp.getString(
				PREF_EPISODE_CACHE_SIZE, "20"));
		enableAutodownload = sp.getBoolean(PREF_ENABLE_AUTODL, false);
		playbackSpeed = sp.getString(PREF_PLAYBACK_SPEED, "1.0");
		playbackSpeedArray = readPlaybackSpeedArray(sp.getString(
				PREF_PLAYBACK_SPEED_ARRAY, null));
		pauseForFocusLoss = sp.getBoolean(PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, false);
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

	private long readUpdateInterval(String valueFromPrefs) {
		int hours = Integer.parseInt(valueFromPrefs);
		return TimeUnit.HOURS.toMillis(hours);
	}

	private int readEpisodeCacheSizeInternal(String valueFromPrefs) {
		if (valueFromPrefs.equals(context
				.getString(R.string.pref_episode_cache_unlimited))) {
			return EPISODE_CACHE_SIZE_UNLIMITED;
		} else {
			return Integer.valueOf(valueFromPrefs);
		}
	}

	private String[] readPlaybackSpeedArray(String valueFromPrefs) {
		String[] selectedSpeeds = null;
		// If this preference hasn't been set yet, return the default options
		if (valueFromPrefs == null) {
			String[] allSpeeds = context.getResources().getStringArray(
					R.array.playback_speed_values);
			List<String> speedList = new LinkedList<String>();
			for (String speedStr : allSpeeds) {
				float speed = Float.parseFloat(speedStr);
				if (speed < 2.0001 && speed * 10 % 1 == 0) {
					speedList.add(speedStr);
				}
			}
			selectedSpeeds = speedList.toArray(new String[speedList.size()]);
		} else {
			try {
				JSONArray jsonArray = new JSONArray(valueFromPrefs);
				selectedSpeeds = new String[jsonArray.length()];
				for (int i = 0; i < jsonArray.length(); i++) {
					selectedSpeeds[i] = jsonArray.getString(i);
				}
			} catch (JSONException e) {
				Log.e(TAG,
						"Got JSON error when trying to get speeds from JSONArray");
				e.printStackTrace();
			}
		}
		return selectedSpeeds;
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

	public static long getUpdateInterval() {
		instanceAvailable();
		return instance.updateInterval;
	}

	public static boolean isAllowMobileUpdate() {
		instanceAvailable();
		return instance.allowMobileUpdate;
	}

	public static boolean isDisplayOnlyEpisodes() {
		instanceAvailable();
		//return instance.displayOnlyEpisodes;
        return false;
	}

	public static boolean isAutoDelete() {
		instanceAvailable();
		return instance.autoDelete;
	}
	
	public static boolean isAutoFlattr() {
		instanceAvailable();
		return instance.autoFlattr;
	}

	public static int getTheme() {
		instanceAvailable();
		return instance.theme;
	}

	public static boolean isEnableAutodownloadWifiFilter() {
		instanceAvailable();
		return instance.enableAutodownloadWifiFilter;
	}

	public static String[] getAutodownloadSelectedNetworks() {
		instanceAvailable();
		return instance.autodownloadSelectedNetworks;
	}

	public static int getEpisodeCacheSizeUnlimited() {
		return EPISODE_CACHE_SIZE_UNLIMITED;
	}

	public static String getPlaybackSpeed() {
		instanceAvailable();
		return instance.playbackSpeed;
	}

	public static String[] getPlaybackSpeedArray() {
		instanceAvailable();
		return instance.playbackSpeedArray;
	}

	/**
	 * Returns the capacity of the episode cache. This method will return the
	 * negative integer EPISODE_CACHE_SIZE_UNLIMITED if the cache size is set to
	 * 'unlimited'.
	 */
	public static int getEpisodeCacheSize() {
		instanceAvailable();
		return instance.episodeCacheSize;
	}

	public static boolean isEnableAutodownload() {
		instanceAvailable();
		return instance.enableAutodownload;
	}
	
	public static boolean shouldPauseForFocusLoss() {
		instanceAvailable();
		return instance.pauseForFocusLoss;
	}

	public static boolean isFreshInstall() {
		instanceAvailable();
		return instance.isFreshInstall;
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
			updateInterval = readUpdateInterval(sp.getString(
					PREF_UPDATE_INTERVAL, "0"));
			restartUpdateAlarm(updateInterval);

		} else if (key.equals(PREF_AUTO_DELETE)) {
			autoDelete = sp.getBoolean(PREF_AUTO_DELETE, false);

		} else if (key.equals(PREF_AUTO_FLATTR)) {
			autoFlattr = sp.getBoolean(PREF_AUTO_FLATTR, false);
		} else if (key.equals(PREF_DISPLAY_ONLY_EPISODES)) {
			displayOnlyEpisodes = sp.getBoolean(PREF_DISPLAY_ONLY_EPISODES,
					false);
		} else if (key.equals(PREF_THEME)) {
			theme = readThemeValue(sp.getString(PREF_THEME, ""));
		} else if (key.equals(PREF_ENABLE_AUTODL_WIFI_FILTER)) {
			enableAutodownloadWifiFilter = sp.getBoolean(
					PREF_ENABLE_AUTODL_WIFI_FILTER, false);
		} else if (key.equals(PREF_AUTODL_SELECTED_NETWORKS)) {
			autodownloadSelectedNetworks = StringUtils.split(
					sp.getString(PREF_AUTODL_SELECTED_NETWORKS, ""), ',');
		} else if (key.equals(PREF_EPISODE_CACHE_SIZE)) {
			episodeCacheSize = readEpisodeCacheSizeInternal(sp.getString(
					PREF_EPISODE_CACHE_SIZE, "20"));
		} else if (key.equals(PREF_ENABLE_AUTODL)) {
			enableAutodownload = sp.getBoolean(PREF_ENABLE_AUTODL, false);
		} else if (key.equals(PREF_PLAYBACK_SPEED)) {
			playbackSpeed = sp.getString(PREF_PLAYBACK_SPEED, "1.0");
		} else if (key.equals(PREF_PLAYBACK_SPEED_ARRAY)) {
			playbackSpeedArray = readPlaybackSpeedArray(sp.getString(
					PREF_PLAYBACK_SPEED_ARRAY, null));
		} else if (key.equals(PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS)) {
			pauseForFocusLoss = sp.getBoolean(PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, false);
		} else if (key.equals(PREF_PAUSE_ON_HEADSET_DISCONNECT)) {
            pauseOnHeadsetDisconnect = sp.getBoolean(PREF_PAUSE_ON_HEADSET_DISCONNECT, true);
        }
	}

	public static void setPlaybackSpeed(String speed) {
		PreferenceManager.getDefaultSharedPreferences(instance.context).edit()
				.putString(PREF_PLAYBACK_SPEED, speed).apply();
	}

	public static void setPlaybackSpeedArray(String[] speeds) {
		JSONArray jsonArray = new JSONArray();
		for (String speed : speeds) {
			jsonArray.put(speed);
		}
		PreferenceManager.getDefaultSharedPreferences(instance.context).edit()
				.putString(PREF_PLAYBACK_SPEED_ARRAY, jsonArray.toString())
				.apply();
	}

	public static void setAutodownloadSelectedNetworks(Context context,
			String[] value) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext())
				.edit();
		editor.putString(PREF_AUTODL_SELECTED_NETWORKS,
				StringUtils.join(value, ','));
		editor.commit();
	}

    /**
     *  Sets the update interval value. Should only be used for testing purposes!
     * */
    public static void setUpdateInterval(Context context, long newValue) {
        instanceAvailable();
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext())
                .edit();
        editor.putString(PREF_UPDATE_INTERVAL,
                String.valueOf(newValue));
        editor.commit();
        instance.updateInterval = newValue;
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

	/**
	 * Updates alarm registered with the AlarmManager service or deactivates it.
	 *
	 * @param millis
	 *            new value to register with AlarmManager. If millis is 0, the
	 *            alarm is deactivated.
	 * */
	public static void restartUpdateAlarm(long millis) {
		instanceAvailable();
		if (AppConfig.DEBUG)
			Log.d(TAG, "Restarting update alarm. New value: " + millis);
		AlarmManager alarmManager = (AlarmManager) instance.context
				.getSystemService(Context.ALARM_SERVICE);
		PendingIntent updateIntent = PendingIntent.getBroadcast(
				instance.context, 0, new Intent(
						FeedUpdateReceiver.ACTION_REFRESH_FEEDS), 0);
		alarmManager.cancel(updateIntent);
		if (millis != 0) {
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, millis, millis,
					updateIntent);
			if (AppConfig.DEBUG)
				Log.d(TAG, "Changed alarm to new interval");
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Automatic update was deactivated");
		}
	}

    /**
     * Reads episode cache size as it is saved in the episode_cache_size_values array.
     * */
    public static int readEpisodeCacheSize(String valueFromPrefs) {
        instanceAvailable();
        return instance.readEpisodeCacheSizeInternal(valueFromPrefs);
    }

    public static double getPlayedDurationAutoflattrThreshold() {
        instanceAvailable();
        return PLAYED_DURATION_AUTOFLATTR_THRESHOLD;
    }
}
