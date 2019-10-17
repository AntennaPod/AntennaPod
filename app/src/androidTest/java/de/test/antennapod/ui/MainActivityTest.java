package de.test.antennapod.ui;

import android.app.Activity;
import android.content.Intent;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.runner.AndroidJUnit4;
import com.robotium.solo.Solo;
import com.robotium.solo.Timeout;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.test.antennapod.EspressoTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.clickPreference;
import static de.test.antennapod.EspressoTestUtils.openNavDrawer;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * User interface tests for MainActivity
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private Solo solo;
    private UITestUtils uiTestUtils;

    @Rule
    public IntentsTestRule<MainActivity> mActivityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws IOException {
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.makeNotFirstRun();
        EspressoTestUtils.clearDatabase();

        mActivityRule.launchActivity(new Intent());

        uiTestUtils = new UITestUtils(InstrumentationRegistry.getTargetContext());
        uiTestUtils.setup();

        solo = new Solo(getInstrumentation(), mActivityRule.getActivity());
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
        PodDBAdapter.deleteDatabase();
    }

    @Test
    public void testAddFeed() throws Exception {
        uiTestUtils.addHostedFeedData();
        final Feed feed = uiTestUtils.hostedFeeds.get(0);
        openNavDrawer();
        onView(withText(R.string.add_feed_label)).perform(click());
        solo.enterText(1, feed.getDownload_url());
        onView(withText(R.string.confirm_label)).perform(click());
        onView(withText(R.string.subscribe_label)).perform(click());
        assertTrue(solo.waitForText(solo.getString(R.string.open_podcast), 0, Timeout.getLargeTimeout(), false));
    }

    private String getActionbarTitle() {
        return ((MainActivity) solo.getCurrentActivity()).getSupportActionBar().getTitle().toString();
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
        assertEquals(solo.getString(R.string.subscriptions_label), getActionbarTitle());
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
        assertTrue(((MainActivity)solo.getCurrentActivity()).isDrawerOpen());
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
        assertThat(mActivityRule.getActivityResult(), hasResultCode(Activity.RESULT_CANCELED));
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
        onView(withText(R.string.yes)).perform(click());
        Thread.sleep(100);
        assertThat(mActivityRule.getActivityResult(), hasResultCode(Activity.RESULT_CANCELED));
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
        assertThat(mActivityRule.getActivityResult(), hasResultCode(Activity.RESULT_CANCELED));
    }
}
