package de.danoeh.antennapod.core.preferences;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.receiver.FeedUpdateReceiver;

/**
 * Provides access to preferences set by the user in the settings screen. A
 * private instance of this class must first be instantiated via
 * init() or otherwise every public method will throw an Exception
 * when called.
 */
public class UserPreferences {

    public static final String IMPORT_DIR = "import/";

    private static final String TAG = "UserPreferences";

    // User Interface
    public static final String PREF_THEME = "prefTheme";
    public static final String PREF_HIDDEN_DRAWER_ITEMS = "prefHiddenDrawerItems";
    public static final String PREF_DRAWER_FEED_ORDER = "prefDrawerFeedOrder";
    public static final String PREF_DRAWER_FEED_COUNTER = "prefDrawerFeedIndicator";
    public static final String PREF_EXPANDED_NOTIFICATION = "prefExpandNotify";
    public static final String PREF_PERSISTENT_NOTIFICATION = "prefPersistNotify";
    public static final String PREF_SHOW_DOWNLOAD_REPORT = "prefShowDownloadReport";

    // Queue
    public static final String PREF_QUEUE_ADD_TO_FRONT = "prefQueueAddToFront";

    // Playback
    public static final String PREF_PAUSE_ON_HEADSET_DISCONNECT = "prefPauseOnHeadsetDisconnect";
    public static final String PREF_UNPAUSE_ON_HEADSET_RECONNECT = "prefUnpauseOnHeadsetReconnect";
    public static final String PREF_FOLLOW_QUEUE = "prefFollowQueue";
    public static final String PREF_AUTO_DELETE = "prefAutoDelete";
    public static final String PREF_SMART_MARK_AS_PLAYED_SECS = "prefSmartMarkAsPlayedSecs";
    public static final String PREF_PLAYBACK_SPEED_ARRAY = "prefPlaybackSpeedArray";
    public static final String PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS = "prefPauseForFocusLoss";
    public static final String PREF_RESUME_AFTER_CALL = "prefResumeAfterCall";

    // Network
    public static final String PREF_UPDATE_INTERVAL = "prefAutoUpdateIntervall";
    public static final String PREF_MOBILE_UPDATE = "prefMobileUpdate";
    public static final String PREF_PARALLEL_DOWNLOADS = "prefParallelDownloads";
    public static final String PREF_EPISODE_CACHE_SIZE = "prefEpisodeCacheSize";
    public static final String PREF_ENABLE_AUTODL = "prefEnableAutoDl";
    public static final String PREF_ENABLE_AUTODL_ON_BATTERY = "prefEnableAutoDownloadOnBattery";
    public static final String PREF_ENABLE_AUTODL_WIFI_FILTER = "prefEnableAutoDownloadWifiFilter";
    public static final String PREF_AUTODL_SELECTED_NETWORKS = "prefAutodownloadSelectedNetworks";

    // Services
    public static final String PREF_AUTO_FLATTR = "pref_auto_flattr";
    public static final String PREF_AUTO_FLATTR_PLAYED_DURATION_THRESHOLD = "prefAutoFlattrPlayedDurationThreshold";

    // Other
    public static final String PREF_DATA_FOLDER = "prefDataFolder";
    public static final String PREF_IMAGE_CACHE_SIZE = "prefImageCacheSize";

    // Mediaplayer
    public static final String PREF_PLAYBACK_SPEED = "prefPlaybackSpeed";
    private static final String PREF_FAST_FORWARD_SECS = "prefFastForwardSecs";
    private static final String PREF_REWIND_SECS = "prefRewindSecs";
    public static final String PREF_QUEUE_LOCKED = "prefQueueLocked";
    public static final String IMAGE_CACHE_DEFAULT_VALUE = "100";
    public static final int IMAGE_CACHE_SIZE_MINIMUM = 20;

    // Constants
    private static int EPISODE_CACHE_SIZE_UNLIMITED = -1;
    public static int FEED_ORDER_COUNTER = 0;
    public static int FEED_ORDER_ALPHABETICAL = 1;
    public static int FEED_COUNTER_SHOW_NEW_UNPLAYED_SUM = 0;
    public static int FEED_COUNTER_SHOW_NEW = 1;
    public static int FEED_COUNTER_SHOW_UNPLAYED = 2;
    public static int FEED_COUNTER_SHOW_NONE = 3;

