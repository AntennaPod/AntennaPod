package de.test.antennapod.ui;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import android.view.View;

import com.robotium.solo.Solo;
import com.robotium.solo.Timeout;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PreferencesTest {
    private Solo solo;
    private Resources res;
    private SharedPreferences prefs;

    @Rule
    public ActivityTestRule<PreferenceActivity> mActivityRule = new ActivityTestRule<>(PreferenceActivity.class);

    @Before
    public void setUp() {
        solo = new Solo(getInstrumentation(), mActivityRule.getActivity());
        Timeout.setSmallTimeout(500);
        Timeout.setLargeTimeout(1000);
        res = mActivityRule.getActivity().getResources();
        UserPreferences.init(mActivityRule.getActivity());

        prefs = PreferenceManager.getDefaultSharedPreferences(mActivityRule.getActivity());
        prefs.edit().clear();
        prefs.edit().putBoolean(UserPreferences.PREF_ENABLE_AUTODL, true).commit();
    }

    @After
    public void tearDown() {
        solo.finishOpenedActivities();
        prefs.edit().clear();
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
        clickPreference(withText(R.string.user_interface_label));
        clickPreference(withText(R.string.pref_set_theme_title));
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
        clickPreference(withText(R.string.user_interface_label));
        clickPreference(withText(R.string.pref_set_theme_title));
        onView(withText(otherTheme)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getTheme() != theme, Timeout.getLargeTimeout()));
    }

    @Test
    public void testEnablePersistentPlaybackControls() {
        final boolean persistNotify = UserPreferences.isPersistNotify();
        clickPreference(withText(R.string.user_interface_label));
        clickPreference(withText(R.string.pref_persistNotify_title));
        assertTrue(solo.waitForCondition(() -> persistNotify != UserPreferences.isPersistNotify(), Timeout.getLargeTimeout()));
        clickPreference(withText(R.string.pref_persistNotify_title));
        assertTrue(solo.waitForCondition(() -> persistNotify == UserPreferences.isPersistNotify(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetLockscreenButtons() {
        solo.clickOnText(solo.getString(R.string.user_interface_label));
        solo.scrollDown();
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

    @Test
    public void testEnqueueAtFront() {
        solo.clickOnText(solo.getString(R.string.playback_pref));
        final boolean enqueueAtFront = UserPreferences.enqueueAtFront();
        solo.scrollDown();
        solo.scrollDown();
        solo.clickOnText(solo.getString(R.string.pref_queueAddToFront_title));
        assertTrue(solo.waitForCondition(() -> enqueueAtFront != UserPreferences.enqueueAtFront(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_queueAddToFront_title));
        assertTrue(solo.waitForCondition(() -> enqueueAtFront == UserPreferences.enqueueAtFront(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testHeadPhonesDisconnect() {
        solo.clickOnText(solo.getString(R.string.playback_pref));
        final boolean pauseOnHeadsetDisconnect = UserPreferences.isPauseOnHeadsetDisconnect();
        solo.clickOnText(solo.getString(R.string.pref_pauseOnHeadsetDisconnect_title));
        assertTrue(solo.waitForCondition(() -> pauseOnHeadsetDisconnect != UserPreferences.isPauseOnHeadsetDisconnect(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_pauseOnHeadsetDisconnect_title));
        assertTrue(solo.waitForCondition(() -> pauseOnHeadsetDisconnect == UserPreferences.isPauseOnHeadsetDisconnect(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testHeadPhonesReconnect() {
        solo.clickOnText(solo.getString(R.string.playback_pref));
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

    @Test
    public void testBluetoothReconnect() {
        solo.clickOnText(solo.getString(R.string.playback_pref));
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

    @Test
    public void testContinuousPlayback() {
        solo.clickOnText(solo.getString(R.string.playback_pref));
        final boolean continuousPlayback = UserPreferences.isFollowQueue();
        solo.scrollDown();
        solo.scrollDown();
        solo.clickOnText(solo.getString(R.string.pref_followQueue_title));
        assertTrue(solo.waitForCondition(() -> continuousPlayback != UserPreferences.isFollowQueue(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_followQueue_title));
        assertTrue(solo.waitForCondition(() -> continuousPlayback == UserPreferences.isFollowQueue(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testAutoDelete() {
        solo.clickOnText(solo.getString(R.string.storage_pref));
        final boolean autoDelete = UserPreferences.isAutoDelete();
        solo.clickOnText(solo.getString(R.string.pref_auto_delete_title));
        assertTrue(solo.waitForCondition(() -> autoDelete != UserPreferences.isAutoDelete(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_auto_delete_title));
        assertTrue(solo.waitForCondition(() -> autoDelete == UserPreferences.isAutoDelete(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testPlaybackSpeeds() {
        clickPreference(withText(R.string.playback_pref));
        clickPreference(withText(R.string.pref_playback_speed_title));
        assertTrue(solo.searchText(res.getStringArray(R.array.playback_speed_values)[0]));
        onView(withText(R.string.cancel_label)).perform(click());
    }

    @Test
    public void testPauseForInterruptions() {
        solo.clickOnText(solo.getString(R.string.playback_pref));
        final boolean pauseForFocusLoss = UserPreferences.shouldPauseForFocusLoss();
        solo.clickOnText(solo.getString(R.string.pref_pausePlaybackForFocusLoss_title));
        assertTrue(solo.waitForCondition(() -> pauseForFocusLoss != UserPreferences.shouldPauseForFocusLoss(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_pausePlaybackForFocusLoss_title));
        assertTrue(solo.waitForCondition(() -> pauseForFocusLoss == UserPreferences.shouldPauseForFocusLoss(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testDisableUpdateInterval() {
        solo.clickOnText(solo.getString(R.string.network_pref));
        solo.clickOnText(solo.getString(R.string.pref_autoUpdateIntervallOrTime_sum));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.pref_autoUpdateIntervallOrTime_Disable));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getUpdateInterval() == 0, 1000));
    }

    @Test
    public void testSetUpdateInterval() {
        clickPreference(withText(R.string.network_pref));
        clickPreference(withText(R.string.pref_autoUpdateIntervallOrTime_title));
        onView(withText(R.string.pref_autoUpdateIntervallOrTime_Interval)).perform(click());
        String search = "12 " + solo.getString(R.string.pref_update_interval_hours_plural);
        onView(withText(search)).perform(click());
        assertTrue(solo.waitForCondition(() -> UserPreferences.getUpdateInterval() ==
                TimeUnit.HOURS.toMillis(12), Timeout.getLargeTimeout()));
    }

    @Test
    public void testMobileUpdates() {
        clickPreference(withText(R.string.network_pref));
        final boolean mobileUpdates = UserPreferences.isAllowMobileUpdate();
        clickPreference(withText(R.string.pref_mobileUpdate_title));
        assertTrue(solo.waitForCondition(() -> mobileUpdates != UserPreferences.isAllowMobileUpdate(), Timeout.getLargeTimeout()));
        clickPreference(withText(R.string.pref_mobileUpdate_title));
        assertTrue(solo.waitForCondition(() -> mobileUpdates == UserPreferences.isAllowMobileUpdate(), Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetSequentialDownload() {
        clickPreference(withText(R.string.network_pref));
        clickPreference(withText(R.string.pref_parallel_downloads_title));
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "1");
        solo.clickOnText(solo.getString(android.R.string.ok));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getParallelDownloads() == 1, Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetParallelDownloads() {
        clickPreference(withText(R.string.network_pref));
        clickPreference(withText(R.string.pref_parallel_downloads_title));
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "10");
        solo.clickOnText(solo.getString(android.R.string.ok));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getParallelDownloads() == 10, Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetParallelDownloadsInvalidInput() {
        clickPreference(withText(R.string.network_pref));
        clickPreference(withText(R.string.pref_parallel_downloads_title));
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
        clickPreference(withText(R.string.network_pref));
        clickPreference(withText(R.string.pref_automatic_download_title));
        clickPreference(withText(R.string.pref_episode_cache_title));
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

        clickPreference(withText(R.string.network_pref));
        clickPreference(withText(R.string.pref_automatic_download_title));
        clickPreference(withText(R.string.pref_episode_cache_title));
        solo.waitForDialogToOpen(1000);
        solo.scrollUp();
        solo.clickOnText(minEntry);
        assertTrue(solo.waitForCondition(() -> UserPreferences.getEpisodeCacheSize() == minValue, Timeout.getLargeTimeout()));
    }

    @Test
    public void testSetEpisodeCacheMax() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String maxEntry = entries[entries.length-1];
        final int maxValue = Integer.valueOf(values[values.length-1]);
        solo.clickOnText(solo.getString(R.string.network_pref));
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        solo.waitForText(solo.getString(R.string.pref_automatic_download_title));
        solo.clickOnText(solo.getString(R.string.pref_episode_cache_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(maxEntry);
        assertTrue(solo.waitForCondition(() -> UserPreferences.getEpisodeCacheSize() == maxValue, Timeout.getLargeTimeout()));
    }

    @Test
    public void testAutomaticDownload() {
        final boolean automaticDownload = UserPreferences.isEnableAutodownload();
        clickPreference(withText(R.string.network_pref));
        clickPreference(withText(R.string.pref_automatic_download_title));
        clickPreference(withText(R.string.pref_automatic_download_title));

        assertTrue(solo.waitForCondition(() -> automaticDownload != UserPreferences.isEnableAutodownload(), Timeout.getLargeTimeout()));
        if(UserPreferences.isEnableAutodownload() == false) {
            clickPreference(withText(R.string.pref_automatic_download_title));
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

    @Test
    public void testEpisodeCleanupQueueOnly() {
        solo.clickOnText(solo.getString(R.string.network_pref));
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        solo.clickOnText(solo.getString(R.string.pref_episode_cleanup_title));
        solo.waitForText(solo.getString(R.string.episode_cleanup_queue_removal));
        solo.clickOnText(solo.getString(R.string.episode_cleanup_queue_removal));
        assertTrue(solo.waitForCondition(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    return alg instanceof APQueueCleanupAlgorithm;
                },
                Timeout.getLargeTimeout()));
    }

    @Test
    public void testEpisodeCleanupNeverAlg() {
        solo.clickOnText(solo.getString(R.string.network_pref));
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        solo.clickOnText(solo.getString(R.string.pref_episode_cleanup_title));
        solo.waitForText(solo.getString(R.string.episode_cleanup_never));
        solo.clickOnText(solo.getString(R.string.episode_cleanup_never));
        assertTrue(solo.waitForCondition(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    return alg instanceof APNullCleanupAlgorithm;
                },
                Timeout.getLargeTimeout()));
    }

    @Test
    public void testEpisodeCleanupClassic() {
        solo.clickOnText(solo.getString(R.string.network_pref));
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        solo.clickOnText(solo.getString(R.string.pref_episode_cleanup_title));
        solo.waitForText(solo.getString(R.string.episode_cleanup_after_listening));
        solo.clickOnText(solo.getString(R.string.episode_cleanup_after_listening));
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
        clickPreference(withText(R.string.network_pref));
        clickPreference(withText(R.string.pref_automatic_download_title));
        clickPreference(withText(R.string.pref_episode_cleanup_title));
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

        clickPreference(withText(R.string.playback_pref));
        clickPreference(withText(R.string.pref_rewind));
        solo.waitForDialogToOpen();

        int currentIndex = Arrays.binarySearch(deltas, seconds);
        assertTrue(currentIndex >= 0 && currentIndex < deltas.length);  // found?

        // Find next value (wrapping around to next)
        int newIndex = (currentIndex + 1) % deltas.length;
        onView(withText(String.valueOf(deltas[newIndex]) + " seconds")).perform(click());
        onView(withText("Confirm")).perform(click());

        solo.waitForDialogToClose();
        assertTrue(solo.waitForCondition(() -> UserPreferences.getRewindSecs() == deltas[newIndex],
                Timeout.getLargeTimeout()));
    }

    @Test
    public void testFastForwardChange() {
        clickPreference(withText(R.string.playback_pref));
        for (int i = 2; i > 0; i--) { // repeat twice to catch any error where fastforward is tracking rewind
            int seconds = UserPreferences.getFastForwardSecs();
            int deltas[] = res.getIntArray(R.array.seek_delta_values);

            clickPreference(withText(R.string.pref_fast_forward));
            solo.waitForDialogToOpen();

            int currentIndex = Arrays.binarySearch(deltas, seconds);
            assertTrue(currentIndex >= 0 && currentIndex < deltas.length);  // found?

            // Find next value (wrapping around to next)
            int newIndex = (currentIndex + 1) % deltas.length;

            onView(withText(String.valueOf(deltas[newIndex]) + " seconds")).perform(click());
            onView(withText("Confirm")).perform(click());

            solo.waitForDialogToClose();
            assertTrue(solo.waitForCondition(() -> UserPreferences.getFastForwardSecs() == deltas[newIndex],
                    Timeout.getLargeTimeout()));
        }
    }

    @Test
    public void testBackButtonBehaviorGoToPageSelector() {
        clickPreference(withText(R.string.user_interface_label));
        clickPreference(withText(R.string.pref_back_button_behavior_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.back_button_go_to_page));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.queue_label));
        solo.clickOnText(solo.getString(R.string.confirm_label));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonBehavior() == UserPreferences.BackButtonBehavior.GO_TO_PAGE,
                Timeout.getLargeTimeout()));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonGoToPage().equals(QueueFragment.TAG),
                Timeout.getLargeTimeout()));
        clickPreference(withText(R.string.pref_back_button_behavior_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.back_button_go_to_page));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.episodes_label));
        solo.clickOnText(solo.getString(R.string.confirm_label));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonBehavior() == UserPreferences.BackButtonBehavior.GO_TO_PAGE,
                Timeout.getLargeTimeout()));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonGoToPage().equals(EpisodesFragment.TAG),
                Timeout.getLargeTimeout()));
        clickPreference(withText(R.string.pref_back_button_behavior_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.back_button_go_to_page));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.subscriptions_label));
        solo.clickOnText(solo.getString(R.string.confirm_label));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonBehavior() == UserPreferences.BackButtonBehavior.GO_TO_PAGE,
                Timeout.getLargeTimeout()));
        assertTrue(solo.waitForCondition(() -> UserPreferences.getBackButtonGoToPage().equals(SubscriptionFragment.TAG),
                Timeout.getLargeTimeout()));
    }

    @Test
    public void testDeleteRemovesFromQueue() {
        clickPreference(withText(R.string.storage_pref));
        if (!UserPreferences.shouldDeleteRemoveFromQueue()) {
            clickPreference(withText(R.string.pref_delete_removes_from_queue_title));
            assertTrue(solo.waitForCondition(UserPreferences::shouldDeleteRemoveFromQueue, Timeout.getLargeTimeout()));
        }
        final boolean deleteRemovesFromQueue = UserPreferences.shouldDeleteRemoveFromQueue();
        solo.clickOnText(solo.getString(R.string.pref_delete_removes_from_queue_title));
        assertTrue(solo.waitForCondition(() -> deleteRemovesFromQueue != UserPreferences.shouldDeleteRemoveFromQueue(), Timeout.getLargeTimeout()));
        solo.clickOnText(solo.getString(R.string.pref_delete_removes_from_queue_title));
        assertTrue(solo.waitForCondition(() -> deleteRemovesFromQueue == UserPreferences.shouldDeleteRemoveFromQueue(), Timeout.getLargeTimeout()));
    }

    private void clickPreference(Matcher<View> matcher) {
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItem(hasDescendant(matcher), click()));
    }
}
