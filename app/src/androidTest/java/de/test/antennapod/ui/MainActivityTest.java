package de.test.antennapod.ui;

import android.content.Intent;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.test.antennapod.EspressoTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.openNavDrawer;
import static de.test.antennapod.EspressoTestUtils.waitForViewGlobally;

/**
 * User interface tests for MainActivity.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private UITestUtils uiTestUtils;

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws IOException {
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();

        activityRule.launchActivity(new Intent());

        uiTestUtils = new UITestUtils(InstrumentationRegistry.getInstrumentation().getTargetContext());
        uiTestUtils.setup();
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
        PodDBAdapter.deleteDatabase();
    }

    @Test
    public void testAddFeed() throws Exception {
        // connect to podcast feed
        uiTestUtils.addHostedFeedData();
        final Feed feed = uiTestUtils.hostedFeeds.get(0);
        openNavDrawer();
        onView(withText(R.string.add_feed_label)).perform(click());
        onView(withId(R.id.addViaUrlButton)).perform(scrollTo(), click());
        onView(withId(R.id.urlEditText)).perform(replaceText(feed.getDownload_url()));
        onView(withText(R.string.confirm_label)).perform(scrollTo(), click());

        // subscribe podcast
        Espresso.closeSoftKeyboard();
        waitForViewGlobally(withText(R.string.subscribe_label), 15000);
        onView(withText(R.string.subscribe_label)).perform(click());

        // wait for podcast feed item list
        waitForViewGlobally(withId(R.id.butShowSettings), 15000);
    }
}
