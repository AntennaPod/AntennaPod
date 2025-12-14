package de.test.antennapod.ui;

import android.content.Intent;
import android.os.Build;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.test.antennapod.EspressoTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.clickBottomNavItem;
import static de.test.antennapod.EspressoTestUtils.clickBottomNavOverflow;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assume.assumeTrue;

/**
 * User interface tests for MainActivity bottom navigation.
 */
@RunWith(AndroidJUnit4.class)
public class BottomNavigationTest {

    private UITestUtils uiTestUtils;

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws IOException {
        uiTestUtils = new UITestUtils(InstrumentationRegistry.getInstrumentation().getTargetContext());
        uiTestUtils.setup();

        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();
        UserPreferences.setBottomNavigationEnabled(true);
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
    }

    @Test
    public void testClickBottomNavigation() throws Exception {
        assumeTrue(Build.VERSION.SDK_INT >= 30); // Unclear why this crashes on old Android versions
        uiTestUtils.addLocalFeedData(false);
        UserPreferences.setDrawerItemOrder(Collections.emptyList(), Collections.emptyList());
        activityRule.launchActivity(new Intent());

        clickBottomNavItem(R.string.home_label_short);
        onView(isRoot()).perform(waitForView(allOf(isDescendantOfA(withId(R.id.toolbar)),
                withText(R.string.home_label)), 1000));

        clickBottomNavItem(R.string.queue_label_short);
        onView(isRoot()).perform(waitForView(allOf(isDescendantOfA(withId(R.id.toolbar)),
                withText(R.string.queue_label)), 1000));

        clickBottomNavItem(R.string.inbox_label_short);
        onView(isRoot()).perform(waitForView(allOf(isDescendantOfA(withId(R.id.toolbar)),
                withText(R.string.inbox_label)), 1000));

        clickBottomNavItem(R.string.subscriptions_label_short);
        onView(isRoot()).perform(waitForView(allOf(isDescendantOfA(withId(R.id.toolbar)),
                withText(R.string.subscriptions_label)), 1000));

        clickBottomNavOverflow(R.string.episodes_label);
        onView(isRoot()).perform(waitForView(allOf(isDescendantOfA(withId(R.id.toolbar)),
                withText(R.string.episodes_label)), 1000));

        clickBottomNavOverflow(R.string.downloads_label);
        onView(isRoot()).perform(waitForView(allOf(isDescendantOfA(withId(R.id.toolbar)),
                withText(R.string.downloads_label)), 1000));

        clickBottomNavOverflow(R.string.playback_history_label);
        onView(isRoot()).perform(waitForView(allOf(isDescendantOfA(withId(R.id.toolbar)),
                withText(R.string.playback_history_label)), 1000));

        clickBottomNavOverflow(R.string.add_feed_label);
        onView(isRoot()).perform(waitForView(allOf(isDescendantOfA(withId(R.id.toolbar)),
                withText(R.string.add_feed_label)), 1000));
    }
}
