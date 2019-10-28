package de.test.antennapod.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.robotium.solo.Solo;
import com.robotium.solo.Timeout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.APCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.APNullCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.APQueueCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.EpisodeCleanupAlgorithm;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.test.antennapod.EspressoTestUtils;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.clickPreference;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@LargeTest
public class PreferencesTest {
    private Solo solo;
    private Resources res;

    @Rule
    public ActivityTestRule<PreferenceActivity> mActivityRule = new ActivityTestRule<>(PreferenceActivity.class, false, false);

    @Before
    public void setUp() {
        EspressoTestUtils.clearDatabase();
        EspressoTestUtils.clearPreferences();
        mActivityRule.launchActivity(new Intent());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivityRule.getActivity());
        prefs.edit().putBoolean(UserPreferences.PREF_ENABLE_AUTODL, true).commit();

        solo = new Solo(getInstrumentation(), mActivityRule.getActivity());
        Timeout.setSmallTimeout(500);
        Timeout.setLargeTimeout(1000);
        res = mActivityRule.getActivity().getResources();
        UserPreferences.init(mActivityRule.getActivity());
    }

    @Test
    public void testSwitchTheme() {
        final int theme = UserPreferences.getTheme();
        int otherTheme;
        if(theme == de.danoeh.antennapod.core.R.style.Theme_AntennaPod_Light) {
            otherTheme = R.string.pref_theme_title_dark;
        } else {
            otherTheme = R.string.pref_theme_title_light;
        }
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_set_theme_title);
        onView(withText(otherTheme)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getTheme() != theme, Timeout.getLargeTimeout()));
    }

    @Test
    public void testSwitchThemeBack() {
        final int theme = UserPreferences.getTheme();
        int otherTheme;
        if(theme == de.danoeh.antennapod.core.R.style.Theme_AntennaPod_Light) {
            otherTheme = R.string.pref_theme_title_dark;
        } else {
            otherTheme = R.string.pref_theme_title_light;
        }
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_set_theme_title);
        onView(withText(otherTheme)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getTheme() != theme, Timeout.getLargeTimeout()));
    }

    @Test
    public void testEnablePersistentPlaybackControls() {
        final boolean persistNotify = UserPreferences.isPersistNotify();
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_persistNotify_title);
        assertTrue(solo.waitForCondition(() -> persistNotify != UserPreferences.isPersistNotify(), Timeout.getLargeTimeout()));
        clickPreference(R.string.pref_persistNotify_title);
        assertTrue(solo.waitForCondition(() -> persistNotify == UserPreferences.isPersistNotify(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetLockscreenButtons() {
        clickPreference(R.string.user_interface_label);
        String[] buttons = res.getStringArray(R.array.compact_notification_buttons_options);
        clickPreference(R.string.pref_compact_notification_buttons_title);
        solo.waitForDialogToOpen(1000);
        // First uncheck checkbox
        onView(withText(buttons[2])).perform(click());

        // Now try to check all checkboxes
        onView(withText(buttons[0])).perform(click());
        onView(withText(buttons[1])).perform(click());
        onView(withText(buttons[2])).perform(click());

        // Make sure that the third checkbox is unchecked
        assertTrue(!solo.isTextChecked(buttons[2]));
        onView(withText(R.string.confirm_label)).perform(click());
        solo.waitForDialogToClose(1000);
        assertTrue(solo.waitForCondition(UserPreferences::showRewindOnCompactNotification, Timeout.getLargeTimeout()));
        assertTrue(solo.waitForCondition(UserPreferences::showFastForwardOnCompactNotification, Timeout.getLargeTimeout()));
        assertTrue(solo.waitForCondition(() -> !UserPreferences.showSkipOnCompactNotification(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testEnqueueLocation() {
        clickPreference(R.string.playback_pref);
        // TODO-2652: implement the test
    }

    @Test
    public void testHeadPhonesDisconnect() {
        onView(withText(R.string.playback_pref)).perform(click());
        final boolean pauseOnHeadsetDisconnect = UserPreferences.isPauseOnHeadsetDisconnect();
        onView(withText(R.string.pref_pauseOnHeadsetDisconnect_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> pauseOnHeadsetDisconnect != UserPreferences.isPauseOnHeadsetDisconnect(), Timeout.getLargeTimeout()));
        onView(withText(R.string.pref_pauseOnHeadsetDisconnect_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> pauseOnHeadsetDisconnect == UserPreferences.isPauseOnHeadsetDisconnect(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testHeadPhonesReconnect() {
        onView(withText(R.string.playback_pref)).perform(click());
        if(UserPreferences.isPauseOnHeadsetDisconnect() == false) {
            onView(withText(R.string.pref_pauseOnHeadsetDisconnect_title)).perform(click());
            assertTrue(solo.waitForCondition(UserPreferences::isPauseOnHeadsetDisconnect, Timeout.getLargeTimeout()));
        }
        final boolean unpauseOnHeadsetReconnect = UserPreferences.isUnpauseOnHeadsetReconnect();
        onView(withText(R.string.pref_unpauseOnHeadsetReconnect_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> unpauseOnHeadsetReconnect != UserPreferences.isUnpauseOnHeadsetReconnect(), Timeout.getLargeTimeout()));
        onView(withText(R.string.pref_unpauseOnHeadsetReconnect_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> unpauseOnHeadsetReconnect == UserPreferences.isUnpauseOnHeadsetReconnect(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testBluetoothReconnect() {
        onView(withText(R.string.playback_pref)).perform(click());
        if(UserPreferences.isPauseOnHeadsetDisconnect() == false) {
            onView(withText(R.string.pref_pauseOnHeadsetDisconnect_title)).perform(click());
            assertTrue(solo.waitForCondition(UserPreferences::isPauseOnHeadsetDisconnect, Timeout.getLargeTimeout()));
        }
        final boolean unpauseOnBluetoothReconnect = UserPreferences.isUnpauseOnBluetoothReconnect();
        onView(withText(R.string.pref_unpauseOnBluetoothReconnect_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> unpauseOnBluetoothReconnect != UserPreferences.isUnpauseOnBluetoothReconnect(), Timeout.getLargeTimeout()));
        onView(withText(R.string.pref_unpauseOnBluetoothReconnect_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> unpauseOnBluetoothReconnect == UserPreferences.isUnpauseOnBluetoothReconnect(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testContinuousPlayback() {
        clickPreference(R.string.playback_pref);
        final boolean continuousPlayback = UserPreferences.isFollowQueue();
        clickPreference(R.string.pref_followQueue_title);
        assertTrue(solo.waitForCondition(() -> continuousPlayback != UserPreferences.isFollowQueue(), Timeout.getLargeTimeout()));
        clickPreference(R.string.pref_followQueue_title);
        assertTrue(solo.waitForCondition(() -> continuousPlayback == UserPreferences.isFollowQueue(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testAutoDelete() {
        onView(withText(R.string.storage_pref)).perform(click());
        final boolean autoDelete = UserPreferences.isAutoDelete();
        onView(withText(R.string.pref_auto_delete_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> autoDelete != UserPreferences.isAutoDelete(), Timeout.getLargeTimeout()));
        onView(withText(R.string.pref_auto_delete_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> autoDelete == UserPreferences.isAutoDelete(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testPlaybackSpeeds() {
        clickPreference(R.string.playback_pref);
        clickPreference(R.string.media_player);
        onView(withText(R.string.media_player_exoplayer)).perform(click());
        clickPreference(R.string.pref_playback_speed_title);
        solo.waitForDialogToOpen();
        onView(withText("0.50")).check(matches(isDisplayed()));
        onView(withText(R.string.cancel_label)).perform(click());
    }

    @Test
    public void testPauseForInterruptions() {
        onView(withText(R.string.playback_pref)).perform(click());
        final boolean pauseForFocusLoss = UserPreferences.shouldPauseForFocusLoss();
        onView(withText(R.string.pref_pausePlaybackForFocusLoss_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> pauseForFocusLoss != UserPreferences.shouldPauseForFocusLoss(), Timeout.getLargeTimeout()));
        onView(withText(R.string.pref_pausePlaybackForFocusLoss_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> pauseForFocusLoss == UserPreferences.shouldPauseForFocusLoss(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testDisableUpdateInterval() {
        onView(withText(R.string.network_pref)).perform(click());
        onView(withText(R.string.pref_autoUpdateIntervallOrTime_title)).perform(click());
        onView(withText(R.string.pref_autoUpdateIntervallOrTime_Disable)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getUpdateInterval() == 0, 1000));
    }

    @Test
    public void testSetUpdateInterval() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_autoUpdateIntervallOrTime_title);
        onView(withText(R.string.pref_autoUpdateIntervallOrTime_Interval)).perform(click());
        String search = "12 " + solo.getString(R.string.pref_update_interval_hours_plural);
        onView(withText(search)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getUpdateInterval() ==
                TimeUnit.HOURS.toMillis(12), Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetSequentialDownload() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_parallel_downloads_title);
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "1");
        onView(withText(android.R.string.ok)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getParallelDownloads() == 1, Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetParallelDownloads() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_parallel_downloads_title);
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "10");
        onView(withText(android.R.string.ok)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getParallelDownloads() == 10, Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetParallelDownloadsInvalidInput() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_parallel_downloads_title);
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "0");
        assertEquals("", solo.getEditText(0).getText().toString());
        solo.clearEditText(0);
        solo.enterText(0, "100");
        assertEquals("", solo.getEditText(0).getText().toString());
    }

    @Test
    public void testSetEpisodeCache() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String entry = entries[entries.length/2];
        final int value = Integer.valueOf(values[values.length/2]);
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_automatic_download_title);
        clickPreference(R.string.pref_episode_cache_title);
        solo.waitForDialogToOpen();
        solo.clickOnText(entry);
        assertTrue(solo.waitForCondition(() -> UserPreferences.getEpisodeCacheSize() == value, Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetEpisodeCacheMin() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String minEntry = entries[0];
        final int minValue = Integer.valueOf(values[0]);

        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_automatic_download_title);
        clickPreference(R.string.pref_episode_cache_title);
        solo.scrollUp();
        onView(withText(minEntry)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getEpisodeCacheSize() == minValue, Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetEpisodeCacheMax() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String maxEntry = entries[entries.length-1];
        final int maxValue = Integer.valueOf(values[values.length-1]);
        onView(withText(R.string.network_pref)).perform(click());
        onView(withText(R.string.pref_automatic_download_title)).perform(click());
        onView(withText(R.string.pref_episode_cache_title)).perform(click());
        onView(withText(maxEntry)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getEpisodeCacheSize() == maxValue, Timeout.getLargeTimeout()));
    }

    @Test
    public void testAutomaticDownload() {
        final boolean automaticDownload = UserPreferences.isEnableAutodownload();
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_automatic_download_title);
        clickPreference(R.string.pref_automatic_download_title);

        assertTrue(solo.waitForCondition(() -> automaticDownload != UserPreferences.isEnableAutodownload(), Timeout.getLargeTimeout()));
        if(UserPreferences.isEnableAutodownload() == false) {
            clickPreference(R.string.pref_automatic_download_title);
        }
        assertTrue(solo.waitForCondition(() -> UserPreferences.isEnableAutodownload() == true, Timeout.getLargeTimeout()));
        final boolean enableAutodownloadOnBattery = UserPreferences.isEnableAutodownloadOnBattery();
        onView(withText(R.string.pref_automatic_download_on_battery_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> enableAutodownloadOnBattery != UserPreferences.isEnableAutodownloadOnBattery(), Timeout.getLargeTimeout()));
        onView(withText(R.string.pref_automatic_download_on_battery_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> enableAutodownloadOnBattery == UserPreferences.isEnableAutodownloadOnBattery(), Timeout.getLargeTimeout()));
        final boolean enableWifiFilter = UserPreferences.isEnableAutodownloadWifiFilter();
        onView(withText(R.string.pref_autodl_wifi_filter_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> enableWifiFilter != UserPreferences.isEnableAutodownloadWifiFilter(), Timeout.getLargeTimeout()));
        onView(withText(R.string.pref_autodl_wifi_filter_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> enableWifiFilter == UserPreferences.isEnableAutodownloadWifiFilter(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testEpisodeCleanupQueueOnly() {
        onView(withText(R.string.network_pref)).perform(click());
        onView(withText(R.string.pref_automatic_download_title)).perform(click());
        onView(withText(R.string.pref_episode_cleanup_title)).perform(click());
        solo.waitForText(solo.getString(R.string.episode_cleanup_queue_removal));
        onView(withText(R.string.episode_cleanup_queue_removal)).perform(click());
        assertTrue(solo.waitForCondition(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    return alg instanceof APQueueCleanupAlgorithm;
                },
                Timeout.getLargeTimeout()));
    }

    @Test
    public void testEpisodeCleanupNeverAlg() {
        onView(withText(R.string.network_pref)).perform(click());
        onView(withText(R.string.pref_automatic_download_title)).perform(click());
        onView(withText(R.string.pref_episode_cleanup_title)).perform(click());
        solo.waitForText(solo.getString(R.string.episode_cleanup_never));
        onView(withText(R.string.episode_cleanup_never)).perform(click());
        assertTrue(solo.waitForCondition(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    return alg instanceof APNullCleanupAlgorithm;
                },
                Timeout.getLargeTimeout()));
    }

    @Test
    public void testEpisodeCleanupClassic() {
        onView(withText(R.string.network_pref)).perform(click());
        onView(withText(R.string.pref_automatic_download_title)).perform(click());
        onView(withText(R.string.pref_episode_cleanup_title)).perform(click());
        solo.waitForText(solo.getString(R.string.episode_cleanup_after_listening));
        onView(withText(R.string.episode_cleanup_after_listening)).perform(click());
        assertTrue(solo.waitForCondition(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    if (alg instanceof APCleanupAlgorithm) {
                        APCleanupAlgorithm cleanupAlg = (APCleanupAlgorithm)alg;
                        return cleanupAlg.getNumberOfHoursAfterPlayback() == 0;
                    }
                    return false;
                },
                Timeout.getLargeTimeout()));
    }

    @Test
    public void testEpisodeCleanupNumDays() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_automatic_download_title);
        clickPreference(R.string.pref_episode_cleanup_title);
        solo.waitForDialogToOpen();
        String search = res.getQuantityString(R.plurals.episode_cleanup_days_after_listening, 5, 5);
        onView(withText(search)).perform(click());
        assertTrue(solo.waitForCondition(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    if (alg instanceof APCleanupAlgorithm) {
                        APCleanupAlgorithm cleanupAlg = (APCleanupAlgorithm)alg;
                        return cleanupAlg.getNumberOfHoursAfterPlayback() == 120; // 5 days
                    }
                    return false;
                },
                Timeout.getLargeTimeout()));
    }

    @Test
    public void testRewindChange() {
        int seconds = UserPreferences.getRewindSecs();
        int deltas[] = res.getIntArray(R.array.seek_delta_values);

        clickPreference(R.string.playback_pref);
        clickPreference(R.string.pref_rewind);

        int currentIndex = Arrays.binarySearch(deltas, seconds);
        assertTrue(currentIndex >= 0 && currentIndex < deltas.length);  // found?

        // Find next value (wrapping around to next)
        int newIndex = (currentIndex + 1) % deltas.length;
        onView(withText(String.valueOf(deltas[newIndex]) + " seconds")).perform(click());
        onView(withText("Confirm")).perform(click());

        assertTrue(solo.waitForCondition(() -> UserPreferences.getRewindSecs() == deltas[newIndex],
                Timeout.getLargeTimeout()));
    }

    @Test
    public void testFastForwardChange() {
        clickPreference(R.string.playback_pref);
        for (int i = 2; i > 0; i--) { // repeat twice to catch any error where fastforward is tracking rewind
            int seconds = UserPreferences.getFastForwardSecs();
            int deltas[] = res.getIntArray(R.array.seek_delta_values);

            clickPreference(R.string.pref_fast_forward);

            int currentIndex = Arrays.binarySearch(deltas, seconds);
            assertTrue(currentIndex >= 0 && currentIndex < deltas.length);  // found?

            // Find next value (wrapping around to next)
            int newIndex = (currentIndex + 1) % deltas.length;

            onView(withText(deltas[newIndex] + " seconds")).perform(click());
            onView(withText("Confirm")).perform(click());

            solo.waitForDialogToClose();
            assertTrue(solo.waitForCondition(() -> UserPreferences.getFastForwardSecs() == deltas[newIndex],
                    Timeout.getLargeTimeout()));
        }
    }

    @Test
    public void testBackButtonBehaviorGoToPageSelector() {
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_back_button_behavior_title);
        onView(withText(R.string.back_button_go_to_page)).perform(click());
        onView(withText(R.string.queue_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonBehavior() == UserPreferences.BackButtonBehavior.GO_TO_PAGE,
                Timeout.getLargeTimeout()));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonGoToPage().equals(QueueFragment.TAG),
                Timeout.getLargeTimeout()));
        clickPreference(R.string.pref_back_button_behavior_title);
        onView(withText(R.string.back_button_go_to_page)).perform(click());
        onView(withText(R.string.episodes_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonBehavior() == UserPreferences.BackButtonBehavior.GO_TO_PAGE,
                Timeout.getLargeTimeout()));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonGoToPage().equals(EpisodesFragment.TAG),
                Timeout.getLargeTimeout()));
        clickPreference(R.string.pref_back_button_behavior_title);
        onView(withText(R.string.back_button_go_to_page)).perform(click());
        onView(withText(R.string.subscriptions_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonBehavior() == UserPreferences.BackButtonBehavior.GO_TO_PAGE,
                Timeout.getLargeTimeout()));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonGoToPage().equals(SubscriptionFragment.TAG),
                Timeout.getLargeTimeout()));
    }

    @Test
    public void testDeleteRemovesFromQueue() {
        clickPreference(R.string.storage_pref);
        if (!UserPreferences.shouldDeleteRemoveFromQueue()) {
            clickPreference(R.string.pref_delete_removes_from_queue_title);
            assertTrue(solo.waitForCondition(UserPreferences::shouldDeleteRemoveFromQueue, Timeout.getLargeTimeout()));
        }
        final boolean deleteRemovesFromQueue = UserPreferences.shouldDeleteRemoveFromQueue();
        onView(withText(R.string.pref_delete_removes_from_queue_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> deleteRemovesFromQueue != UserPreferences.shouldDeleteRemoveFromQueue(), Timeout.getLargeTimeout()));
        onView(withText(R.string.pref_delete_removes_from_queue_title)).perform(click());
        assertTrue(solo.waitForCondition(() -> deleteRemovesFromQueue == UserPreferences.shouldDeleteRemoveFromQueue(), Timeout.getLargeTimeout()));
    }
}