    private static Context context;
    private static SharedPreferences prefs;

    /**
     * Sets up the UserPreferences class.
     *
     * @throws IllegalArgumentException if context is null
     */
    public static void init(Context context) {
        Log.d(TAG, "Creating new instance of UserPreferences");
        Validate.notNull(context);

        UserPreferences.context = context;
        UserPreferences.prefs = PreferenceManager.getDefaultSharedPreferences(context);

        createImportDirectory();
        createNoMediaFile();
    }

    /**
     * Returns theme as R.style value
     *
     * @return R.style.Theme_AntennaPod_Light or R.style.Theme_AntennaPod_Dark
     */
    public static int getTheme() {
        return readThemeValue(prefs.getString(PREF_THEME, "0"));
    }

    public static int getNoTitleTheme() {
        int theme = getTheme();
        if (theme == R.style.Theme_AntennaPod_Dark) {
            return R.style.Theme_AntennaPod_Dark_NoTitle;
        } else {
            return R.style.Theme_AntennaPod_Light_NoTitle;
        }
    }

    public static List<String> getHiddenDrawerItems() {
        String hiddenItems = prefs.getString(PREF_HIDDEN_DRAWER_ITEMS, "");
        return new ArrayList<String>(Arrays.asList(StringUtils.split(hiddenItems, ',')));
    }

    public static int getFeedOrder() {
        String value = prefs.getString(PREF_DRAWER_FEED_ORDER, "0");
        return Integer.valueOf(value);
    }

    public static int getFeedCounterSetting() {
        String value = prefs.getString(PREF_DRAWER_FEED_COUNTER, "0");
        return Integer.valueOf(value);
    }

    /**
     * Returns notification priority.
     *
     * @return NotificationCompat.PRIORITY_MAX or NotificationCompat.PRIORITY_DEFAULT
     */
    public static int getNotifyPriority() {
        if (prefs.getBoolean(PREF_EXPANDED_NOTIFICATION, false)) {
            return NotificationCompat.PRIORITY_MAX;
        } else {
            return NotificationCompat.PRIORITY_DEFAULT;
        }
    }

    /**
     * Returns true if notifications are persistent
     *
     * @return {@code true} if notifications are persistent, {@code false}  otherwise
     */
    public static boolean isPersistNotify() {
        return prefs.getBoolean(PREF_PERSISTENT_NOTIFICATION, false);
    }

    /**
     * Returns true if download reports are shown
     *
     * @return {@code true} if download reports are shown, {@code false}  otherwise
     */
    public static boolean showDownloadReport() {
        return prefs.getBoolean(PREF_SHOW_DOWNLOAD_REPORT, true);
    }

    /**
     * Returns {@code true} if new queue elements are added to the front
     *
     * @return {@code true} if new queue elements are added to the front; {@code false}  otherwise
     */
    public static boolean enqueueAtFront() {
        return prefs.getBoolean(PREF_QUEUE_ADD_TO_FRONT, false);
    }

    public static boolean isPauseOnHeadsetDisconnect() {
        return prefs.getBoolean(PREF_PAUSE_ON_HEADSET_DISCONNECT, true);
    }

    public static boolean isUnpauseOnHeadsetReconnect() {
        return prefs.getBoolean(PREF_UNPAUSE_ON_HEADSET_RECONNECT, true);
    }


    public static boolean isFollowQueue() {
        return prefs.getBoolean(PREF_FOLLOW_QUEUE, false);
    }

    public static boolean isAutoDelete() {
        return prefs.getBoolean(PREF_AUTO_DELETE, false);
    }

    public static int getSmartMarkAsPlayedSecs() {
        return Integer.valueOf(prefs.getString(PREF_SMART_MARK_AS_PLAYED_SECS, "30"));
    }

    public static boolean isAutoFlattr() {
        return prefs.getBoolean(PREF_AUTO_FLATTR, false);
    }

    public static String getPlaybackSpeed() {
        return prefs.getString(PREF_PLAYBACK_SPEED, "1.0");
    }

