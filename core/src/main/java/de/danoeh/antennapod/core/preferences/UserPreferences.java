package de.danoeh.antennapod.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.service.download.ProxyConfig;
import de.danoeh.antennapod.core.storage.APCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.APNullCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.APQueueCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.EpisodeCleanupAlgorithm;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;

/**
 * Provides access to preferences set by the user in the settings screen. A
 * private instance of this class must first be instantiated via
 * init() or otherwise every public method will throw an Exception
 * when called.
 */
public class UserPreferences {
    private UserPreferences(){}

    private static final String IMPORT_DIR = "import/";

    private static final String TAG = "UserPreferences";

    // User Interface
    public static final String PREF_THEME = "prefTheme";
    public static final String PREF_HIDDEN_DRAWER_ITEMS = "prefHiddenDrawerItems";
    private static final String PREF_DRAWER_FEED_ORDER = "prefDrawerFeedOrder";
    private static final String PREF_DRAWER_FEED_COUNTER = "prefDrawerFeedIndicator";
    public static final String PREF_EXPANDED_NOTIFICATION = "prefExpandNotify";
    private static final String PREF_PERSISTENT_NOTIFICATION = "prefPersistNotify";
    public static final String PREF_COMPACT_NOTIFICATION_BUTTONS = "prefCompactNotificationButtons";
    public static final String PREF_LOCKSCREEN_BACKGROUND = "prefLockscreenBackground";
    private static final String PREF_SHOW_DOWNLOAD_REPORT = "prefShowDownloadReport";
    public static final String PREF_BACK_BUTTON_BEHAVIOR = "prefBackButtonBehavior";
    private static final String PREF_BACK_BUTTON_GO_TO_PAGE = "prefBackButtonGoToPage";

    // Queue
    private static final String PREF_QUEUE_ADD_TO_FRONT = "prefQueueAddToFront";

    // Playback
    public static final String PREF_PAUSE_ON_HEADSET_DISCONNECT = "prefPauseOnHeadsetDisconnect";
    public static final String PREF_UNPAUSE_ON_HEADSET_RECONNECT = "prefUnpauseOnHeadsetReconnect";
    private static final String PREF_UNPAUSE_ON_BLUETOOTH_RECONNECT = "prefUnpauseOnBluetoothReconnect";
    private static final String PREF_HARDWARE_FOWARD_BUTTON_SKIPS = "prefHardwareForwardButtonSkips";
    private static final String PREF_HARDWARE_PREVIOUS_BUTTON_RESTARTS = "prefHardwarePreviousButtonRestarts";
    public static final String PREF_FOLLOW_QUEUE = "prefFollowQueue";
    private static final String PREF_SKIP_KEEPS_EPISODE = "prefSkipKeepsEpisode";
    private static final String PREF_FAVORITE_KEEPS_EPISODE = "prefFavoriteKeepsEpisode";
    private static final String PREF_AUTO_DELETE = "prefAutoDelete";
    public static final String PREF_SMART_MARK_AS_PLAYED_SECS = "prefSmartMarkAsPlayedSecs";
    private static final String PREF_PLAYBACK_SPEED_ARRAY = "prefPlaybackSpeedArray";
    private static final String PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS = "prefPauseForFocusLoss";
    private static final String PREF_RESUME_AFTER_CALL = "prefResumeAfterCall";
    public static final String PREF_VIDEO_BEHAVIOR = "prefVideoBehavior";
    private static final String PREF_TIME_RESPECTS_SPEED = "prefPlaybackTimeRespectsSpeed";

