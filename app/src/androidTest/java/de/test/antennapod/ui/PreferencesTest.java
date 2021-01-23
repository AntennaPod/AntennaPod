package de.test.antennapod.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.preference.PreferenceManager;
import androidx.annotation.StringRes;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences.EnqueueLocation;
import de.danoeh.antennapod.core.storage.APCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.APNullCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.APQueueCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.EpisodeCleanupAlgorithm;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.test.antennapod.EspressoTestUtils;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.clickPreference;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

@LargeTest
public class PreferencesTest {
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

        res = mActivityRule.getActivity().getResources();
        UserPreferences.init(mActivityRule.getActivity());
    }

    @Test
    public void testSwitchTheme() {
        final int theme = UserPreferences.getTheme();
        int otherTheme;
        if (theme == de.danoeh.antennapod.core.R.style.Theme_AntennaPod_Light) {
            otherTheme = R.string.pref_theme_title_dark;
        } else {
            otherTheme = R.string.pref_theme_title_light;
        }
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_set_theme_title);
        onView(withText(otherTheme)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getTheme() != theme);
    }

    @Test
    public void testSwitchThemeBack() {
        final int theme = UserPreferences.getTheme();
        int otherTheme;
        if (theme == de.danoeh.antennapod.core.R.style.Theme_AntennaPod_Light) {
            otherTheme = R.string.pref_theme_title_dark;
        } else {
            otherTheme = R.string.pref_theme_title_light;
        }
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_set_theme_title);
        onView(withText(otherTheme)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getTheme() != theme);
    }

    @Test
    public void testEnablePersistentPlaybackControls() {
        final boolean persistNotify = UserPreferences.isPersistNotify();
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_persistNotify_title);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> persistNotify != UserPreferences.isPersistNotify());
        clickPreference(R.string.pref_persistNotify_title);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> persistNotify == UserPreferences.isPersistNotify());
    }

    @Test
    public void testSetLockscreenButtons() {
        clickPreference(R.string.user_interface_label);
        String[] buttons = res.getStringArray(R.array.compact_notification_buttons_options);
        clickPreference(R.string.pref_compact_notification_buttons_title);
        // First uncheck checkbox
        onView(withText(buttons[2])).perform(click());

        // Now try to check all checkboxes
        onView(withText(buttons[0])).perform(click());
        onView(withText(buttons[1])).perform(click());
        onView(withText(buttons[2])).perform(click());

        // Make sure that the third checkbox is unchecked
        onView(withText(buttons[2])).check(matches(not(isChecked())));

        String snackBarText = String.format(res.getString(
                R.string.pref_compact_notification_buttons_dialog_error), 2);
        Awaitility.await().ignoreExceptions().atMost(4000, MILLISECONDS)
                .until(() -> {
                    onView(withText(snackBarText)).check(doesNotExist());
                    return true;
                });

        onView(withText(R.string.confirm_label)).perform(click());

        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(UserPreferences::showRewindOnCompactNotification);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(UserPreferences::showFastForwardOnCompactNotification);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> !UserPreferences.showSkipOnCompactNotification());
    }

    @Test
    public void testEnqueueLocation() {
        clickPreference(R.string.playback_pref);
        doTestEnqueueLocation(R.string.enqueue_location_after_current, EnqueueLocation.AFTER_CURRENTLY_PLAYING);
        doTestEnqueueLocation(R.string.enqueue_location_front, EnqueueLocation.FRONT);
        doTestEnqueueLocation(R.string.enqueue_location_back, EnqueueLocation.BACK);
    }

    private void doTestEnqueueLocation(@StringRes int optionResId, EnqueueLocation expected) {
        clickPreference(R.string.pref_enqueue_location_title);
        onView(withText(optionResId)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> expected == UserPreferences.getEnqueueLocation());
    }

    @Test
    public void testHeadPhonesDisconnect() {
        clickPreference(R.string.playback_pref);
        final boolean pauseOnHeadsetDisconnect = UserPreferences.isPauseOnHeadsetDisconnect();
        onView(withText(R.string.pref_pauseOnHeadsetDisconnect_title)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> pauseOnHeadsetDisconnect != UserPreferences.isPauseOnHeadsetDisconnect());
        onView(withText(R.string.pref_pauseOnHeadsetDisconnect_title)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> pauseOnHeadsetDisconnect == UserPreferences.isPauseOnHeadsetDisconnect());
    }

    @Test
    public void testHeadPhonesReconnect() {
        clickPreference(R.string.playback_pref);
        if (!UserPreferences.isPauseOnHeadsetDisconnect()) {
            onView(withText(R.string.pref_pauseOnHeadsetDisconnect_title)).perform(click());
            Awaitility.await().atMost(1000, MILLISECONDS)
                    .until(UserPreferences::isPauseOnHeadsetDisconnect);
        }
        final boolean unpauseOnHeadsetReconnect = UserPreferences.isUnpauseOnHeadsetReconnect();
        onView(withText(R.string.pref_unpauseOnHeadsetReconnect_title)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> unpauseOnHeadsetReconnect != UserPreferences.isUnpauseOnHeadsetReconnect());
        onView(withText(R.string.pref_unpauseOnHeadsetReconnect_title)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> unpauseOnHeadsetReconnect == UserPreferences.isUnpauseOnHeadsetReconnect());
    }

    @Test
    public void testBluetoothReconnect() {
        clickPreference(R.string.playback_pref);
        if (!UserPreferences.isPauseOnHeadsetDisconnect()) {
            onView(withText(R.string.pref_pauseOnHeadsetDisconnect_title)).perform(click());
            Awaitility.await().atMost(1000, MILLISECONDS)
                    .until(UserPreferences::isPauseOnHeadsetDisconnect);
        }
        final boolean unpauseOnBluetoothReconnect = UserPreferences.isUnpauseOnBluetoothReconnect();
        onView(withText(R.string.pref_unpauseOnBluetoothReconnect_title)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> unpauseOnBluetoothReconnect != UserPreferences.isUnpauseOnBluetoothReconnect());
        onView(withText(R.string.pref_unpauseOnBluetoothReconnect_title)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> unpauseOnBluetoothReconnect == UserPreferences.isUnpauseOnBluetoothReconnect());
    }

    @Test
    public void testContinuousPlayback() {
        clickPreference(R.string.playback_pref);
        final boolean continuousPlayback = UserPreferences.isFollowQueue();
        clickPreference(R.string.pref_followQueue_title);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> continuousPlayback != UserPreferences.isFollowQueue());
        clickPreference(R.string.pref_followQueue_title);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> continuousPlayback == UserPreferences.isFollowQueue());
    }

    @Test
    public void testAutoDelete() {
        clickPreference(R.string.storage_pref);
        final boolean autoDelete = UserPreferences.isAutoDelete();
        onView(withText(R.string.pref_auto_delete_title)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> autoDelete != UserPreferences.isAutoDelete());
        onView(withText(R.string.pref_auto_delete_title)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> autoDelete == UserPreferences.isAutoDelete());
    }

    @Test
    public void testPlaybackSpeeds() {
        clickPreference(R.string.playback_pref);
        clickPreference(R.string.playback_speed);
        onView(isRoot()).perform(waitForView(withText("0.75"), 1000));
        onView(withText("0.75")).check(matches(isDisplayed()));
        onView(withText(R.string.close_label)).perform(click());
    }

    @Test
    public void testPauseForInterruptions() {
        clickPreference(R.string.playback_pref);
        final boolean pauseForFocusLoss = UserPreferences.shouldPauseForFocusLoss();
        clickPreference(R.string.pref_pausePlaybackForFocusLoss_title);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> pauseForFocusLoss != UserPreferences.shouldPauseForFocusLoss());
        clickPreference(R.string.pref_pausePlaybackForFocusLoss_title);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> pauseForFocusLoss == UserPreferences.shouldPauseForFocusLoss());
    }

    @Test
    public void testDisableUpdateInterval() {
        clickPreference(R.string.network_pref);
        onView(withText(R.string.pref_autoUpdateIntervallOrTime_title)).perform(click());
        onView(withText(R.string.pref_autoUpdateIntervallOrTime_Disable)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getUpdateInterval() == 0);
    }

    @Test
    public void testSetUpdateInterval() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_autoUpdateIntervallOrTime_title);
        onView(withText(R.string.pref_autoUpdateIntervallOrTime_Interval)).perform(click());
        String search = "12 " + res.getString(R.string.pref_update_interval_hours_plural);
        onView(withText(search)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getUpdateInterval() == TimeUnit.HOURS.toMillis(12));
    }

    @Test
    public void testSetSequentialDownload() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_parallel_downloads_title);
        onView(isRoot()).perform(waitForView(withClassName(endsWith("EditText")), 1000));
        onView(withClassName(endsWith("EditText"))).perform(replaceText("1"));
        onView(withText(android.R.string.ok)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getParallelDownloads() == 1);
    }

    @Test
    public void testSetParallelDownloads() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_parallel_downloads_title);
        onView(isRoot()).perform(waitForView(withClassName(endsWith("EditText")), 1000));
        onView(withClassName(endsWith("EditText"))).perform(replaceText("10"));
        onView(withText(android.R.string.ok)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getParallelDownloads() == 10);
    }

    @Test
    public void testSetParallelDownloadsInvalidInput() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_parallel_downloads_title);
        onView(isRoot()).perform(waitForView(withClassName(endsWith("EditText")), 1000));
        onView(withClassName(endsWith("EditText"))).perform(replaceText("0"));
        onView(withClassName(endsWith("EditText"))).check(matches(withText("")));
        onView(withClassName(endsWith("EditText"))).perform(replaceText("100"));
        onView(withClassName(endsWith("EditText"))).check(matches(withText("")));
    }

    @Test
    public void testSetEpisodeCache() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String entry = entries[entries.length / 2];
        final int value = Integer.parseInt(values[values.length / 2]);
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_automatic_download_title);
        clickPreference(R.string.pref_episode_cache_title);
        onView(isRoot()).perform(waitForView(withText(entry), 1000));
        onView(withText(entry)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getEpisodeCacheSize() == value);
    }

    @Test
    public void testSetEpisodeCacheMin() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String minEntry = entries[0];
        final int minValue = Integer.parseInt(values[0]);

        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_automatic_download_title);
        clickPreference(R.string.pref_episode_cache_title);
        onView(withId(R.id.select_dialog_listview)).perform(swipeDown());
        onView(withText(minEntry)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getEpisodeCacheSize() == minValue);
    }

    @Test
    public void testSetEpisodeCacheMax() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String maxEntry = entries[entries.length - 1];
        final int maxValue = Integer.parseInt(values[values.length - 1]);
        onView(withText(R.string.network_pref)).perform(click());
        onView(withText(R.string.pref_automatic_download_title)).perform(click());
        onView(withText(R.string.pref_episode_cache_title)).perform(click());
        onView(withId(R.id.select_dialog_listview)).perform(swipeUp());
        onView(withText(maxEntry)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getEpisodeCacheSize() == maxValue);
    }

    @Test
    public void testAutomaticDownload() {
        final boolean automaticDownload = UserPreferences.isEnableAutodownload();
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_automatic_download_title);
        clickPreference(R.string.pref_automatic_download_title);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> automaticDownload != UserPreferences.isEnableAutodownload());
        if (!UserPreferences.isEnableAutodownload()) {
            clickPreference(R.string.pref_automatic_download_title);
        }
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(UserPreferences::isEnableAutodownload);
        final boolean enableAutodownloadOnBattery = UserPreferences.isEnableAutodownloadOnBattery();
        clickPreference(R.string.pref_automatic_download_on_battery_title);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> enableAutodownloadOnBattery != UserPreferences.isEnableAutodownloadOnBattery());
        clickPreference(R.string.pref_automatic_download_on_battery_title);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> enableAutodownloadOnBattery == UserPreferences.isEnableAutodownloadOnBattery());
    }

    @Test
    public void testEpisodeCleanupQueueOnly() {
        clickPreference(R.string.network_pref);
        onView(withText(R.string.pref_automatic_download_title)).perform(click());
        onView(withText(R.string.pref_episode_cleanup_title)).perform(click());
        onView(isRoot()).perform(waitForView(withText(R.string.episode_cleanup_queue_removal), 1000));
        onView(withText(R.string.episode_cleanup_queue_removal)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getEpisodeCleanupAlgorithm() instanceof APQueueCleanupAlgorithm);
    }

    @Test
    public void testEpisodeCleanupNeverAlg() {
        clickPreference(R.string.network_pref);
        onView(withText(R.string.pref_automatic_download_title)).perform(click());
        onView(withText(R.string.pref_episode_cleanup_title)).perform(click());
        onView(withId(R.id.select_dialog_listview)).perform(swipeUp());
        onView(withText(R.string.episode_cleanup_never)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getEpisodeCleanupAlgorithm() instanceof APNullCleanupAlgorithm);
    }

    @Test
    public void testEpisodeCleanupClassic() {
        clickPreference(R.string.network_pref);
        onView(withText(R.string.pref_automatic_download_title)).perform(click());
        onView(withText(R.string.pref_episode_cleanup_title)).perform(click());
        onView(isRoot()).perform(waitForView(withText(R.string.episode_cleanup_after_listening), 1000));
        onView(withText(R.string.episode_cleanup_after_listening)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    if (alg instanceof APCleanupAlgorithm) {
                        APCleanupAlgorithm cleanupAlg = (APCleanupAlgorithm) alg;
                        return cleanupAlg.getNumberOfHoursAfterPlayback() == 0;
                    }
                    return false;
                });
    }

    @Test
    public void testEpisodeCleanupNumDays() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_automatic_download_title);
        clickPreference(R.string.pref_episode_cleanup_title);
        String search = res.getQuantityString(R.plurals.episode_cleanup_days_after_listening, 3, 3);
        onView(isRoot()).perform(waitForView(withText(search), 1000));
        onView(withText(search)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> {
                    EpisodeCleanupAlgorithm alg = UserPreferences.getEpisodeCleanupAlgorithm();
                    if (alg instanceof APCleanupAlgorithm) {
                        APCleanupAlgorithm cleanupAlg = (APCleanupAlgorithm) alg;
                        return cleanupAlg.getNumberOfHoursAfterPlayback() == 72; // 5 days
                    }
                    return false;
                });
    }

    @Test
    public void testRewindChange() {
        int seconds = UserPreferences.getRewindSecs();
        int[] deltas = res.getIntArray(R.array.seek_delta_values);

        clickPreference(R.string.playback_pref);
        clickPreference(R.string.pref_rewind);

        int currentIndex = Arrays.binarySearch(deltas, seconds);
        assertTrue(currentIndex >= 0 && currentIndex < deltas.length);  // found?

        // Find next value (wrapping around to next)
        int newIndex = (currentIndex + 1) % deltas.length;
        onView(withText(deltas[newIndex] + " seconds")).perform(click());
        onView(withText("Confirm")).perform(click());

        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getRewindSecs() == deltas[newIndex]);
    }

    @Test
    public void testFastForwardChange() {
        clickPreference(R.string.playback_pref);
        for (int i = 2; i > 0; i--) { // repeat twice to catch any error where fastforward is tracking rewind
            int seconds = UserPreferences.getFastForwardSecs();
            int[] deltas = res.getIntArray(R.array.seek_delta_values);

            clickPreference(R.string.pref_fast_forward);

            int currentIndex = Arrays.binarySearch(deltas, seconds);
            assertTrue(currentIndex >= 0 && currentIndex < deltas.length);  // found?

            // Find next value (wrapping around to next)
            int newIndex = (currentIndex + 1) % deltas.length;

            onView(withText(deltas[newIndex] + " seconds")).perform(click());
            onView(withText("Confirm")).perform(click());

            Awaitility.await().atMost(1000, MILLISECONDS)
                    .until(() -> UserPreferences.getFastForwardSecs() == deltas[newIndex]);
        }
    }

    @Test
    public void testBackButtonBehaviorGoToPageSelector() {
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_back_button_behavior_title);
        onView(withText(R.string.back_button_go_to_page)).perform(click());
        onView(withText(R.string.queue_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getBackButtonBehavior() == UserPreferences.BackButtonBehavior.GO_TO_PAGE);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getBackButtonGoToPage().equals(QueueFragment.TAG));
        clickPreference(R.string.pref_back_button_behavior_title);
        onView(withText(R.string.back_button_go_to_page)).perform(click());
        onView(withText(R.string.episodes_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getBackButtonBehavior() == UserPreferences.BackButtonBehavior.GO_TO_PAGE);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getBackButtonGoToPage().equals(EpisodesFragment.TAG));
        clickPreference(R.string.pref_back_button_behavior_title);
        onView(withText(R.string.back_button_go_to_page)).perform(click());
        onView(withText(R.string.subscriptions_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getBackButtonBehavior() == UserPreferences.BackButtonBehavior.GO_TO_PAGE);
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getBackButtonGoToPage().equals(SubscriptionFragment.TAG));
    }

    @Test
    public void testDeleteRemovesFromQueue() {
        clickPreference(R.string.storage_pref);
        if (!UserPreferences.shouldDeleteRemoveFromQueue()) {
            clickPreference(R.string.pref_delete_removes_from_queue_title);
            Awaitility.await().atMost(1000, MILLISECONDS)
                    .until(UserPreferences::shouldDeleteRemoveFromQueue);
        }
        final boolean deleteRemovesFromQueue = UserPreferences.shouldDeleteRemoveFromQueue();
        onView(withText(R.string.pref_delete_removes_from_queue_title)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> deleteRemovesFromQueue != UserPreferences.shouldDeleteRemoveFromQueue());
        onView(withText(R.string.pref_delete_removes_from_queue_title)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> deleteRemovesFromQueue == UserPreferences.shouldDeleteRemoveFromQueue());
    }
}