    public static String[] getPlaybackSpeedArray() {
        return readPlaybackSpeedArray(prefs.getString(PREF_PLAYBACK_SPEED_ARRAY, null));
    }

    public static boolean shouldPauseForFocusLoss() {
        return prefs.getBoolean(PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, false);
    }

    public static long getUpdateInterval() {
        String updateInterval = prefs.getString(PREF_UPDATE_INTERVAL, "0");
        if(false == updateInterval.contains(":")) {
            return readUpdateInterval(updateInterval);
        } else {
            return 0;
        }
    }

    public static int[] getUpdateTimeOfDay() {
        String datetime = prefs.getString(PREF_UPDATE_INTERVAL, "");
        if(datetime.length() >= 3 && datetime.contains(":")) {
            String[] parts = datetime.split(":");
            int hourOfDay = Integer.valueOf(parts[0]);
            int minute = Integer.valueOf(parts[1]);
            return new int[] { hourOfDay, minute };
        } else {
            return new int[0];
        }
    }

    public static boolean isAllowMobileUpdate() {
        return prefs.getBoolean(PREF_MOBILE_UPDATE, false);
    }

    public static int getParallelDownloads() {
        return Integer.valueOf(prefs.getString(PREF_PARALLEL_DOWNLOADS, "4"));
    }

    public static int getEpisodeCacheSizeUnlimited() {
        return context.getResources().getInteger(R.integer.episode_cache_size_unlimited);
    }

    /**
     * Returns the capacity of the episode cache. This method will return the
     * negative integer EPISODE_CACHE_SIZE_UNLIMITED if the cache size is set to
     * 'unlimited'.
     */
    public static int getEpisodeCacheSize() {
        return readEpisodeCacheSizeInternal(prefs.getString(PREF_EPISODE_CACHE_SIZE, "20"));
    }

    public static boolean isEnableAutodownload() {
        return prefs.getBoolean(PREF_ENABLE_AUTODL, false);
    }

    public static boolean isEnableAutodownloadOnBattery() {
        return prefs.getBoolean(PREF_ENABLE_AUTODL_ON_BATTERY, true);
    }

    public static boolean isEnableAutodownloadWifiFilter() {
        return prefs.getBoolean(PREF_ENABLE_AUTODL_WIFI_FILTER, false);
    }

    public static int getImageCacheSize() {
        String cacheSizeString = prefs.getString(PREF_IMAGE_CACHE_SIZE, IMAGE_CACHE_DEFAULT_VALUE);
        int cacheSizeInt = Integer.valueOf(cacheSizeString);
        // if the cache size is too small the user won't get any images at all
        // that's bad, force it back to the default.
        if (cacheSizeInt < IMAGE_CACHE_SIZE_MINIMUM) {
            prefs.edit().putString(PREF_IMAGE_CACHE_SIZE, IMAGE_CACHE_DEFAULT_VALUE).apply();
            cacheSizeInt = Integer.valueOf(IMAGE_CACHE_DEFAULT_VALUE);
        }
        int cacheSizeMB = cacheSizeInt * 1024 * 1024;
        return cacheSizeMB;
    }

    public static int getFastFowardSecs() {
        return prefs.getInt(PREF_FAST_FORWARD_SECS, 30);
    }

    public static int getRewindSecs() {
        return prefs.getInt(PREF_REWIND_SECS, 30);
    }


    /**
     * Returns the time after which an episode should be auto-flattr'd in percent of the episode's
     * duration.
     */
    public static float getAutoFlattrPlayedDurationThreshold() {
        return prefs.getFloat(PREF_AUTO_FLATTR_PLAYED_DURATION_THRESHOLD, 0.8f);
    }

    public static String[] getAutodownloadSelectedNetworks() {
        String selectedNetWorks = prefs.getString(PREF_AUTODL_SELECTED_NETWORKS, "");
        return StringUtils.split(selectedNetWorks, ',');
    }

    public static boolean shouldResumeAfterCall() {
        return prefs.getBoolean(PREF_RESUME_AFTER_CALL, true);
    }

    public static boolean isQueueLocked() {
        return prefs.getBoolean(PREF_QUEUE_LOCKED, false);
    }

