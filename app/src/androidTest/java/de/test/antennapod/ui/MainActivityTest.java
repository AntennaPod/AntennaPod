package de.test.antennapod.ui;

import android.app.Activity;
import android.content.Intent;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.robotium.solo.Solo;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.test.antennapod.EspressoTestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.clickPreference;
import static de.test.antennapod.EspressoTestUtils.openNavDrawer;
import static de.test.antennapod.EspressoTestUtils.waitForViewGlobally;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * User interface tests for MainActivity.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private Solo solo;
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

        solo = new Solo(InstrumentationRegistry.getInstrumentation(), activityRule.getActivity());
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

    @Test
    public void testBackButtonBehaviorGoToPage() {
        openNavDrawer();
        onView(withText(R.string.settings_label)).perform(click());
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_back_button_behavior_title);

        onView(withText(R.string.back_button_go_to_page)).perform(click());
        onView(withText(R.string.subscriptions_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());

        solo.goBackToActivity(MainActivity.class.getSimpleName());
        solo.goBack();
        solo.goBack();
        onView(allOf(withId(R.id.toolbar), isDisplayed())).check(
                matches(hasDescendant(withText(R.string.subscriptions_label))));
        solo.goBack();
        assertThat(activityRule.getActivityResult(), hasResultCode(Activity.RESULT_CANCELED));
    }

    @Test
    public void testBackButtonBehaviorOpenDrawer() {
        openNavDrawer();
        onView(withText(R.string.settings_label)).perform(click());
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_back_button_behavior_title);
        onView(withText(R.string.back_button_open_drawer)).perform(click());
        solo.goBackToActivity(MainActivity.class.getSimpleName());
        solo.goBack();
        solo.goBack();
        assertTrue(((MainActivity) solo.getCurrentActivity()).isDrawerOpen());
    }

    @Test
    public void testBackButtonBehaviorDoubleTap() {
        openNavDrawer();
        onView(withText(R.string.settings_label)).perform(click());
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_back_button_behavior_title);
        onView(withText(R.string.back_button_double_tap)).perform(click());
        solo.goBackToActivity(MainActivity.class.getSimpleName());
        solo.goBack();
        solo.goBack();
        solo.goBack();
        assertThat(activityRule.getActivityResult(), hasResultCode(Activity.RESULT_CANCELED));
    }

    @Test
    public void testBackButtonBehaviorPrompt() throws Exception {
        openNavDrawer();
        onView(withText(R.string.settings_label)).perform(click());
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_back_button_behavior_title);
        onView(withText(R.string.back_button_show_prompt)).perform(click());
        solo.goBackToActivity(MainActivity.class.getSimpleName());
        solo.goBack();
        solo.goBack();
        onView(withText(R.string.yes)).perform(click());
        Thread.sleep(100);
        assertThat(activityRule.getActivityResult(), hasResultCode(Activity.RESULT_CANCELED));
    }

    @Test
    public void testBackButtonBehaviorDefault() {
        openNavDrawer();
        onView(withText(R.string.settings_label)).perform(click());
        clickPreference(R.string.user_interface_label);
        clickPreference(R.string.pref_back_button_behavior_title);
        onView(withText(R.string.back_button_default)).perform(click());
        solo.goBackToActivity(MainActivity.class.getSimpleName());
        solo.goBack();
        solo.goBack();
        assertThat(activityRule.getActivityResult(), hasResultCode(Activity.RESULT_CANCELED));
    }
}
