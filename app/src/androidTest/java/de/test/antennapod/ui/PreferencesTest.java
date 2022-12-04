package de.test.antennapod.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.preference.PreferenceManager;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import de.danoeh.antennapod.core.storage.EpisodeCleanupAlgorithmFactory;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.APCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.EpisodeCleanupAlgorithm;
import de.danoeh.antennapod.core.storage.ExceptFavoriteCleanupAlgorithm;
import de.test.antennapod.EspressoTestUtils;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeDown;
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
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

@LargeTest
public class PreferencesTest {
    private Resources res;

    @Rule
    public ActivityTestRule<PreferenceActivity> activityTestRule =
            new ActivityTestRule<>(PreferenceActivity.class,
                    false,
                    false);


    @Before
    public void setUp() {
        EspressoTestUtils.clearDatabase();
        EspressoTestUtils.clearPreferences();
        activityTestRule.launchActivity(new Intent());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activityTestRule.getActivity());
        prefs.edit().putBoolean(UserPreferences.PREF_ENABLE_AUTODL, true).commit();

        res = activityTestRule.getActivity().getResources();
        UserPreferences.init(activityTestRule.getActivity());
    }

    @Test
    public void testSwitchTheme() {
        final UserPreferences.ThemePreference theme = UserPreferences.getTheme();
        int otherThemeText;
        if (theme == UserPreferences.ThemePreference.DARK) {
            otherThemeText = R.string.pref_theme_title_light;
        } else {
            otherThemeText = R.string.pref_theme_title_dark;
        }
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_set_theme_title);
        onView(withText(otherThemeText)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getTheme() != theme);
    }

    @Test
    public void testSetLockscreenButtons() {
        clickPreference(R.string.user_interface_label);
        String[] buttons = res.getStringArray(R.array.compact_notification_buttons_options);
        clickPreference(R.string.pref_compact_notification_buttons_title);
        // First uncheck checkboxes
        onView(withText(buttons[0])).perform(click());
        onView(withText(buttons[1])).perform(click());

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
    public void testPlaybackSpeeds() {
        clickPreference(R.string.playback_pref);
        clickPreference(R.string.playback_speed);
        onView(isRoot()).perform(waitForView(withText("1.25"), 1000));
        onView(withText("1.25")).check(matches(isDisplayed()));
    }

    @Test
    public void testDisableUpdateInterval() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.feed_refresh_title);
        onView(withText(R.string.feed_refresh_never)).perform(click());
        onView(withId(R.id.disableRadioButton)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getUpdateInterval() == 0);
    }

    @Test
    public void testSetUpdateInterval() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.feed_refresh_title);
        onView(withId(R.id.intervalRadioButton)).perform(click());
        onView(withId(R.id.spinner)).perform(click());
        int position = 1; // an arbitrary position
        onData(anything()).inRoot(RootMatchers.isPlatformPopup()).atPosition(position).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> UserPreferences.getUpdateInterval() == TimeUnit.HOURS.toMillis(2));
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
        onView(withClassName(endsWith("EditText"))).perform(closeSoftKeyboard());
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
    public void testEpisodeCleanupFavoriteOnly() {
        clickPreference(R.string.network_pref);
        onView(withText(R.string.pref_automatic_download_title)).perform(click());
        onView(withText(R.string.pref_episode_cleanup_title)).perform(click());
        onView(withId(R.id.select_dialog_listview)).perform(swipeDown());
        onView(withText(R.string.episode_cleanup_except_favorite_removal)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> EpisodeCleanupAlgorithmFactory.build() instanceof ExceptFavoriteCleanupAlgorithm);
    }

    @Test
    public void testEpisodeCleanupNumDays() {
        clickPreference(R.string.network_pref);
        clickPreference(R.string.pref_automatic_download_title);
        clickPreference(R.string.pref_episode_cleanup_title);
        String search = res.getQuantityString(R.plurals.episode_cleanup_days_after_listening, 3, 3);
        onView(withText(search)).perform(scrollTo());
        onView(withText(search)).perform(click());
        Awaitility.await().atMost(1000, MILLISECONDS)
                .until(() -> {
                    EpisodeCleanupAlgorithm alg = EpisodeCleanupAlgorithmFactory.build();
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
}