    public static void setPrefFastForwardSecs(int secs) {
        prefs.edit()
             .putInt(PREF_FAST_FORWARD_SECS, secs)
             .apply();
    }

    public static void setPrefRewindSecs(int secs) {
        prefs.edit()
             .putInt(PREF_REWIND_SECS, secs)
             .apply();
    }

    public static void setPlaybackSpeed(String speed) {
        prefs.edit()
             .putString(PREF_PLAYBACK_SPEED, speed)
             .apply();
    }

    public static void setPlaybackSpeedArray(String[] speeds) {
        JSONArray jsonArray = new JSONArray();
        for (String speed : speeds) {
            jsonArray.put(speed);
        }
        prefs.edit()
             .putString(PREF_PLAYBACK_SPEED_ARRAY, jsonArray.toString())
             .apply();
    }

    public static void setAutodownloadSelectedNetworks(String[] value) {
        prefs.edit()
             .putString(PREF_AUTODL_SELECTED_NETWORKS, StringUtils.join(value, ','))
             .apply();
    }

    /**
     * Sets the update interval value.
     */
    public static void setUpdateInterval(long hours) {
        prefs.edit()
             .putString(PREF_UPDATE_INTERVAL, String.valueOf(hours))
             .apply();
        // when updating with an interval, we assume the user wants
        // to update *now* and then every 'hours' interval thereafter.
        restartUpdateAlarm(true);
    }

    /**
     * Sets the update interval value.
     */
    public static void setUpdateTimeOfDay(int hourOfDay, int minute) {
        prefs.edit()
             .putString(PREF_UPDATE_INTERVAL, hourOfDay + ":" + minute)
             .apply();
        restartUpdateAlarm(false);
    }

    /**
     * Change the auto-flattr settings
     *
     * @param enabled Whether automatic flattring should be enabled at all
     * @param autoFlattrThreshold The percentage of playback time after which an episode should be
     *                            flattrd. Must be a value between 0 and 1 (inclusive)
     * */
    public static void setAutoFlattrSettings( boolean enabled, float autoFlattrThreshold) {
        Validate.inclusiveBetween(0.0, 1.0, autoFlattrThreshold);
        prefs.edit()
             .putBoolean(PREF_AUTO_FLATTR, enabled)
             .putFloat(PREF_AUTO_FLATTR_PLAYED_DURATION_THRESHOLD, autoFlattrThreshold)
             .apply();
    }

    public static void setHiddenDrawerItems(Context context, List<String> items) {
        String str = StringUtils.join(items, ',');
        prefs.edit()
             .putString(PREF_HIDDEN_DRAWER_ITEMS, str)
             .apply();
    }

    public static void setQueueLocked(boolean locked) {
        prefs.edit()
             .putBoolean(PREF_QUEUE_LOCKED, locked)
             .apply();
    }

    private static int readThemeValue(String valueFromPrefs) {
        switch (Integer.parseInt(valueFromPrefs)) {
            case 0:
                return R.style.Theme_AntennaPod_Light;
            case 1:
                return R.style.Theme_AntennaPod_Dark;
            default:
                return R.style.Theme_AntennaPod_Light;
        }
    }

    private static long readUpdateInterval(String valueFromPrefs) {
        int hours = Integer.parseInt(valueFromPrefs);
        return TimeUnit.HOURS.toMillis(hours);
    }

    private static int readEpisodeCacheSizeInternal(String valueFromPrefs) {
        if (valueFromPrefs.equals(context.getString(R.string.pref_episode_cache_unlimited))) {
            return EPISODE_CACHE_SIZE_UNLIMITED;
        } else {
            return Integer.valueOf(valueFromPrefs);
        }
    }