    // Network
    private static final String PREF_ENQUEUE_DOWNLOADED = "prefEnqueueDownloaded";
    public static final String PREF_UPDATE_INTERVAL = "prefAutoUpdateIntervall";
    public static final String PREF_MOBILE_UPDATE = "prefMobileUpdateAllowed";
    public static final String PREF_EPISODE_CLEANUP = "prefEpisodeCleanup";
    public static final String PREF_PARALLEL_DOWNLOADS = "prefParallelDownloads";
    public static final String PREF_EPISODE_CACHE_SIZE = "prefEpisodeCacheSize";
    public static final String PREF_ENABLE_AUTODL = "prefEnableAutoDl";
    public static final String PREF_ENABLE_AUTODL_ON_BATTERY = "prefEnableAutoDownloadOnBattery";
    public static final String PREF_ENABLE_AUTODL_WIFI_FILTER = "prefEnableAutoDownloadWifiFilter";
    public static final String PREF_ENABLE_AUTODL_ON_MOBILE = "prefEnableAutoDownloadOnMobile";
    private static final String PREF_AUTODL_SELECTED_NETWORKS = "prefAutodownloadSelectedNetworks";
    private static final String PREF_PROXY_TYPE = "prefProxyType";
    private static final String PREF_PROXY_HOST = "prefProxyHost";
    private static final String PREF_PROXY_PORT = "prefProxyPort";
    private static final String PREF_PROXY_USER = "prefProxyUser";
    private static final String PREF_PROXY_PASSWORD = "prefProxyPassword";

    // Services
    private static final String PREF_GPODNET_NOTIFICATIONS = "pref_gpodnet_notifications";

    // Other
    private static final String PREF_DATA_FOLDER = "prefDataFolder";
    public static final String PREF_IMAGE_CACHE_SIZE = "prefImageCacheSize";
    public static final String PREF_DELETE_REMOVES_FROM_QUEUE = "prefDeleteRemovesFromQueue";

    // Mediaplayer
    public static final String PREF_MEDIA_PLAYER = "prefMediaPlayer";
    public static final String PREF_MEDIA_PLAYER_EXOPLAYER = "exoplayer";
    private static final String PREF_PLAYBACK_SPEED = "prefPlaybackSpeed";
    public static final String PREF_PLAYBACK_SKIP_SILENCE = "prefSkipSilence";
    private static final String PREF_FAST_FORWARD_SECS = "prefFastForwardSecs";
    private static final String PREF_REWIND_SECS = "prefRewindSecs";
    private static final String PREF_QUEUE_LOCKED = "prefQueueLocked";
    private static final String IMAGE_CACHE_DEFAULT_VALUE = "100";
    private static final int IMAGE_CACHE_SIZE_MINIMUM = 20;
    private static final String PREF_LEFT_VOLUME = "prefLeftVolume";
    private static final String PREF_RIGHT_VOLUME = "prefRightVolume";

    // Experimental
    private static final String PREF_STEREO_TO_MONO = "PrefStereoToMono";
    public static final String PREF_CAST_ENABLED = "prefCast"; //Used for enabling Chromecast support
    public static final int EPISODE_CLEANUP_QUEUE = -1;
    public static final int EPISODE_CLEANUP_NULL = -2;
    public static final int EPISODE_CLEANUP_DEFAULT = 0;

    // Constants
    private static final int NOTIFICATION_BUTTON_REWIND = 0;
    private static final int NOTIFICATION_BUTTON_FAST_FORWARD = 1;
    private static final int NOTIFICATION_BUTTON_SKIP = 2;
    private static final int EPISODE_CACHE_SIZE_UNLIMITED = -1;
    public static final int FEED_ORDER_COUNTER = 0;
    public static final int FEED_ORDER_ALPHABETICAL = 1;
    public static final int FEED_ORDER_MOST_PLAYED = 3;
    public static final int FEED_COUNTER_SHOW_NEW_UNPLAYED_SUM = 0;
    public static final int FEED_COUNTER_SHOW_NEW = 1;
    public static final int FEED_COUNTER_SHOW_UNPLAYED = 2;
    public static final int FEED_COUNTER_SHOW_NONE = 3;
    public static final int FEED_COUNTER_SHOW_DOWNLOADED = 4;

