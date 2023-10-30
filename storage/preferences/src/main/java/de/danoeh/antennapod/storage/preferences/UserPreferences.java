package de.danoeh.antennapod.storage.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import de.danoeh.antennapod.model.download.ProxyConfig;
import de.danoeh.antennapod.model.feed.FeedCounter;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.model.feed.SubscriptionsFilter;
import de.danoeh.antennapod.model.playback.MediaType;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Provides access to preferences set by the user in the settings screen. A
 * private instance of this class must first be instantiated via
 * init() or otherwise every public method will throw an Exception
 * when called.
 */
public class UserPreferences {
    private UserPreferences(){}

    private static final String TAG = "UserPreferences";

    // User Interface
    public static final String PREF_THEME = "prefTheme";
    public static final String PREF_THEME_BLACK = "prefThemeBlack";
    public static final String PREF_TINTED_COLORS = "prefTintedColors";
    public static final String PREF_HIDDEN_DRAWER_ITEMS = "prefHiddenDrawerItems";
    public static final String PREF_DRAWER_FEED_ORDER = "prefDrawerFeedOrder";
    public static final String PREF_DRAWER_FEED_COUNTER = "prefDrawerFeedIndicator";
    public static final String PREF_EXPANDED_NOTIFICATION = "prefExpandNotify";
    public static final String PREF_USE_EPISODE_COVER = "prefEpisodeCover";
    public static final String PREF_SHOW_TIME_LEFT = "showTimeLeft";
    private static final String PREF_PERSISTENT_NOTIFICATION = "prefPersistNotify";
    public static final String PREF_COMPACT_NOTIFICATION_BUTTONS = "prefCompactNotificationButtons";
    private static final String PREF_SHOW_DOWNLOAD_REPORT = "prefShowDownloadReport";
    public static final String PREF_DEFAULT_PAGE = "prefDefaultPage";
    public static final String PREF_FILTER_FEED = "prefSubscriptionsFilter";
    public static final String PREF_SUBSCRIPTION_TITLE = "prefSubscriptionTitle";
    public static final String PREF_BACK_OPENS_DRAWER = "prefBackButtonOpensDrawer";

    public static final String PREF_QUEUE_KEEP_SORTED = "prefQueueKeepSorted";
    public static final String PREF_QUEUE_KEEP_SORTED_ORDER = "prefQueueKeepSortedOrder";
    public static final String PREF_NEW_EPISODES_ACTION = "prefNewEpisodesAction";
    private static final String PREF_DOWNLOADS_SORTED_ORDER = "prefDownloadSortedOrder";
    private static final String PREF_INBOX_SORTED_ORDER = "prefInboxSortedOrder";

    // Playback
    public static final String PREF_PAUSE_ON_HEADSET_DISCONNECT = "prefPauseOnHeadsetDisconnect";
    public static final String PREF_UNPAUSE_ON_HEADSET_RECONNECT = "prefUnpauseOnHeadsetReconnect";
    public static final String PREF_UNPAUSE_ON_BLUETOOTH_RECONNECT = "prefUnpauseOnBluetoothReconnect";
    public static final String PREF_HARDWARE_FORWARD_BUTTON = "prefHardwareForwardButton";
    public static final String PREF_HARDWARE_PREVIOUS_BUTTON = "prefHardwarePreviousButton";
    public static final String PREF_FOLLOW_QUEUE = "prefFollowQueue";
    public static final String PREF_SKIP_KEEPS_EPISODE = "prefSkipKeepsEpisode";
    private static final String PREF_FAVORITE_KEEPS_EPISODE = "prefFavoriteKeepsEpisode";
    private static final String PREF_AUTO_DELETE = "prefAutoDelete";
    private static final String PREF_AUTO_DELETE_LOCAL = "prefAutoDeleteLocal";
    public static final String PREF_SMART_MARK_AS_PLAYED_SECS = "prefSmartMarkAsPlayedSecs";
    private static final String PREF_PLAYBACK_SPEED_ARRAY = "prefPlaybackSpeedArray";
    public static final String PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS = "prefPauseForFocusLoss";
    private static final String PREF_TIME_RESPECTS_SPEED = "prefPlaybackTimeRespectsSpeed";
    public static final String PREF_STREAM_OVER_DOWNLOAD = "prefStreamOverDownload";