    private static String[] readPlaybackSpeedArray(String valueFromPrefs) {
        String[] selectedSpeeds = null;
        // If this preference hasn't been set yet, return the default options
        if (valueFromPrefs == null) {
            String[] allSpeeds = context.getResources().getStringArray(R.array.playback_speed_values);
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
                Log.e(TAG, "Got JSON error when trying to get speeds from JSONArray");
                e.printStackTrace();
            }
        }
        return selectedSpeeds;
    }


    /**
     * Return the folder where the app stores all of its data. This method will
     * return the standard data folder if none has been set by the user.
     *
     * @param type The name of the folder inside the data folder. May be null
     *             when accessing the root of the data folder.
     * @return The data folder that has been requested or null if the folder
     * could not be created.
     */
    public static File getDataFolder(Context context, String type) {
        String strDir = prefs.getString(PREF_DATA_FOLDER, null);
        if (strDir == null) {
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
                            Log.e(TAG, "Could not create data folder named " + type);
                            return null;
                        }
                    }
                }
                return typeDir;
            }
        }
    }

    public static void setDataFolder(String dir) {
        Log.d(TAG, "Result from DirectoryChooser: " + dir);
        prefs.edit()
             .putString(PREF_DATA_FOLDER, dir)
             .apply();
        createImportDirectory();
    }

    /**
     * Create a .nomedia file to prevent scanning by the media scanner.
     */
    private static void createNoMediaFile() {
        File f = new File(context.getExternalFilesDir(null), ".nomedia");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "Could not create .nomedia file");
                e.printStackTrace();
            }
            Log.d(TAG, ".nomedia file created");
        }
    }

    /**
     * Creates the import directory if it doesn't exist and if storage is
     * available
     */
    private static void createImportDirectory() {
        File importDir = getDataFolder(context, IMPORT_DIR);
        if (importDir != null) {
            if (importDir.exists()) {
                Log.d(TAG, "Import directory already exists");
            } else {
                Log.d(TAG, "Creating import directory");
                importDir.mkdir();
            }
        } else {
            Log.d(TAG, "Could not access external storage.");
        }
    }

    public static void restartUpdateAlarm(boolean now) {
        int[] timeOfDay = getUpdateTimeOfDay();
        Log.d(TAG, "timeOfDay: " + Arrays.toString(timeOfDay));
        if (timeOfDay.length == 2) {
            restartUpdateTimeOfDayAlarm(timeOfDay[0], timeOfDay[1]);
        } else {
            long hours = getUpdateInterval();
            long startTrigger = hours;
            if (now) {
                startTrigger = TimeUnit.SECONDS.toMillis(10);
            }
            restartUpdateIntervalAlarm(startTrigger, hours);
        }
    }

    /**
     * Sets the interval in which the feeds are refreshed automatically
     */
    public static void restartUpdateIntervalAlarm(long triggerAtMillis, long intervalMillis) {
        Log.d(TAG, "Restarting update alarm.");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent updateIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(ClientConfig.applicationCallbacks.getApplicationInstance(), FeedUpdateReceiver.class), 0);
        alarmManager.cancel(updateIntent);
        if (intervalMillis > 0) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + triggerAtMillis,
                    updateIntent);
            Log.d(TAG, "Changed alarm to new interval " + TimeUnit.MILLISECONDS.toHours(intervalMillis) + " h");
        } else {
            Log.d(TAG, "Automatic update was deactivated");
        }
    }

    /**
     * Sets time of day the feeds are refreshed automatically
     */
    public static void restartUpdateTimeOfDayAlarm(int hoursOfDay, int minute) {
        Log.d(TAG, "Restarting update alarm.");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent updateIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(ClientConfig.applicationCallbacks.getApplicationInstance(), FeedUpdateReceiver.class), 0);
        alarmManager.cancel(updateIntent);

        Calendar now = Calendar.getInstance();
        Calendar alarm = (Calendar)now.clone();
        alarm.set(Calendar.HOUR_OF_DAY, hoursOfDay);
        alarm.set(Calendar.MINUTE, minute);
        if (alarm.before(now) || alarm.equals(now)) {
            alarm.add(Calendar.DATE, 1);
        }
        Log.d(TAG, "Alarm set for: " + alarm.toString() + " : " + alarm.getTimeInMillis());
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                alarm.getTimeInMillis(),
                updateIntent);
        Log.d(TAG, "Changed alarm to new time of day " + hoursOfDay + ":" + minute);
    }

    /**
     * Reads episode cache size as it is saved in the episode_cache_size_values array.
     */
    public static int readEpisodeCacheSize(String valueFromPrefs) {
        return readEpisodeCacheSizeInternal(valueFromPrefs);
    }

}
