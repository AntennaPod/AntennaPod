package de.test.antennapod.ui;

import android.content.Intent;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.test.antennapod.EspressoTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.onDrawerItem;
import static de.test.antennapod.EspressoTestUtils.openNavDrawer;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;

/**
 * Test UI for feeds that do not have media files
 */
@RunWith(AndroidJUnit4.class)
public class TextOnlyFeedsTest {

    private UITestUtils uiTestUtils;

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws IOException {
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.makeNotFirstRun();
        EspressoTestUtils.clearDatabase();

        uiTestUtils = new UITestUtils(InstrumentationRegistry.getInstrumentation().getTargetContext());
        uiTestUtils.setHostTextOnlyFeeds(true);
        uiTestUtils.setup();

        activityRule.launchActivity(new Intent());
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
    }

    @Test
    public void testMarkAsPlayedList() throws Exception {
        uiTestUtils.addLocalFeedData(false);
        final Feed feed = uiTestUtils.hostedFeeds.get(0);
        openNavDrawer();
        onDrawerItem(withText(feed.getTitle())).perform(scrollTo(), click());
        onView(withText(feed.getItemAtIndex(0).getTitle())).perform(click());
        onView(isRoot()).perform(waitForView(withText(R.string.mark_read_no_media_label), 3000));
        onView(withText(R.string.mark_read_no_media_label)).perform(click());
        onView(isRoot()).perform(waitForView(allOf(withText(R.string.mark_read_no_media_label), not(isDisplayed())), 3000));
    }

}