    // Network
    private static final String PREF_ENQUEUE_DOWNLOADED = "prefEnqueueDownloaded";
    public static final String PREF_ENQUEUE_LOCATION = "prefEnqueueLocation";
    public static final String PREF_UPDATE_INTERVAL = "prefAutoUpdateIntervall";
    private static final String PREF_MOBILE_UPDATE = "prefMobileUpdateTypes";
    public static final String PREF_EPISODE_CLEANUP = "prefEpisodeCleanup";
    public static final String PREF_EPISODE_CACHE_SIZE = "prefEpisodeCacheSize";
    public static final String PREF_ENABLE_AUTODL = "prefEnableAutoDl";
    public static final String PREF_ENABLE_AUTODL_ON_BATTERY = "prefEnableAutoDownloadOnBattery";
    public static final String PREF_ENABLE_AUTODL_WIFI_FILTER = "prefEnableAutoDownloadWifiFilter";
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
    public static final String PREF_DELETE_REMOVES_FROM_QUEUE = "prefDeleteRemovesFromQueue";

    // Mediaplayer
    private static final String PREF_PLAYBACK_SPEED = "prefPlaybackSpeed";
    private static final String PREF_VIDEO_PLAYBACK_SPEED = "prefVideoPlaybackSpeed";
    public static final String PREF_PLAYBACK_SKIP_SILENCE = "prefSkipSilence";
    private static final String PREF_FAST_FORWARD_SECS = "prefFastForwardSecs";
    private static final String PREF_REWIND_SECS = "prefRewindSecs";
    private static final String PREF_QUEUE_LOCKED = "prefQueueLocked";

    // Experimental
    public static final int EPISODE_CLEANUP_QUEUE = -1;
    public static final int EPISODE_CLEANUP_NULL = -2;
    public static final int EPISODE_CLEANUP_EXCEPT_FAVORITE = -3;
    public static final int EPISODE_CLEANUP_DEFAULT = 0;

    // Constants
    private static final int NOTIFICATION_BUTTON_REWIND = 0;
    private static final int NOTIFICATION_BUTTON_FAST_FORWARD = 1;
    private static final int NOTIFICATION_BUTTON_SKIP = 2;
    public static final int EPISODE_CACHE_SIZE_UNLIMITED = -1;
    public static final int FEED_ORDER_COUNTER = 0;
    public static final int FEED_ORDER_ALPHABETICAL = 1;
    public static final int FEED_ORDER_MOST_PLAYED = 3;
    public static final String DEFAULT_PAGE_REMEMBER = "remember";

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

