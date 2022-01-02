package de.test.antennapod.dialogs;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.test.antennapod.EspressoTestUtils;
import de.test.antennapod.ui.UITestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.onDrawerItem;
import static de.test.antennapod.EspressoTestUtils.openNavDrawer;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static de.test.antennapod.NthMatcher.first;
import static org.hamcrest.CoreMatchers.allOf;

/**
 * User interface tests for share dialog.
 */
@RunWith(AndroidJUnit4.class)
public class ShareDialogTest {

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    private UITestUtils uiTestUtils;
    protected Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();
        EspressoTestUtils.setLastNavFragment(EpisodesFragment.TAG);
        uiTestUtils = new UITestUtils(context);
        uiTestUtils.setup();
        uiTestUtils.addLocalFeedData(true);

        activityRule.launchActivity(new Intent());

        openNavDrawer();
        onDrawerItem(withText(R.string.episodes_label)).perform(click());
        onView(isRoot()).perform(waitForView(withText(R.string.all_episodes_short_label), 1000));
        onView(withText(R.string.all_episodes_short_label)).perform(click());

        Matcher<View> allEpisodesMatcher;
        allEpisodesMatcher = Matchers.allOf(withId(android.R.id.list), isDisplayed(), hasMinimumChildCount(2));
        onView(isRoot()).perform(waitForView(allEpisodesMatcher, 1000));
        onView(allEpisodesMatcher).perform(actionOnItemAtPosition(0, click()));
        onView(first(EspressoTestUtils.actionBarOverflow())).perform(click());
    }

    @Test
    public void testShareDialogDisplayed() throws InterruptedException {
        onView(withText(R.string.share_label_with_ellipses)).perform(click());
        onView(allOf(isDisplayed(), withText(R.string.share_label)));
    }

    @Test
    public void testShareDialogCancelButton() {
        onView(withText(R.string.share_label_with_ellipses)).perform(scrollTo()).perform(click());
        onView(withText(R.string.cancel_label)).check(matches(isDisplayed())).perform(scrollTo()).perform(click());
    }

}
