package de.test.antennapod.ui;

import android.content.Intent;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.storage.database.DBReader;
import de.test.antennapod.EspressoTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.clickPreference;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class FeedSettingsTest {
    private UITestUtils uiTestUtils;
    private Feed feed;

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws Exception {
        uiTestUtils = new UITestUtils(InstrumentationRegistry.getInstrumentation().getTargetContext());
        uiTestUtils.setup();

        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();

        uiTestUtils.addLocalFeedData(true);
        feed = uiTestUtils.hostedFeeds.get(0);

        // Use same approach as working tests
        EspressoTestUtils.setLaunchScreen("" + feed.getId());
        activityRule.launchActivity(new Intent());
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
    }

    @Test
    public void testClickFeedSettings() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        }
        onView(withId(R.id.butShowSettings)).perform(click());

        clickPreference(R.string.keep_updated);

        clickPreference(R.string.authentication_label);
        onView(withText(R.string.cancel_label)).perform(click());

        clickPreference(R.string.playback_speed);
        onView(withText(R.string.cancel_label)).perform(click());

        clickPreference(R.string.pref_feed_skip);
        onView(withText(R.string.cancel_label)).perform(click());

        clickPreference(R.string.pref_auto_delete_playback_title);
        onView(withText(R.string.cancel_label)).perform(click());

        clickPreference(R.string.feed_volume_adapdation);
        onView(withText(R.string.cancel_label)).perform(click());
    }

    @Test
    public void testEnqueueLocationPersistence() {
        // Wait for feed to load (same as working test)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Open feed settings
        onView(withId(R.id.butShowSettings)).perform(click());

        // Wait for settings to load
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Get initial preference value from FeedPreferences (not SharedPreferences)
        FeedPreferences initialPrefs = feed.getPreferences();
        FeedPreferences.EnqueueLocation initialLocation = initialPrefs.getEnqueueLocation();

        // Open enqueue location preference
        clickPreference(R.string.pref_enqueue_location_title);

        // Select "Front" option (different from initial)
        onView(withText(R.string.enqueue_location_front)).perform(click());

        // Wait for preference change to be processed
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Get feed from database to verify persistence
        Feed feedFromDb = DBReader.getFeed(feed.getId(), false, 0, Integer.MAX_VALUE);
        FeedPreferences.EnqueueLocation persistedLocation = feedFromDb.getPreferences().getEnqueueLocation();

        // Verify preference value persisted after navigation
        assertEquals("DB should persist enqueue location for feed", FeedPreferences.EnqueueLocation.FRONT,
                persistedLocation);

        // Test complete - preference persisted correctly
    }
}