    private static Context context;
    private static SharedPreferences prefs;

    /**
     * Sets up the UserPreferences class.
     *
     * @throws IllegalArgumentException if context is null
     */
    public static void init(@NonNull Context context) {
        Log.d(TAG, "Creating new instance of UserPreferences");

        UserPreferences.context = context.getApplicationContext();
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
        } else if (theme == R.style.Theme_AntennaPod_TrueBlack) {
            return R.style.Theme_AntennaPod_TrueBlack_NoTitle;
        } else {
            return R.style.Theme_AntennaPod_Light_NoTitle;
        }
    }

    public static List<String> getHiddenDrawerItems() {
        String hiddenItems = prefs.getString(PREF_HIDDEN_DRAWER_ITEMS, "");
        return new ArrayList<>(Arrays.asList(TextUtils.split(hiddenItems, ",")));
    }

    public static List<Integer> getCompactNotificationButtons() {
        String[] buttons = TextUtils.split(
                prefs.getString(PREF_COMPACT_NOTIFICATION_BUTTONS,
                        String.valueOf(NOTIFICATION_BUTTON_SKIP)),
                ",");
        List<Integer> notificationButtons = new ArrayList<>();
        for (String button : buttons) {
            notificationButtons.add(Integer.parseInt(button));
        }
        return notificationButtons;
    }

    /**
     * Helper function to return whether the specified button should be shown on compact
     * notifications.
     *
     * @param buttonId Either NOTIFICATION_BUTTON_REWIND, NOTIFICATION_BUTTON_FAST_FORWARD or
     *                 NOTIFICATION_BUTTON_SKIP.
     * @return {@code true} if button should be shown, {@code false}  otherwise
     */
    private static boolean showButtonOnCompactNotification(int buttonId) {
        return getCompactNotificationButtons().contains(buttonId);
    }

    public static boolean showRewindOnCompactNotification() {
        return showButtonOnCompactNotification(NOTIFICATION_BUTTON_REWIND);
    }

    public static boolean showFastForwardOnCompactNotification() {
        return showButtonOnCompactNotification(NOTIFICATION_BUTTON_FAST_FORWARD);
    }

    public static boolean showSkipOnCompactNotification() {
        return showButtonOnCompactNotification(NOTIFICATION_BUTTON_SKIP);
    }

    public static int getFeedOrder() {
        String value = prefs.getString(PREF_DRAWER_FEED_ORDER, "0");
        return Integer.parseInt(value);
    }

    public static int getFeedCounterSetting() {
        String value = prefs.getString(PREF_DRAWER_FEED_COUNTER, "0");
        return Integer.parseInt(value);
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
        return prefs.getBoolean(PREF_PERSISTENT_NOTIFICATION, true);
    }

    /**
     * Returns true if the lockscreen background should be set to the current episode's image
     *
     * @return {@code true} if the lockscreen background should be set, {@code false}  otherwise
     */
    public static boolean setLockscreenBackground() {
        return prefs.getBoolean(PREF_LOCKSCREEN_BACKGROUND, true);
    }

    /**
     * Returns true if download reports are shown
     *
     * @return {@code true} if download reports are shown, {@code false}  otherwise
     */
    public static boolean showDownloadReport() {
        return prefs.getBoolean(PREF_SHOW_DOWNLOAD_REPORT, true);
    }

    public static boolean enqueueDownloadedEpisodes() {
        return prefs.getBoolean(PREF_ENQUEUE_DOWNLOADED, true);
    }

    public static boolean enqueueAtFront() {
        return prefs.getBoolean(PREF_QUEUE_ADD_TO_FRONT, false);
    }

    public static boolean isPauseOnHeadsetDisconnect() {
        return prefs.getBoolean(PREF_PAUSE_ON_HEADSET_DISCONNECT, true);
    }

    public static boolean isUnpauseOnHeadsetReconnect() {
        return prefs.getBoolean(PREF_UNPAUSE_ON_HEADSET_RECONNECT, true);
    }

    public static boolean isUnpauseOnBluetoothReconnect() {
        return prefs.getBoolean(PREF_UNPAUSE_ON_BLUETOOTH_RECONNECT, false);
    }

    public static boolean shouldHardwareButtonSkip() {
        return prefs.getBoolean(PREF_HARDWARE_FOWARD_BUTTON_SKIPS, false);
    }

    public static boolean shouldHardwarePreviousButtonRestart() {
        return prefs.getBoolean(PREF_HARDWARE_PREVIOUS_BUTTON_RESTARTS, false);
    }


    public static boolean isFollowQueue() {
        return prefs.getBoolean(PREF_FOLLOW_QUEUE, true);
    }

    public static boolean shouldSkipKeepEpisode() { return prefs.getBoolean(PREF_SKIP_KEEPS_EPISODE, true); }

    public static boolean shouldFavoriteKeepEpisode() {
        return prefs.getBoolean(PREF_FAVORITE_KEEPS_EPISODE, true);
    }

    public static boolean isAutoDelete() {
        return prefs.getBoolean(PREF_AUTO_DELETE, false);
    }

    public static int getSmartMarkAsPlayedSecs() {
        return Integer.parseInt(prefs.getString(PREF_SMART_MARK_AS_PLAYED_SECS, "30"));
    }

    public static boolean shouldDeleteRemoveFromQueue() {
        return prefs.getBoolean(PREF_DELETE_REMOVES_FROM_QUEUE, false);
    }

    public static String getPlaybackSpeed() {
        return prefs.getString(PREF_PLAYBACK_SPEED, "1.00");
    }

    public static boolean isSkipSilence() {
        return prefs.getBoolean(PREF_PLAYBACK_SKIP_SILENCE, false);
    }

    public static String[] getPlaybackSpeedArray() {
        return readPlaybackSpeedArray(prefs.getString(PREF_PLAYBACK_SPEED_ARRAY, null));
    }

    public static float getLeftVolume() {
        int volume = prefs.getInt(PREF_LEFT_VOLUME, 100);
        return Converter.getVolumeFromPercentage(volume);
    }

    public static float getRightVolume() {
        int volume = prefs.getInt(PREF_RIGHT_VOLUME, 100);
        return Converter.getVolumeFromPercentage(volume);
    }

    public static int getLeftVolumePercentage() {
        return prefs.getInt(PREF_LEFT_VOLUME, 100);
    }

    public static int getRightVolumePercentage() {
        return prefs.getInt(PREF_RIGHT_VOLUME, 100);
    }

    public static boolean shouldPauseForFocusLoss() {
        return prefs.getBoolean(PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, false);
    }


    /*
     * Returns update interval in milliseconds; value 0 means that auto update is disabled
     * or feeds are updated at a certain time of day
     */
    public static long getUpdateInterval() {
        String updateInterval = prefs.getString(PREF_UPDATE_INTERVAL, "0");
        if(!updateInterval.contains(":")) {
            return readUpdateInterval(updateInterval);
        } else {
            return 0;
        }
    }

    public static int[] getUpdateTimeOfDay() {
        String datetime = prefs.getString(PREF_UPDATE_INTERVAL, "");
        if(datetime.length() >= 3 && datetime.contains(":")) {
            String[] parts = datetime.split(":");
            int hourOfDay = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return new int[] { hourOfDay, minute };
        } else {
            return new int[0];
        }
    }

    public static boolean isAutoUpdateDisabled() {
        return prefs.getString(PREF_UPDATE_INTERVAL, "").equals("0");
    }

    public static String getMobileUpdatesEnabled() {
        return prefs.getString(PREF_MOBILE_UPDATE, "images");
    }

    public static boolean isAllowMobileUpdate() {
        return getMobileUpdatesEnabled().equals("everything");
    }

    public static boolean isAllowMobileImages() {
        return isAllowMobileUpdate() || getMobileUpdatesEnabled().equals("images");
    }

    public static int getParallelDownloads() {
        return Integer.parseInt(prefs.getString(PREF_PARALLEL_DOWNLOADS, "4"));
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

    public static boolean isEnableAutodownloadOnMobile() {
        return prefs.getBoolean(PREF_ENABLE_AUTODL_ON_MOBILE, false);
    }


    public static int getImageCacheSize() {
        String cacheSizeString = prefs.getString(PREF_IMAGE_CACHE_SIZE, IMAGE_CACHE_DEFAULT_VALUE);
        int cacheSizeInt = Integer.parseInt(cacheSizeString);
        // if the cache size is too small the user won't get any images at all
        // that's bad, force it back to the default.
        if (cacheSizeInt < IMAGE_CACHE_SIZE_MINIMUM) {
            prefs.edit().putString(PREF_IMAGE_CACHE_SIZE, IMAGE_CACHE_DEFAULT_VALUE).apply();
            cacheSizeInt = Integer.parseInt(IMAGE_CACHE_DEFAULT_VALUE);
        }
        int cacheSizeMB = cacheSizeInt * 1024 * 1024;
        return cacheSizeMB;
    }

    public static int getFastForwardSecs() {
        return prefs.getInt(PREF_FAST_FORWARD_SECS, 30);
    }

    public static int getRewindSecs() {
        return prefs.getInt(PREF_REWIND_SECS, 30);
    }

    public static String[] getAutodownloadSelectedNetworks() {
        String selectedNetWorks = prefs.getString(PREF_AUTODL_SELECTED_NETWORKS, "");
        return TextUtils.split(selectedNetWorks, ",");
    }

    public static void setProxyConfig(ProxyConfig config) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_PROXY_TYPE, config.type.name());
        if(TextUtils.isEmpty(config.host)) {
            editor.remove(PREF_PROXY_HOST);
        } else {
            editor.putString(PREF_PROXY_HOST, config.host);
        }
        if(config.port <= 0 || config.port > 65535) {
            editor.remove(PREF_PROXY_PORT);
        } else {
            editor.putInt(PREF_PROXY_PORT, config.port);
        }
        if(TextUtils.isEmpty(config.username)) {
            editor.remove(PREF_PROXY_USER);
        } else {
            editor.putString(PREF_PROXY_USER, config.username);
        }
        if(TextUtils.isEmpty(config.password)) {
            editor.remove(PREF_PROXY_PASSWORD);
        } else {
            editor.putString(PREF_PROXY_PASSWORD, config.password);
        }
        editor.apply();
    }

    public static ProxyConfig getProxyConfig() {
        Proxy.Type type = Proxy.Type.valueOf(prefs.getString(PREF_PROXY_TYPE, Proxy.Type.DIRECT.name()));
        String host = prefs.getString(PREF_PROXY_HOST, null);
        int port = prefs.getInt(PREF_PROXY_PORT, 0);
        String username = prefs.getString(PREF_PROXY_USER, null);
        String password = prefs.getString(PREF_PROXY_PASSWORD, null);
        return new ProxyConfig(type, host, port, username, password);
    }

    public static boolean shouldResumeAfterCall() {
        return prefs.getBoolean(PREF_RESUME_AFTER_CALL, true);
    }

    public static boolean isQueueLocked() {
        return prefs.getBoolean(PREF_QUEUE_LOCKED, false);
    }

    public static void setFastForwardSecs(int secs) {
        prefs.edit()
             .putInt(PREF_FAST_FORWARD_SECS, secs)
             .apply();
    }

    public static void setRewindSecs(int secs) {
        prefs.edit()
             .putInt(PREF_REWIND_SECS, secs)
             .apply();
    }

    public static void setPlaybackSpeed(String speed) {
        prefs.edit()
             .putString(PREF_PLAYBACK_SPEED, speed)
             .apply();
    }

    public static void setSkipSilence(boolean skipSilence) {
        prefs.edit()
                .putBoolean(PREF_PLAYBACK_SKIP_SILENCE, skipSilence)
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

    public static void setVolume(@IntRange(from = 0, to = 100) int leftVolume,
                                 @IntRange(from = 0, to = 100) int rightVolume) {
        prefs.edit()
             .putInt(PREF_LEFT_VOLUME, leftVolume)
             .putInt(PREF_RIGHT_VOLUME, rightVolume)
             .apply();
    }

    public static void setAutodownloadSelectedNetworks(String[] value) {
        prefs.edit()
             .putString(PREF_AUTODL_SELECTED_NETWORKS, TextUtils.join(",", value))
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
        restartUpdateAlarm();
    }

    /**
     * Sets the update interval value.
     */
    public static void setUpdateTimeOfDay(int hourOfDay, int minute) {
        prefs.edit()
             .putString(PREF_UPDATE_INTERVAL, hourOfDay + ":" + minute)
             .apply();
        restartUpdateAlarm();
    }

    public static void disableAutoUpdate() {
        prefs.edit()
                .putString(PREF_UPDATE_INTERVAL, "0")
                .apply();
        AutoUpdateManager.disableAutoUpdate();
    }

    public static boolean gpodnetNotificationsEnabled() {
        return prefs.getBoolean(PREF_GPODNET_NOTIFICATIONS, true);
    }

    public static void setGpodnetNotificationsEnabled() {
        prefs.edit()
                .putBoolean(PREF_GPODNET_NOTIFICATIONS, true)
                .apply();
    }

    public static void setHiddenDrawerItems(List<String> items) {
        String str = TextUtils.join(",", items);
        prefs.edit()
             .putString(PREF_HIDDEN_DRAWER_ITEMS, str)
             .apply();
    }

    public static void setCompactNotificationButtons(List<Integer> items) {
        String str = TextUtils.join(",", items);
        prefs.edit()
             .putString(PREF_COMPACT_NOTIFICATION_BUTTONS, str)
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
            case 2:
                return R.style.Theme_AntennaPod_TrueBlack;
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
            return Integer.parseInt(valueFromPrefs);
        }
    }

    private static String[] readPlaybackSpeedArray(String valueFromPrefs) {
        String[] selectedSpeeds = null;
        // If this preference hasn't been set yet, return the default options
        if (valueFromPrefs == null) {
            selectedSpeeds = new String[] { "1.00", "1.25", "1.50", "1.75", "2.00" };
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

    public static boolean useSonic() {
        return prefs.getString(PREF_MEDIA_PLAYER, "sonic").equals("sonic");
    }

    public static boolean useExoplayer() {
        return prefs.getString(PREF_MEDIA_PLAYER, "sonic").equals(PREF_MEDIA_PLAYER_EXOPLAYER);
    }

    public static void enableSonic() {
        prefs.edit().putString(PREF_MEDIA_PLAYER, "sonic").apply();
    }

    public static boolean stereoToMono() {
        return prefs.getBoolean(PREF_STEREO_TO_MONO, false);
    }

    public static void stereoToMono(boolean enable) {
        prefs.edit()
                .putBoolean(PREF_STEREO_TO_MONO, enable)
                .apply();
    }

    public static VideoBackgroundBehavior getVideoBackgroundBehavior() {
        switch (prefs.getString(PREF_VIDEO_BEHAVIOR, "stop")) {
            case "stop": return VideoBackgroundBehavior.STOP;
            case "pip": return VideoBackgroundBehavior.PICTURE_IN_PICTURE;
            case "continue": return VideoBackgroundBehavior.CONTINUE_PLAYING;
            default: return VideoBackgroundBehavior.STOP;
        }
    }

    public static EpisodeCleanupAlgorithm getEpisodeCleanupAlgorithm() {
        int cleanupValue = getEpisodeCleanupValue();
        if (cleanupValue == EPISODE_CLEANUP_QUEUE) {
            return new APQueueCleanupAlgorithm();
        } else if (cleanupValue == EPISODE_CLEANUP_NULL) {
            return new APNullCleanupAlgorithm();
        } else {
            return new APCleanupAlgorithm(cleanupValue);
        }
    }

    public static int getEpisodeCleanupValue() {
        return Integer.parseInt(prefs.getString(PREF_EPISODE_CLEANUP, "-1"));
    }

    public static void setEpisodeCleanupValue(int episodeCleanupValue) {
        prefs.edit()
                .putString(PREF_EPISODE_CLEANUP, Integer.toString(episodeCleanupValue))
                .apply();
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
    public static File getDataFolder(String type) {
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
                            dataDir = getDataFolder(dirs[i]);
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
        Log.d(TAG, "setDataFolder(dir: " + dir + ")");
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
        File importDir = getDataFolder(IMPORT_DIR);
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
     *
     * @return true if auto update is set to a specific time
     *         false if auto update is set to interval
     */
    public static boolean isAutoUpdateTimeOfDay() {
        return getUpdateTimeOfDay().length == 2;
    }

    public static void restartUpdateAlarm() {
        if (isAutoUpdateDisabled()) {
            AutoUpdateManager.disableAutoUpdate();
        } else if (isAutoUpdateTimeOfDay()) {
            int[] timeOfDay = getUpdateTimeOfDay();
            Log.d(TAG, "timeOfDay: " + Arrays.toString(timeOfDay));
            AutoUpdateManager.restartUpdateTimeOfDayAlarm(timeOfDay[0], timeOfDay[1]);
        } else {
            long milliseconds = getUpdateInterval();
            AutoUpdateManager.restartUpdateIntervalAlarm(milliseconds);
        }
    }

    /**
     * Reads episode cache size as it is saved in the episode_cache_size_values array.
     */
    public static int readEpisodeCacheSize(String valueFromPrefs) {
        return readEpisodeCacheSizeInternal(valueFromPrefs);
    }

    /**
     * Evaluates whether Cast support (Chromecast, Audio Cast, etc) is enabled on the preferences.
     */
    public static boolean isCastEnabled() {
        return prefs.getBoolean(PREF_CAST_ENABLED, false);
    }

    public enum VideoBackgroundBehavior {
        STOP, PICTURE_IN_PICTURE, CONTINUE_PLAYING
    }

    public enum BackButtonBehavior {
        DEFAULT, OPEN_DRAWER, DOUBLE_TAP, SHOW_PROMPT, GO_TO_PAGE
    }

    public static BackButtonBehavior getBackButtonBehavior() {
        switch (prefs.getString(PREF_BACK_BUTTON_BEHAVIOR, "default")) {
            case "default": return BackButtonBehavior.DEFAULT;
            case "drawer": return BackButtonBehavior.OPEN_DRAWER;
            case "doubletap": return BackButtonBehavior.DOUBLE_TAP;
            case "prompt": return BackButtonBehavior.SHOW_PROMPT;
            case "page": return BackButtonBehavior.GO_TO_PAGE;
            default: return BackButtonBehavior.DEFAULT;
        }
    }

    public static String getBackButtonGoToPage() {
        return prefs.getString(PREF_BACK_BUTTON_GO_TO_PAGE, "QueueFragment");
    }

    public static void setBackButtonGoToPage(String tag) {
        prefs.edit()
                .putString(PREF_BACK_BUTTON_GO_TO_PAGE, tag)
                .apply();
    }

    public static boolean timeRespectsSpeed() {
        return prefs.getBoolean(PREF_TIME_RESPECTS_SPEED, false);
    }
}
