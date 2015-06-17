package de.danoeh.antennapod.core.preferences;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.receiver.FeedUpdateReceiver;

/**
 * Provides access to preferences set by the user in the settings screen. A
 * private instance of this class must first be instantiated via
 * createInstance() or otherwise every public method will throw an Exception
 * when called.
 */
public class UserPreferences implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String IMPORT_DIR = "import/";

    private static final String TAG = "UserPreferences";

    // User Infercasce
    public static final String PREF_THEME = "prefTheme";
    public static final String PREF_HIDDEN_DRAWER_ITEMS = "prefHiddenDrawerItems";
    public static final String PREF_EXPANDED_NOTIFICATION = "prefExpandNotify";
    public static final String PREF_PERSISTENT_NOTIFICATION = "prefPersistNotify";

    // Queue
    public static final String PREF_QUEUE_ADD_TO_FRONT = "prefQueueAddToFront";

    // Playback
    public static final String PREF_PAUSE_ON_HEADSET_DISCONNECT = "prefPauseOnHeadsetDisconnect";
    public static final String PREF_UNPAUSE_ON_HEADSET_RECONNECT = "prefUnpauseOnHeadsetReconnect";
    public static final String PREF_FOLLOW_QUEUE = "prefFollowQueue";
    public static final String PREF_AUTO_DELETE = "prefAutoDelete";
    public static final String PREF_SMART_MARK_AS_PLAYED_SECS = "prefSmartMarkAsPlayedSecs";
    private static final String PREF_PLAYBACK_SPEED_ARRAY = "prefPlaybackSpeedArray";
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

    // Mediaplayer
    public static final String PREF_PLAYBACK_SPEED = "prefPlaybackSpeed";
    private static final String PREF_FAST_FORWARD_SECS = "prefFastForwardSecs";
    private static final String PREF_REWIND_SECS = "prefRewindSecs";
    public static final String PREF_QUEUE_LOCKED = "prefQueueLocked";

    // TODO: Make this value configurable
    private static final float PREF_AUTO_FLATTR_PLAYED_DURATION_THRESHOLD_DEFAULT = 0.8f;

    private static int EPISODE_CACHE_SIZE_UNLIMITED = -1;

    private static UserPreferences instance;
    private final Context context;

    // User Interface
    private int theme;
    private List<String> hiddenDrawerItems;
    private int notifyPriority;
    private boolean persistNotify;

    // Queue
    private boolean enqueueAtFront;

    // Playback
    private boolean pauseOnHeadsetDisconnect;
    private boolean unpauseOnHeadsetReconnect;
    private boolean followQueue;
    private boolean autoDelete;
    private int smartMarkAsPlayedSecs;
    private String[] playbackSpeedArray;
    private boolean pauseForFocusLoss;
    private boolean resumeAfterCall;

    // Network
    private long updateInterval;
    private boolean allowMobileUpdate;
    private int parallelDownloads;
    private int episodeCacheSize;
    private boolean enableAutodownload;
    private boolean enableAutodownloadOnBattery;
    private boolean enableAutodownloadWifiFilter;
    private String[] autodownloadSelectedNetworks;

    // Services
    private boolean autoFlattr;
    private float autoFlattrPlayedDurationThreshold;

    // Settings somewhere in the GUI
    private String playbackSpeed;
    private int fastForwardSecs;
    private int rewindSecs;
    private boolean queueLocked;


    private UserPreferences(Context context) {
        this.context = context;
        loadPreferences();
    }

    /**
     * Sets up the UserPreferences class.
     *
     * @throws IllegalArgumentException if context is null
     */
    public static void createInstance(Context context) {
        Log.d(TAG, "Creating new instance of UserPreferences");
        Validate.notNull(context);

        instance = new UserPreferences(context);

        createImportDirectory();
        createNoMediaFile();
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(instance);

    }

    private void loadPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        // User Interface
        theme = readThemeValue(sp.getString(PREF_THEME, "0"));
        if (sp.getBoolean(PREF_EXPANDED_NOTIFICATION, false)) {
            notifyPriority = NotificationCompat.PRIORITY_MAX;
        } else {
            notifyPriority = NotificationCompat.PRIORITY_DEFAULT;
        }
        hiddenDrawerItems = Arrays.asList(StringUtils.split(sp.getString(PREF_HIDDEN_DRAWER_ITEMS, ""), ','));
        persistNotify = sp.getBoolean(PREF_PERSISTENT_NOTIFICATION, false);

        // Queue
        enqueueAtFront = sp.getBoolean(PREF_QUEUE_ADD_TO_FRONT, false);

        // Playback
        pauseOnHeadsetDisconnect = sp.getBoolean(PREF_PAUSE_ON_HEADSET_DISCONNECT, true);
        unpauseOnHeadsetReconnect = sp.getBoolean(PREF_UNPAUSE_ON_HEADSET_RECONNECT, true);
        followQueue = sp.getBoolean(PREF_FOLLOW_QUEUE, false);
        autoDelete = sp.getBoolean(PREF_AUTO_DELETE, false);
        smartMarkAsPlayedSecs = Integer.valueOf(sp.getString(PREF_SMART_MARK_AS_PLAYED_SECS, "30"));
        playbackSpeedArray = readPlaybackSpeedArray(sp.getString(
                PREF_PLAYBACK_SPEED_ARRAY, null));
        pauseForFocusLoss = sp.getBoolean(PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, false);

        // Network
        updateInterval = readUpdateInterval(sp.getString(PREF_UPDATE_INTERVAL, "0"));
        allowMobileUpdate = sp.getBoolean(PREF_MOBILE_UPDATE, false);
        parallelDownloads = Integer.valueOf(sp.getString(PREF_PARALLEL_DOWNLOADS, "6"));
        EPISODE_CACHE_SIZE_UNLIMITED = context.getResources().getInteger(
                R.integer.episode_cache_size_unlimited);
        episodeCacheSize = readEpisodeCacheSizeInternal(sp.getString(PREF_EPISODE_CACHE_SIZE, "20"));
        enableAutodownload = sp.getBoolean(PREF_ENABLE_AUTODL, false);
        enableAutodownloadOnBattery = sp.getBoolean(PREF_ENABLE_AUTODL_ON_BATTERY, true);
        enableAutodownloadWifiFilter = sp.getBoolean(PREF_ENABLE_AUTODL_WIFI_FILTER, false);
        autodownloadSelectedNetworks = StringUtils.split(
                sp.getString(PREF_AUTODL_SELECTED_NETWORKS, ""), ',');

        // Services
        autoFlattr = sp.getBoolean(PREF_AUTO_FLATTR, false);
        autoFlattrPlayedDurationThreshold = sp.getFloat(PREF_AUTO_FLATTR_PLAYED_DURATION_THRESHOLD,
                PREF_AUTO_FLATTR_PLAYED_DURATION_THRESHOLD_DEFAULT);

        // MediaPlayer
        playbackSpeed = sp.getString(PREF_PLAYBACK_SPEED, "1.0");
        fastForwardSecs = sp.getInt(PREF_FAST_FORWARD_SECS, 30);
        rewindSecs = sp.getInt(PREF_REWIND_SECS, 30);
        queueLocked = sp.getBoolean(PREF_QUEUE_LOCKED, false);
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
                Log.e(TAG, "Got JSON error when trying to get speeds from JSONArray");
                e.printStackTrace();
            }
        }
        return selectedSpeeds;
    }

    private static void instanceAvailable() {
        if (instance == null) {
            throw new IllegalStateException("UserPreferences was used before being set up");
        }
    }

    /**
     * Returns theme as R.style value
     *
     * @return R.style.Theme_AntennaPod_Light or R.style.Theme_AntennaPod_Dark
     */
    public static int getTheme() {
        instanceAvailable();
        return instance.theme;
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
        instanceAvailable();
        return new ArrayList<String>(instance.hiddenDrawerItems);
    }

    /**
     * Returns notification priority.
     *
     * @return NotificationCompat.PRIORITY_MAX or NotificationCompat.PRIORITY_DEFAULT
     */
    public static int getNotifyPriority() {
        instanceAvailable();
        return instance.notifyPriority;
    }

    /**
     * Returns true if notifications are persistent
     *
     * @return {@code true} if notifications are persistent, {@code false}  otherwise
     */
    public static boolean isPersistNotify() {
        instanceAvailable();
        return instance.persistNotify;
    }

    /**
     * Returns {@code true} if new queue elements are added to the front
     *
     * @return {@code true} if new queue elements are added to the front; {@code false}  otherwise
     */
    public static boolean enqueueAtFront() {
        instanceAvailable();
        return instance.enqueueAtFront;
    }

    public static boolean isPauseOnHeadsetDisconnect() {
        instanceAvailable();
        return instance.pauseOnHeadsetDisconnect;
    }

    public static boolean isUnpauseOnHeadsetReconnect() {
        instanceAvailable();
        return instance.unpauseOnHeadsetReconnect;
    }


    public static boolean isFollowQueue() {
        instanceAvailable();
        return instance.followQueue;
    }

    public static boolean isAutoDelete() {
        instanceAvailable();
        return instance.autoDelete;
    }

    public static int getSmartMarkAsPlayedSecs() {
        instanceAvailable();
        return instance.smartMarkAsPlayedSecs;
    }

    public static boolean isAutoFlattr() {
        instanceAvailable();
        return instance.autoFlattr;
    }

    public static String getPlaybackSpeed() {
        instanceAvailable();
        return instance.playbackSpeed;
    }

    public static String[] getPlaybackSpeedArray() {
        instanceAvailable();
        return instance.playbackSpeedArray;
    }

    public static boolean shouldPauseForFocusLoss() {
        instanceAvailable();
        return instance.pauseForFocusLoss;
    }

    public static long getUpdateInterval() {
        instanceAvailable();
        return instance.updateInterval;
    }

    public static boolean isAllowMobileUpdate() {
        instanceAvailable();
        return instance.allowMobileUpdate;
    }

    public static int getParallelDownloads() {
        instanceAvailable();
        return instance.parallelDownloads;
    }

    public static int getEpisodeCacheSizeUnlimited() {
        return EPISODE_CACHE_SIZE_UNLIMITED;
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

    public static boolean isEnableAutodownloadOnBattery() {
        instanceAvailable();
        return instance.enableAutodownloadOnBattery;
    }

    public static boolean isEnableAutodownloadWifiFilter() {
        instanceAvailable();
        return instance.enableAutodownloadWifiFilter;
    }

    public static int getFastFowardSecs() {
        instanceAvailable();
        return instance.fastForwardSecs;
    }

    public static int getRewindSecs() {
        instanceAvailable();
        return instance.rewindSecs;
    }


    /**
     * Returns the time after which an episode should be auto-flattr'd in percent of the episode's
     * duration.
     */
    public static float getAutoFlattrPlayedDurationThreshold() {
        instanceAvailable();
        return instance.autoFlattrPlayedDurationThreshold;
    }

    public static String[] getAutodownloadSelectedNetworks() {
        instanceAvailable();
        return instance.autodownloadSelectedNetworks;
    }

    public static boolean shouldResumeAfterCall() {
        instanceAvailable();
        return instance.resumeAfterCall;
    }

    public static boolean isQueueLocked() {
        instanceAvailable();
        return instance.queueLocked;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        Log.d(TAG, "Registered change of user preferences. Key: " + key);
        switch(key) {
            // User Interface
            case PREF_THEME:
                theme = readThemeValue(sp.getString(PREF_THEME, ""));
                break;
            case PREF_HIDDEN_DRAWER_ITEMS:
                hiddenDrawerItems = Arrays.asList(StringUtils.split(sp.getString(PREF_HIDDEN_DRAWER_ITEMS, ""), ','));
                break;
            case PREF_EXPANDED_NOTIFICATION:
                if (sp.getBoolean(PREF_EXPANDED_NOTIFICATION, false)) {
                    notifyPriority = NotificationCompat.PRIORITY_MAX;
                } else {
                    notifyPriority = NotificationCompat.PRIORITY_DEFAULT;
                }
                break;
            case PREF_PERSISTENT_NOTIFICATION:
                persistNotify = sp.getBoolean(PREF_PERSISTENT_NOTIFICATION, false);
                break;
            // Queue
            case PREF_QUEUE_ADD_TO_FRONT:
                enqueueAtFront = sp.getBoolean(PREF_QUEUE_ADD_TO_FRONT, false);
                break;
            // Playback
            case PREF_PAUSE_ON_HEADSET_DISCONNECT:
                pauseOnHeadsetDisconnect = sp.getBoolean(PREF_PAUSE_ON_HEADSET_DISCONNECT, true);
                break;
            case PREF_UNPAUSE_ON_HEADSET_RECONNECT:
                unpauseOnHeadsetReconnect = sp.getBoolean(PREF_UNPAUSE_ON_HEADSET_RECONNECT, true);
                break;
            case PREF_FOLLOW_QUEUE:
                followQueue = sp.getBoolean(PREF_FOLLOW_QUEUE, false);
                break;
            case PREF_AUTO_DELETE:
                autoDelete = sp.getBoolean(PREF_AUTO_DELETE, false);
                break;
            case PREF_SMART_MARK_AS_PLAYED_SECS:
                smartMarkAsPlayedSecs = Integer.valueOf(sp.getString(PREF_SMART_MARK_AS_PLAYED_SECS, "30"));
                break;
            case PREF_PLAYBACK_SPEED_ARRAY:
                playbackSpeedArray = readPlaybackSpeedArray(sp.getString(PREF_PLAYBACK_SPEED_ARRAY, null));
                break;
            case PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS:
                pauseForFocusLoss = sp.getBoolean(PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, false);
                break;
            case PREF_RESUME_AFTER_CALL:
                resumeAfterCall = sp.getBoolean(PREF_RESUME_AFTER_CALL, true);
                break;
            // Network
            case PREF_UPDATE_INTERVAL:
                updateInterval = readUpdateInterval(sp.getString(PREF_UPDATE_INTERVAL, "0"));
                ClientConfig.applicationCallbacks.setUpdateInterval(updateInterval);
                break;
            case PREF_MOBILE_UPDATE:
                allowMobileUpdate = sp.getBoolean(PREF_MOBILE_UPDATE, false);
                break;
            case PREF_PARALLEL_DOWNLOADS:
                parallelDownloads = Integer.valueOf(sp.getString(PREF_PARALLEL_DOWNLOADS, "6"));
                break;
            case PREF_EPISODE_CACHE_SIZE:
                episodeCacheSize = readEpisodeCacheSizeInternal(sp.getString(PREF_EPISODE_CACHE_SIZE, "20"));
                break;
            case PREF_ENABLE_AUTODL:
                enableAutodownload = sp.getBoolean(PREF_ENABLE_AUTODL, false);
                break;
            case PREF_ENABLE_AUTODL_ON_BATTERY:
                enableAutodownloadOnBattery = sp.getBoolean(PREF_ENABLE_AUTODL_ON_BATTERY, true);
                break;
            case PREF_ENABLE_AUTODL_WIFI_FILTER:
                enableAutodownloadWifiFilter = sp.getBoolean(PREF_ENABLE_AUTODL_WIFI_FILTER, false);
                break;
            case PREF_AUTODL_SELECTED_NETWORKS:
                autodownloadSelectedNetworks = StringUtils.split(
                        sp.getString(PREF_AUTODL_SELECTED_NETWORKS, ""), ',');
                break;
            // Services
            case PREF_AUTO_FLATTR:
                autoFlattr = sp.getBoolean(PREF_AUTO_FLATTR, false);
                break;
            case PREF_AUTO_FLATTR_PLAYED_DURATION_THRESHOLD:
                autoFlattrPlayedDurationThreshold = sp.getFloat(PREF_AUTO_FLATTR_PLAYED_DURATION_THRESHOLD,
                        PREF_AUTO_FLATTR_PLAYED_DURATION_THRESHOLD_DEFAULT);
                break;
            // Mediaplayer
            case PREF_PLAYBACK_SPEED:
                playbackSpeed = sp.getString(PREF_PLAYBACK_SPEED, "1.0");
                break;
            case PREF_FAST_FORWARD_SECS:
                fastForwardSecs = sp.getInt(PREF_FAST_FORWARD_SECS, 30);
                break;
            case PREF_REWIND_SECS:
                rewindSecs = sp.getInt(PREF_REWIND_SECS, 30);
                break;
            case PREF_QUEUE_LOCKED:
                queueLocked = sp.getBoolean(PREF_QUEUE_LOCKED, false);
                break;
            default:
                Log.w(TAG, "Unhandled key: " + key);
        }
    }

    public static void setPrefFastForwardSecs(int secs) {
        Log.d(TAG, "setPrefFastForwardSecs(" + secs +")");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(instance.context).edit();
        editor.putInt(PREF_FAST_FORWARD_SECS, secs);
        editor.commit();
    }

    public static void setPrefRewindSecs(int secs) {
        Log.d(TAG, "setPrefRewindSecs(" + secs +")");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(instance.context).edit();
        editor.putInt(PREF_REWIND_SECS, secs);
        editor.commit();
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
     * Sets the update interval value. Should only be used for testing purposes!
     */
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
     * Change the auto-flattr settings
     *
     * @param context For accessing the shared preferences
     * @param enabled Whether automatic flattring should be enabled at all
     * @param autoFlattrThreshold The percentage of playback time after which an episode should be
     *                            flattrd. Must be a value between 0 and 1 (inclusive)
     * */
    public static void setAutoFlattrSettings(Context context, boolean enabled, float autoFlattrThreshold) {
        instanceAvailable();
        Validate.inclusiveBetween(0.0, 1.0, autoFlattrThreshold);
        PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext())
                .edit()
                .putBoolean(PREF_AUTO_FLATTR, enabled)
                .putFloat(PREF_AUTO_FLATTR_PLAYED_DURATION_THRESHOLD, autoFlattrThreshold)
                .commit();
        instance.autoFlattr = enabled;
        instance.autoFlattrPlayedDurationThreshold = autoFlattrThreshold;
    }

    public static void setHiddenDrawerItems(Context context, List<String> items) {
        instanceAvailable();
        instance.hiddenDrawerItems = items;
        String str = StringUtils.join(items, ',');
        PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext())
                .edit()
                .putString(PREF_HIDDEN_DRAWER_ITEMS, str)
                .commit();
    }

    public static void setQueueLocked(boolean locked) {
        instanceAvailable();
        instance.queueLocked = locked;
        PreferenceManager.getDefaultSharedPreferences(instance.context)
                .edit()
                .putBoolean(PREF_QUEUE_LOCKED, locked)
                .commit();
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
        instanceAvailable();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
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
        instanceAvailable();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(instance.context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_DATA_FOLDER, dir);
        editor.commit();
        createImportDirectory();
    }

    /**
     * Create a .nomedia file to prevent scanning by the media scanner.
     */
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
            Log.d(TAG, ".nomedia file created");
        }
    }

    /**
     * Creates the import directory if it doesn't exist and if storage is
     * available
     */
    private static void createImportDirectory() {
        File importDir = getDataFolder(instance.context,
                IMPORT_DIR);
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

    /**
     * Updates alarm registered with the AlarmManager service or deactivates it.
     */
    public static void restartUpdateAlarm(long triggerAtMillis, long intervalMillis) {
        instanceAvailable();
        Log.d(TAG, "Restarting update alarm.");
        AlarmManager alarmManager = (AlarmManager) instance.context
                .getSystemService(Context.ALARM_SERVICE);
        PendingIntent updateIntent = PendingIntent.getBroadcast(
                instance.context, 0, new Intent(ClientConfig.applicationCallbacks.getApplicationInstance(), FeedUpdateReceiver.class), 0);
        alarmManager.cancel(updateIntent);
        if (intervalMillis != 0) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis,
                    updateIntent);
            Log.d(TAG, "Changed alarm to new interval");
        } else {
            Log.d(TAG, "Automatic update was deactivated");
        }
    }


    /**
     * Reads episode cache size as it is saved in the episode_cache_size_values array.
     */
    public static int readEpisodeCacheSize(String valueFromPrefs) {
        instanceAvailable();
        return instance.readEpisodeCacheSizeInternal(valueFromPrefs);
    }
}