        createNoMediaFile();
    }

    public enum ThemePreference {
        LIGHT, DARK, BLACK, SYSTEM
    }

    public static void setTheme(ThemePreference theme) {
        switch (theme) {
            case LIGHT:
                prefs.edit().putString(PREF_THEME, "0").apply();
                break;
            case DARK:
                prefs.edit().putString(PREF_THEME, "1").apply();
                break;
            default:
                prefs.edit().putString(PREF_THEME, "system").apply();
                break;
        }
    }

    public static ThemePreference getTheme() {
        switch (prefs.getString(PREF_THEME, "system")) {
            case "0":
                return ThemePreference.LIGHT;
            case "1":
                return ThemePreference.DARK;
            default:
                return ThemePreference.SYSTEM;
        }
    }

    public static boolean getIsBlackTheme() {
        return prefs.getBoolean(PREF_THEME_BLACK, false);
    }

    public static boolean getIsThemeColorTinted() {
        return Build.VERSION.SDK_INT >= 31 && prefs.getBoolean(PREF_TINTED_COLORS, false);
    }

    public static List<String> getHiddenDrawerItems() {
        String hiddenItems = prefs.getString(PREF_HIDDEN_DRAWER_ITEMS, "");
        return new ArrayList<>(Arrays.asList(TextUtils.split(hiddenItems, ",")));
    }

    public static List<Integer> getCompactNotificationButtons() {
        String[] buttons = TextUtils.split(
                prefs.getString(PREF_COMPACT_NOTIFICATION_BUTTONS,
                        NOTIFICATION_BUTTON_REWIND + "," + NOTIFICATION_BUTTON_FAST_FORWARD),
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
        String value = prefs.getString(PREF_DRAWER_FEED_ORDER, "" + FEED_ORDER_COUNTER);
        return Integer.parseInt(value);
    }

    public static void setFeedOrder(String selected) {
        prefs.edit()
                .putString(PREF_DRAWER_FEED_ORDER, selected)
                .apply();
    }

    public static FeedCounter getFeedCounterSetting() {
        String value = prefs.getString(PREF_DRAWER_FEED_COUNTER, "" + FeedCounter.SHOW_NEW.id);
        return FeedCounter.fromOrdinal(Integer.parseInt(value));
    }

    /**
     * @return {@code true} if episodes should use their own cover, {@code false}  otherwise
     */
    public static boolean getUseEpisodeCoverSetting() {
        return prefs.getBoolean(PREF_USE_EPISODE_COVER, true);
    }

    /**
     * @return {@code true} if we should show remaining time or the duration
     */
    public static boolean shouldShowRemainingTime() {
        return prefs.getBoolean(PREF_SHOW_TIME_LEFT, false);
    }

    /**
     * Sets the preference for whether we show the remain time, if not show the duration. This will
     * send out events so the current playing screen, queue and the episode list would refresh
     *
     * @return {@code true} if we should show remaining time or the duration
     */
    public static void setShowRemainTimeSetting(Boolean showRemain) {
        prefs.edit().putBoolean(PREF_SHOW_TIME_LEFT, showRemain).apply();
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
     * Used for migration of the preference to system notification channels.
     */
    public static boolean getShowDownloadReportRaw() {
        return prefs.getBoolean(PREF_SHOW_DOWNLOAD_REPORT, true);
    }

    public static boolean enqueueDownloadedEpisodes() {
        return prefs.getBoolean(PREF_ENQUEUE_DOWNLOADED, true);
    }

    public enum EnqueueLocation {
        BACK, FRONT, AFTER_CURRENTLY_PLAYING, RANDOM
    }

    @NonNull
    public static EnqueueLocation getEnqueueLocation() {
        String valStr = prefs.getString(PREF_ENQUEUE_LOCATION, EnqueueLocation.BACK.name());
        try {
            return EnqueueLocation.valueOf(valStr);
        } catch (Throwable t) {
            // should never happen but just in case
            Log.e(TAG, "getEnqueueLocation: invalid value '" + valStr + "' Use default.", t);
            return EnqueueLocation.BACK;
        }
    }

    public static void setEnqueueLocation(@NonNull EnqueueLocation location) {
        prefs.edit()
                .putString(PREF_ENQUEUE_LOCATION, location.name())
                .apply();
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

    public static int getHardwareForwardButton() {
        return Integer.parseInt(prefs.getString(PREF_HARDWARE_FORWARD_BUTTON,
                String.valueOf(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)));
    }

    public static int getHardwarePreviousButton() {
        return Integer.parseInt(prefs.getString(PREF_HARDWARE_PREVIOUS_BUTTON,
                String.valueOf(KeyEvent.KEYCODE_MEDIA_REWIND)));
    }


    public static boolean isFollowQueue() {
        return prefs.getBoolean(PREF_FOLLOW_QUEUE, true);
    }

    /**
     * Set to true to enable Continuous Playback
     */
    @VisibleForTesting
    public static void setFollowQueue(boolean value) {
        prefs.edit().putBoolean(UserPreferences.PREF_FOLLOW_QUEUE, value).apply();
    }

    public static boolean shouldSkipKeepEpisode() { return prefs.getBoolean(PREF_SKIP_KEEPS_EPISODE, true); }

    public static boolean shouldFavoriteKeepEpisode() {
        return prefs.getBoolean(PREF_FAVORITE_KEEPS_EPISODE, true);
    }

    public static boolean isAutoDelete() {
        return prefs.getBoolean(PREF_AUTO_DELETE, false);
    }

    public static boolean isAutoDeleteLocal() {
        return prefs.getBoolean(PREF_AUTO_DELETE_LOCAL, false);
    }

    public static int getSmartMarkAsPlayedSecs() {
        return Integer.parseInt(prefs.getString(PREF_SMART_MARK_AS_PLAYED_SECS, "30"));
    }

    public static boolean shouldDeleteRemoveFromQueue() {
        return prefs.getBoolean(PREF_DELETE_REMOVES_FROM_QUEUE, false);
    }

    public static float getPlaybackSpeed(MediaType mediaType) {
        if (mediaType == MediaType.VIDEO) {
            return getVideoPlaybackSpeed();
        } else {
            return getAudioPlaybackSpeed();
        }
    }

    private static float getAudioPlaybackSpeed() {
        try {
            return Float.parseFloat(prefs.getString(PREF_PLAYBACK_SPEED, "1.00"));
        } catch (NumberFormatException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            UserPreferences.setPlaybackSpeed(1.0f);
            return 1.0f;
        }
    }

    private static float getVideoPlaybackSpeed() {
        try {
            return Float.parseFloat(prefs.getString(PREF_VIDEO_PLAYBACK_SPEED, "1.00"));
        } catch (NumberFormatException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            UserPreferences.setVideoPlaybackSpeed(1.0f);
            return 1.0f;
        }
    }

    public static boolean isSkipSilence() {
        return prefs.getBoolean(PREF_PLAYBACK_SKIP_SILENCE, false);
    }

    public static List<Float> getPlaybackSpeedArray() {
        return readPlaybackSpeedArray(prefs.getString(PREF_PLAYBACK_SPEED_ARRAY, null));
    }

    public static boolean shouldPauseForFocusLoss() {
        return prefs.getBoolean(PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, true);
    }

    public static long getUpdateInterval() {
        return Integer.parseInt(prefs.getString(PREF_UPDATE_INTERVAL, "12"));
    }

    public static boolean isAutoUpdateDisabled() {
        return getUpdateInterval() == 0;
    }

    private static boolean isAllowMobileFor(String type) {
        HashSet<String> defaultValue = new HashSet<>();
        defaultValue.add("images");
        Set<String> allowed = prefs.getStringSet(PREF_MOBILE_UPDATE, defaultValue);
        return allowed.contains(type);
    }

    public static boolean isAllowMobileFeedRefresh() {
        return isAllowMobileFor("feed_refresh");
    }

    public static boolean isAllowMobileSync() {
        return isAllowMobileFor("sync");
    }

    public static boolean isAllowMobileEpisodeDownload() {
        return isAllowMobileFor("episode_download");
    }

    public static boolean isAllowMobileAutoDownload() {
        return isAllowMobileFor("auto_download");
    }

    public static boolean isAllowMobileStreaming() {
        return isAllowMobileFor("streaming");
    }

    public static boolean isAllowMobileImages() {
        return isAllowMobileFor("images");
    }

    private static void setAllowMobileFor(String type, boolean allow) {
        HashSet<String> defaultValue = new HashSet<>();
        defaultValue.add("images");
        final Set<String> getValueStringSet = prefs.getStringSet(PREF_MOBILE_UPDATE, defaultValue);
        final Set<String> allowed = new HashSet<>(getValueStringSet);
        if (allow) {
            allowed.add(type);
        } else {
            allowed.remove(type);
        }
        prefs.edit().putStringSet(PREF_MOBILE_UPDATE, allowed).apply();
    }

    public static void setAllowMobileFeedRefresh(boolean allow) {
        setAllowMobileFor("feed_refresh", allow);
    }

    public static void setAllowMobileEpisodeDownload(boolean allow) {
        setAllowMobileFor("episode_download", allow);
    }

    public static void setAllowMobileAutoDownload(boolean allow) {
        setAllowMobileFor("auto_download", allow);
    }

    public static void setAllowMobileStreaming(boolean allow) {
        setAllowMobileFor("streaming", allow);
    }

    public static void setAllowMobileImages(boolean allow) {
        setAllowMobileFor("images", allow);
    }

    public static void setAllowMobileSync(boolean allow) {
        setAllowMobileFor("sync", allow);
    }

    /**
     * Returns the capacity of the episode cache. This method will return the
     * negative integer EPISODE_CACHE_SIZE_UNLIMITED if the cache size is set to
     * 'unlimited'.
     */
    public static int getEpisodeCacheSize() {
        return Integer.parseInt(prefs.getString(PREF_EPISODE_CACHE_SIZE, "20"));
    }

    public static boolean isEnableAutodownload() {
        return prefs.getBoolean(PREF_ENABLE_AUTODL, false);
    }

    @VisibleForTesting
    public static void setEnableAutodownload(boolean enabled) {
        prefs.edit().putBoolean(PREF_ENABLE_AUTODL, enabled).apply();
    }

    public static boolean isEnableAutodownloadOnBattery() {
        return prefs.getBoolean(PREF_ENABLE_AUTODL_ON_BATTERY, true);
    }

    public static boolean isEnableAutodownloadWifiFilter() {
        return Build.VERSION.SDK_INT < 29 && prefs.getBoolean(PREF_ENABLE_AUTODL_WIFI_FILTER, false);
    }

    public static int getFastForwardSecs() {
        return prefs.getInt(PREF_FAST_FORWARD_SECS, 30);
    }

    public static int getRewindSecs() {
        return prefs.getInt(PREF_REWIND_SECS, 10);
    }

    public static String[] getAutodownloadSelectedNetworks() {
        String selectedNetWorks = prefs.getString(PREF_AUTODL_SELECTED_NETWORKS, "");
        return TextUtils.split(selectedNetWorks, ",");
    }

    public static void setProxyConfig(ProxyConfig config) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_PROXY_TYPE, config.type.name());
        if (TextUtils.isEmpty(config.host)) {
            editor.remove(PREF_PROXY_HOST);
        } else {
            editor.putString(PREF_PROXY_HOST, config.host);
        }
        if (config.port <= 0 || config.port > 65535) {
            editor.remove(PREF_PROXY_PORT);
        } else {
            editor.putInt(PREF_PROXY_PORT, config.port);
        }
        if (TextUtils.isEmpty(config.username)) {
            editor.remove(PREF_PROXY_USER);
        } else {
            editor.putString(PREF_PROXY_USER, config.username);
        }
        if (TextUtils.isEmpty(config.password)) {
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

    public static void setPlaybackSpeed(float speed) {
        prefs.edit()
             .putString(PREF_PLAYBACK_SPEED, String.valueOf(speed))
             .apply();
    }

    public static void setVideoPlaybackSpeed(float speed) {
        prefs.edit()
                .putString(PREF_VIDEO_PLAYBACK_SPEED, String.valueOf(speed))
                .apply();
    }

    public static void setSkipSilence(boolean skipSilence) {
        prefs.edit()
                .putBoolean(PREF_PLAYBACK_SKIP_SILENCE, skipSilence)
                .apply();
    }

    public static void setPlaybackSpeedArray(List<Float> speeds) {
        DecimalFormatSymbols format = new DecimalFormatSymbols(Locale.US);
        format.setDecimalSeparator('.');
        DecimalFormat speedFormat = new DecimalFormat("0.00", format);
        JSONArray jsonArray = new JSONArray();
        for (float speed : speeds) {
            jsonArray.put(speedFormat.format(speed));
        }
        prefs.edit()
             .putString(PREF_PLAYBACK_SPEED_ARRAY, jsonArray.toString())
             .apply();
    }

    public static void setAutodownloadSelectedNetworks(String[] value) {
        prefs.edit()
             .putString(PREF_AUTODL_SELECTED_NETWORKS, TextUtils.join(",", value))
             .apply();
    }

    public static boolean gpodnetNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= 26) {
            return true; // System handles notification preferences
        }
        return prefs.getBoolean(PREF_GPODNET_NOTIFICATIONS, true);
    }

    /**
     * Used for migration of the preference to system notification channels.
     */
    public static boolean getGpodnetNotificationsEnabledRaw() {
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

    private static List<Float> readPlaybackSpeedArray(String valueFromPrefs) {
        if (valueFromPrefs != null) {
            try {
                JSONArray jsonArray = new JSONArray(valueFromPrefs);
                List<Float> selectedSpeeds = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    selectedSpeeds.add((float) jsonArray.getDouble(i));
                }
                return selectedSpeeds;
            } catch (JSONException e) {
                Log.e(TAG, "Got JSON error when trying to get speeds from JSONArray");
                e.printStackTrace();
            }
        }
        // If this preference hasn't been set yet, return the default options
        return Arrays.asList(1.0f, 1.25f, 1.5f);
    }

    public static int getEpisodeCleanupValue() {
        return Integer.parseInt(prefs.getString(PREF_EPISODE_CLEANUP, "" + EPISODE_CLEANUP_NULL));
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
     * @return The data folder that has been requested or null if the folder could not be created.
     */
    public static File getDataFolder(@Nullable String type) {
        File dataFolder = getTypeDir(prefs.getString(PREF_DATA_FOLDER, null), type);
        if (dataFolder == null || !dataFolder.canWrite()) {
            Log.d(TAG, "User data folder not writable or not set. Trying default.");
            dataFolder = context.getExternalFilesDir(type);
        }
        if (dataFolder == null || !dataFolder.canWrite()) {
            Log.d(TAG, "Default data folder not available or not writable. Falling back to internal memory.");
            dataFolder = getTypeDir(context.getFilesDir().getAbsolutePath(), type);
        }
        return dataFolder;
    }

    @Nullable
    private static File getTypeDir(@Nullable String baseDirPath, @Nullable String type) {
        if (baseDirPath == null) {
            return null;
        }
        File baseDir = new File(baseDirPath);
        File typeDir = type == null ? baseDir : new File(baseDir, type);
        if (!typeDir.exists()) {
            if (!baseDir.canWrite()) {
                Log.e(TAG, "Base dir is not writable " + baseDir.getAbsolutePath());
                return null;
            }
            if (!typeDir.mkdirs()) {
                Log.e(TAG, "Could not create type dir " + typeDir.getAbsolutePath());
                return null;
            }
        }
        return typeDir;
    }

    public static void setDataFolder(String dir) {
        Log.d(TAG, "setDataFolder(dir: " + dir + ")");
        prefs.edit()
             .putString(PREF_DATA_FOLDER, dir)
             .apply();
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

    public static String getDefaultPage() {
        return prefs.getString(PREF_DEFAULT_PAGE, "HomeFragment");
    }

    public static void setDefaultPage(String defaultPage) {
        prefs.edit().putString(PREF_DEFAULT_PAGE, defaultPage).apply();
    }

    public static boolean backButtonOpensDrawer() {
        return prefs.getBoolean(PREF_BACK_OPENS_DRAWER, false);
    }

    public static boolean timeRespectsSpeed() {
        return prefs.getBoolean(PREF_TIME_RESPECTS_SPEED, false);
    }

    public static boolean isStreamOverDownload() {
        return prefs.getBoolean(PREF_STREAM_OVER_DOWNLOAD, false);
    }

    public static void setStreamOverDownload(boolean stream) {
        prefs.edit().putBoolean(PREF_STREAM_OVER_DOWNLOAD, stream).apply();
    }

    /**
     * Returns if the queue is in keep sorted mode.
     *
     * @see #getQueueKeepSortedOrder()
     */
    public static boolean isQueueKeepSorted() {
        return prefs.getBoolean(PREF_QUEUE_KEEP_SORTED, false);
    }

    /**
     * Enables/disables the keep sorted mode of the queue.
     *
     * @see #setQueueKeepSortedOrder(SortOrder)
     */
    public static void setQueueKeepSorted(boolean keepSorted) {
        prefs.edit()
                .putBoolean(PREF_QUEUE_KEEP_SORTED, keepSorted)
                .apply();
    }

    /**
     * Returns the sort order for the queue keep sorted mode.
     * Note: This value is stored independently from the keep sorted state.
     *
     * @see #isQueueKeepSorted()
     */
    public static SortOrder getQueueKeepSortedOrder() {
        String sortOrderStr = prefs.getString(PREF_QUEUE_KEEP_SORTED_ORDER, "use-default");
        return SortOrder.parseWithDefault(sortOrderStr, SortOrder.DATE_NEW_OLD);
    }

    /**
     * Sets the sort order for the queue keep sorted mode.
     *
     * @see #setQueueKeepSorted(boolean)
     */
    public static void setQueueKeepSortedOrder(SortOrder sortOrder) {
        if (sortOrder == null) {
            return;
        }
        prefs.edit()
                .putString(PREF_QUEUE_KEEP_SORTED_ORDER, sortOrder.name())
                .apply();
    }

    public static FeedPreferences.NewEpisodesAction getNewEpisodesAction() {
        String str = prefs.getString(PREF_NEW_EPISODES_ACTION,
                "" + FeedPreferences.NewEpisodesAction.ADD_TO_INBOX.code);
        return FeedPreferences.NewEpisodesAction.fromCode(Integer.parseInt(str));
    }

    /**
     * Returns the sort order for the downloads.
     */
    public static SortOrder getDownloadsSortedOrder() {
        String sortOrderStr = prefs.getString(PREF_DOWNLOADS_SORTED_ORDER, "" + SortOrder.DATE_NEW_OLD.code);
        return SortOrder.fromCodeString(sortOrderStr);
    }

    /**
     * Sets the sort order for the downloads.
     */
    public static void setDownloadsSortedOrder(SortOrder sortOrder) {
        prefs.edit().putString(PREF_DOWNLOADS_SORTED_ORDER, "" + sortOrder.code).apply();
    }

    public static SortOrder getInboxSortedOrder() {
        String sortOrderStr = prefs.getString(PREF_INBOX_SORTED_ORDER, "" + SortOrder.DATE_NEW_OLD.code);
        return SortOrder.fromCodeString(sortOrderStr);
    }

    public static void setInboxSortedOrder(SortOrder sortOrder) {
        prefs.edit().putString(PREF_INBOX_SORTED_ORDER, "" + sortOrder.code).apply();
    }

    public static SubscriptionsFilter getSubscriptionsFilter() {
        String value = prefs.getString(PREF_FILTER_FEED, "");
        return new SubscriptionsFilter(value);
    }

    public static void setSubscriptionsFilter(SubscriptionsFilter value) {
        prefs.edit()
                .putString(PREF_FILTER_FEED, value.serialize())
                .apply();
    }

    public static boolean shouldShowSubscriptionTitle() {
        return prefs.getBoolean(PREF_SUBSCRIPTION_TITLE, false);
    }
}
