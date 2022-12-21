package de.test.antennapod.ui;

import android.content.Intent;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.test.antennapod.EspressoTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.clickPreference;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

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

        uiTestUtils.addLocalFeedData(false);
        feed = uiTestUtils.hostedFeeds.get(0);
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_FEED_ID, feed.getId());
        activityRule.launchActivity(intent);
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
    }

    @Test
    public void testClickFeedSettings() {
        onView(isRoot()).perform(waitForView(allOf(isDescendantOfA(withId(R.id.appBar)),
                withText(feed.getTitle()), isDisplayed()), 1000));
        onView(withId(R.id.butShowSettings)).perform(click());

        clickPreference(R.string.keep_updated);

        clickPreference(R.string.authentication_label);
        onView(withText(R.string.cancel_label)).perform(click());

        clickPreference(R.string.playback_speed);
        onView(withText(R.string.cancel_label)).perform(click());

        clickPreference(R.string.pref_feed_skip);
        onView(withText(R.string.cancel_label)).perform(click());

        clickPreference(R.string.auto_delete_label);
        onView(withText(R.string.cancel_label)).perform(click());

        clickPreference(R.string.feed_volume_reduction);
        onView(withText(R.string.cancel_label)).perform(click());
    }

    /**
     * Test that modifying a feed's authentication settings results in proper behavior.
     * Expect:
     *      - Feed is refreshed automatically
     *      - Database has updated username and password
     */
    @Test
    public void testAuthenticationSettingsUpdate() throws IOException {
        onView(isRoot()).perform(waitForView(allOf(isDescendantOfA(withId(R.id.appBar)),
                withText(feed.getTitle()), isDisplayed()), 1000));

        String updatedTitle = "modified episode title";
        String username = "username";
        String password = "password";

        // update feed hosted on server
        feed.getItems().get(0).setTitle(updatedTitle);
        uiTestUtils.hostFeed(feed);

        // interact with UI to update authentication settings
        updateAuthenticationSettings(username, password);

        // expect feed to have refreshed and be showing new episode title
        onView(isRoot()).perform(waitForView(withText(updatedTitle), 5000));

        // expect database to be updated with correct username and password
        Feed updatedFeed = DBReader.getFeed(feed.getId());
        assertNotNull(updatedFeed);

        FeedPreferences updatedFeedPreferences = updatedFeed.getPreferences();
        assertNotNull(updatedFeedPreferences);

        assertEquals("database updated with username", username, updatedFeedPreferences.getUsername());
        assertEquals("database updated with password", password, updatedFeedPreferences.getPassword());
    }

    private void updateAuthenticationSettings(String username, String password) {
        onView(withId(R.id.butShowSettings)).perform(click());

        clickPreference(R.string.authentication_label);
        onView(withId(R.id.usernameEditText)).perform(typeText(username));
        onView(withId(R.id.passwordEditText)).perform(typeText(password));
        onView(withText(R.string.confirm_label)).perform(click());

        onView(isRoot()).perform(pressBack());
    }

}
