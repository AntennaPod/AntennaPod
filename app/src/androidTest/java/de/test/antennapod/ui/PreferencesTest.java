package de.test.antennapod.ui;

import android.content.Context;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;
import com.robotium.solo.Timeout;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.APCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.APNullCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.APQueueCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.EpisodeCleanupAlgorithm;

public class PreferencesTest extends ActivityInstrumentationTestCase2<PreferenceActivity>  {

    private static final String TAG = "PreferencesTest";

    private Solo solo;
    private Context context;
    private Resources res;

    public PreferencesTest() {
        super(PreferenceActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
        Timeout.setSmallTimeout(500);
        Timeout.setLargeTimeout(1000);
        context = getInstrumentation().getTargetContext();
        res = getActivity().getResources();
        UserPreferences.init(context);
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

    public void testSwitchTheme() {
        final int theme = UserPreferences.getTheme();
        int otherTheme;
        if(theme == de.danoeh.antennapod.core.R.style.Theme_AntennaPod_Light) {
            otherTheme = R.string.pref_theme_title_dark;
        } else {
            otherTheme = R.string.pref_theme_title_light;
        }
        solo.clickOnText(solo.getString(R.string.pref_set_theme_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(otherTheme));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getTheme() != theme, Timeout.getLargeTimeout()));
    }

    public void testSwitchThemeBack() {
        final int theme = UserPreferences.getTheme();
        int otherTheme;
        if(theme == de.danoeh.antennapod.core.R.style.Theme_AntennaPod_Light) {
            otherTheme = R.string.pref_theme_title_dark;
        } else {
            otherTheme = R.string.pref_theme_title_light;
        }
        solo.clickOnText(solo.getString(R.string.pref_set_theme_title));
        solo.waitForDialogToOpen(1000);
        solo.clickOnText(solo.getString(otherTheme));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getTheme() != theme, Timeout.getLargeTimeout()));
    }

    public void testExpandNotification() {
        final int priority = UserPreferences.getNotifyPriority();
        solo.clickOnText(solo.getString(R.string.pref_expandNotify_title));
        assertTrue(solo.waitForCondition(() -> priority != UserPreferences.getNotifyPriority(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_expandNotify_title));
        assertTrue(solo.waitForCondition(() -> priority == UserPreferences.getNotifyPriority(), Timeout.getLargeTimeout()));
    }

    public void testEnablePersistentPlaybackControls() {
        final boolean persistNotify = UserPreferences.isPersistNotify();
        solo.clickOnText(solo.getString(R.string.pref_persistNotify_title));
        assertTrue(solo.waitForCondition(() -> persistNotify != UserPreferences.isPersistNotify(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_persistNotify_title));
        assertTrue(solo.waitForCondition(() -> persistNotify == UserPreferences.isPersistNotify(), Timeout.getLargeTimeout()));
    }

    public void testSetLockscreenButtons() {
        String[] buttons = res.getStringArray(R.array.compact_notification_buttons_options);
        solo.clickOnText(solo.getString(R.string.pref_compact_notification_buttons_title));
        solo.waitForDialogToOpen(1000);
        // First uncheck every checkbox
        for (String button : buttons) {
            assertTrue(solo.searchText(button));
            if (solo.isTextChecked(button)) {
                solo.clickOnText(button);
            }
        }
        // Now try to check all checkboxes
        solo.clickOnText(buttons[0]);
        solo.clickOnText(buttons[1]);
        solo.clickOnText(buttons[2]);
        // Make sure that the third checkbox is unchecked
        assertTrue(!solo.isTextChecked(buttons[2]));
        solo.clickOnText(solo.getString(R.string.confirm_label));
        solo.waitForDialogToClose(1000);
        assertTrue(solo.waitForCondition(UserPreferences::showRewindOnCompactNotification, Timeout.getLargeTimeout()));
        assertTrue(solo.waitForCondition(UserPreferences::showFastForwardOnCompactNotification, Timeout.getLargeTimeout()));
        assertTrue(solo.waitForCondition(() -> !UserPreferences.showSkipOnCompactNotification(), Timeout.getLargeTimeout()));
    }

    public void testEnqueueAtFront() {
        final boolean enqueueAtFront = UserPreferences.enqueueAtFront();
        solo.clickOnText(solo.getString(R.string.pref_queueAddToFront_title));
        assertTrue(solo.waitForCondition(() -> enqueueAtFront != UserPreferences.enqueueAtFront(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_queueAddToFront_title));
        assertTrue(solo.waitForCondition(() -> enqueueAtFront == UserPreferences.enqueueAtFront(), Timeout.getLargeTimeout()));
    }

    public void testHeadPhonesDisconnect() {
        final boolean pauseOnHeadsetDisconnect = UserPreferences.isPauseOnHeadsetDisconnect();
        solo.clickOnText(solo.getString(R.string.pref_pauseOnHeadsetDisconnect_title));
        assertTrue(solo.waitForCondition(() -> pauseOnHeadsetDisconnect != UserPreferences.isPauseOnHeadsetDisconnect(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_pauseOnHeadsetDisconnect_title));
        assertTrue(solo.waitForCondition(() -> pauseOnHeadsetDisconnect == UserPreferences.isPauseOnHeadsetDisconnect(), Timeout.getLargeTimeout()));
    }

    public void testHeadPhonesReconnect() {
        if(UserPreferences.isPauseOnHeadsetDisconnect() == false) {
            solo.clickOnText(solo.getString(R.string.pref_pauseOnHeadsetDisconnect_title));
            assertTrue(solo.waitForCondition(UserPreferences::isPauseOnHeadsetDisconnect, Timeout.getLargeTimeout()));
        }
        final boolean unpauseOnHeadsetReconnect = UserPreferences.isUnpauseOnHeadsetReconnect();
        solo.clickOnText(solo.getString(R.string.pref_unpauseOnHeadsetReconnect_title));
        assertTrue(solo.waitForCondition(() -> unpauseOnHeadsetReconnect != UserPreferences.isUnpauseOnHeadsetReconnect(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_unpauseOnHeadsetReconnect_title));
        assertTrue(solo.waitForCondition(() -> unpauseOnHeadsetReconnect == UserPreferences.isUnpauseOnHeadsetReconnect(), Timeout.getLargeTimeout()));
    }

    public void testBluetoothReconnect() {
        if(UserPreferences.isPauseOnHeadsetDisconnect() == false) {
            solo.clickOnText(solo.getString(R.string.pref_pauseOnHeadsetDisconnect_title));
            assertTrue(solo.waitForCondition(UserPreferences::isPauseOnHeadsetDisconnect, Timeout.getLargeTimeout()));
        }
        final boolean unpauseOnBluetoothReconnect = UserPreferences.isUnpauseOnBluetoothReconnect();
        solo.clickOnText(solo.getString(R.string.pref_unpauseOnBluetoothReconnect_title));
        assertTrue(solo.waitForCondition(() -> unpauseOnBluetoothReconnect != UserPreferences.isUnpauseOnBluetoothReconnect(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_unpauseOnBluetoothReconnect_title));
        assertTrue(solo.waitForCondition(() -> unpauseOnBluetoothReconnect == UserPreferences.isUnpauseOnBluetoothReconnect(), Timeout.getLargeTimeout()));
    }

    public void testContinuousPlayback() {
        final boolean continuousPlayback = UserPreferences.isFollowQueue();
        solo.clickOnText(solo.getString(R.string.pref_followQueue_title));
        assertTrue(solo.waitForCondition(() -> continuousPlayback != UserPreferences.isFollowQueue(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_followQueue_title));
        assertTrue(solo.waitForCondition(() -> continuousPlayback == UserPreferences.isFollowQueue(), Timeout.getLargeTimeout()));
    }

    public void testAutoDelete() {
        final boolean autoDelete = UserPreferences.isAutoDelete();
        solo.clickOnText(solo.getString(R.string.pref_auto_delete_title));
        assertTrue(solo.waitForCondition(() -> autoDelete != UserPreferences.isAutoDelete(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_auto_delete_title));
        assertTrue(solo.waitForCondition(() -> autoDelete == UserPreferences.isAutoDelete(), Timeout.getLargeTimeout()));
    }

    public void testPlaybackSpeeds() {
        solo.clickOnText(solo.getString(R.string.pref_playback_speed_title));
        solo.waitForDialogToOpen(1000);
        assertTrue(solo.searchText(res.getStringArray(R.array.playback_speed_values)[0]));
        solo.clickOnText(solo.getString(R.string.cancel_label));
        solo.waitForDialogToClose(1000);
    }

    public void testPauseForInterruptions() {
        final boolean pauseForFocusLoss = UserPreferences.shouldPauseForFocusLoss();
        solo.clickOnText(solo.getString(R.string.pref_pausePlaybackForFocusLoss_title));
        assertTrue(solo.waitForCondition(() -> pauseForFocusLoss != UserPreferences.shouldPauseForFocusLoss(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_pausePlaybackForFocusLoss_title));
        assertTrue(solo.waitForCondition(() -> pauseForFocusLoss == UserPreferences.shouldPauseForFocusLoss(), Timeout.getLargeTimeout()));
    }

    public void testDisableUpdateInterval() {
        solo.clickOnText(solo.getString(R.string.pref_autoUpdateIntervallOrTime_sum));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.pref_autoUpdateIntervallOrTime_Disable));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getUpdateInterval() == 0, 1000));
    }

    public void testSetUpdateInterval() {
        solo.clickOnText(solo.getString(R.string.pref_autoUpdateIntervallOrTime_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.pref_autoUpdateIntervallOrTime_Interval));
        solo.waitForDialogToOpen();
        String search = "12 " + solo.getString(R.string.pref_update_interval_hours_plural);
        solo.clickOnText(search);
        solo.waitForDialogToClose();
        assertTrue(solo.waitForCondition(() -> UserPreferences.getUpdateInterval() ==
                TimeUnit.HOURS.toMillis(12), Timeout.getLargeTimeout()));
    }

    public void testMobileUpdates() {
        final boolean mobileUpdates = UserPreferences.isAllowMobileUpdate();
        solo.clickOnText(solo.getString(R.string.pref_mobileUpdate_title));
        assertTrue(solo.waitForCondition(() -> mobileUpdates != UserPreferences.isAllowMobileUpdate(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_mobileUpdate_title));
        assertTrue(solo.waitForCondition(() -> mobileUpdates == UserPreferences.isAllowMobileUpdate(), Timeout.getLargeTimeout()));
    }

    public void testSetSequentialDownload() {
        solo.clickOnText(solo.getString(R.string.pref_parallel_downloads_title));
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "1");
        solo.clickOnText(solo.getString(android.R.string.ok));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getParallelDownloads() == 1, Timeout.getLargeTimeout()));
    }

    public void testSetParallelDownloads() {
        solo.clickOnText(solo.getString(R.string.pref_parallel_downloads_title));
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "10");
        solo.clickOnText(solo.getString(android.R.string.ok));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getParallelDownloads() == 10, Timeout.getLargeTimeout()));
    }

    public void testSetParallelDownloadsInvalidInput() {
        solo.clickOnText(solo.getString(R.string.pref_parallel_downloads_title));
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "0");
        assertEquals("1", solo.getEditText(0).getText().toString());
        solo.clearEditText(0);
        solo.enterText(0, "100");
        assertEquals("50", solo.getEditText(0).getText().toString());
    }

    public void testSetEpisodeCache() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String entry = entries[entries.length/2];
        final int value = Integer.valueOf(values[values.length/2]);
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        solo.waitForText(solo.getString(R.string.pref_automatic_download_title));
        solo.clickOnText(solo.getString(R.string.pref_episode_cache_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(entry);
        assertTrue(solo.waitForCondition(() -> UserPreferences.getEpisodeCacheSize() == value, Timeout.getLargeTimeout()));
    }

    public void testSetEpisodeCacheMin() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String minEntry = entries[0];
        final int minValue = Integer.valueOf(values[0]);
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        solo.waitForText(solo.getString(R.string.pref_automatic_download_title));
        if(!UserPreferences.isEnableAutodownload()) {
            solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        }
        solo.clickOnText(solo.getString(R.string.pref_episode_cache_title));
        solo.waitForDialogToOpen(1000);
        solo.scrollUp();
        solo.clickOnText(minEntry);
        assertTrue(solo.waitForCondition(() -> UserPreferences.getEpisodeCacheSize() == minValue, Timeout.getLargeTimeout()));
    }

    public void testSetEpisodeCacheMax() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String maxEntry = entries[entries.length-1];
        final int maxValue = Integer.valueOf(values[values.length-1]);
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        solo.waitForText(solo.getString(R.string.pref_automatic_download_title));
        if(!UserPreferences.isEnableAutodownload()) {
            solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        }
        solo.clickOnText(solo.getString(R.string.pref_episode_cache_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(maxEntry);
        assertTrue(solo.waitForCondition(() -> UserPreferences.getEpisodeCacheSize() == maxValue, Timeout.getLargeTimeout()));
    }

    public void testAutomaticDownload() {
        final boolean automaticDownload = UserPreferences.isEnableAutodownload();
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        solo.waitForText(solo.getString(R.string.pref_automatic_download_title));
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        assertTrue(solo.waitForCondition(() -> automaticDownload != UserPreferences.isEnableAutodownload(), Timeout.getLargeTimeout()));
        if(UserPreferences.isEnableAutodownload() == false) {
            solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        }
        assertTrue(solo.waitForCondition(() -> UserPreferences.isEnableAutodownload() == true, Timeout.getLargeTimeout()));
        final boolean enableAutodownloadOnBattery = UserPreferences.isEnableAutodownloadOnBattery();
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_on_battery_title));
        assertTrue(solo.waitForCondition(() -> enableAutodownloadOnBattery != UserPreferences.isEnableAutodownloadOnBattery(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_on_battery_title));
        assertTrue(solo.waitForCondition(() -> enableAutodownloadOnBattery == UserPreferences.isEnableAutodownloadOnBattery(), Timeout.getLargeTimeout()));
        final boolean enableWifiFilter = UserPreferences.isEnableAutodownloadWifiFilter();
        solo.clickOnText(solo.getString(R.string.pref_autodl_wifi_filter_title));
        assertTrue(solo.waitForCondition(() -> enableWifiFilter != UserPreferences.isEnableAutodownloadWifiFilter(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_autodl_wifi_filter_title));
        assertTrue(solo.waitForCondition(() -> enableWifiFilter == UserPreferences.isEnableAutodownloadWifiFilter(), Timeout.getLargeTimeout()));
    }

    public void testEpisodeCleanupQueueOnly() {
        solo.clickOnText(solo.getString(R.string.pref_episode_cleanup_title));
        solo.waitForText(solo.getString(R.string.episode_cleanup_queue_removal));
        solo.clickOnText(solo.getString(R.string.episode_cleanup_queue_removal));
        assertTrue(solo.waitForCondition(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    return alg instanceof APQueueCleanupAlgorithm;
                },
                Timeout.getLargeTimeout()));
    }

    public void testEpisodeCleanupNeverAlg() {
        solo.clickOnText(solo.getString(R.string.pref_episode_cleanup_title));
        solo.waitForText(solo.getString(R.string.episode_cleanup_never));
        solo.clickOnText(solo.getString(R.string.episode_cleanup_never));
        assertTrue(solo.waitForCondition(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    return alg instanceof APNullCleanupAlgorithm;
                },
                Timeout.getLargeTimeout()));
    }

    public void testEpisodeCleanupClassic() {
        solo.clickOnText(solo.getString(R.string.pref_episode_cleanup_title));
        solo.waitForText(solo.getString(R.string.episode_cleanup_after_listening));
        solo.clickOnText(solo.getString(R.string.episode_cleanup_after_listening));
        assertTrue(solo.waitForCondition(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    if (alg instanceof APCleanupAlgorithm) {
                        APCleanupAlgorithm cleanupAlg = (APCleanupAlgorithm)alg;
                        return cleanupAlg.getNumberOfDaysAfterPlayback() == 0;
                    }
                    return false;
                },
                Timeout.getLargeTimeout()));
    }

    public void testEpisodeCleanupNumDays() {
        solo.clickOnText(solo.getString(R.string.pref_episode_cleanup_title));
        solo.waitForText(solo.getString(R.string.episode_cleanup_after_listening));
        solo.clickOnText("5");
        assertTrue(solo.waitForCondition(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    if (alg instanceof APCleanupAlgorithm) {
                        APCleanupAlgorithm cleanupAlg = (APCleanupAlgorithm)alg;
                        return cleanupAlg.getNumberOfDaysAfterPlayback() == 5;
                    }
                    return false;
                },
                Timeout.getLargeTimeout()));
    }


    public void testRewindChange() {
        int seconds = UserPreferences.getRewindSecs();
        int deltas[] = res.getIntArray(R.array.seek_delta_values);

        solo.clickOnText(solo.getString(R.string.pref_rewind));
        solo.waitForDialogToOpen();

        int currentIndex = Arrays.binarySearch(deltas, seconds);
        assertTrue(currentIndex >= 0 && currentIndex < deltas.length);  // found?

        // Find next value (wrapping around to next)
        int newIndex = (currentIndex + 1) % deltas.length;

        solo.clickOnText(String.valueOf(deltas[newIndex]) + " seconds");
        solo.clickOnButton("Confirm");

        solo.waitForDialogToClose();
        assertTrue(solo.waitForCondition(() -> UserPreferences.getRewindSecs() == deltas[newIndex],
                Timeout.getLargeTimeout()));
    }

    public void testFastForwardChange() {
        for (int i = 2; i > 0; i--) { // repeat twice to catch any error where fastforward is tracking rewind
            int seconds = UserPreferences.getFastForwardSecs();
            int deltas[] = res.getIntArray(R.array.seek_delta_values);

            solo.clickOnText(solo.getString(R.string.pref_fast_forward));
            solo.waitForDialogToOpen();

            int currentIndex = Arrays.binarySearch(deltas, seconds);
            assertTrue(currentIndex >= 0 && currentIndex < deltas.length);  // found?

            // Find next value (wrapping around to next)
            int newIndex = (currentIndex + 1) % deltas.length;

            solo.clickOnText(String.valueOf(deltas[newIndex]) + " seconds");
            solo.clickOnButton("Confirm");

            solo.waitForDialogToClose();
            assertTrue(solo.waitForCondition(() -> UserPreferences.getFastForwardSecs() == deltas[newIndex],
                    Timeout.getLargeTimeout()));
        }
    }
}
