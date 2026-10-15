package de.test.antennapod.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;

import android.content.Intent;
import android.view.View;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.SubscriptionsFilter;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.screen.subscriptions.SubscriptionFragment;
import de.test.antennapod.EspressoTestUtils;

/**
 * User interface tests for tags bar of subscriptions fragment.
 */
@RunWith(AndroidJUnit4.class)
public class SubscriptionFragmentTagsTest {

    private UITestUtils uiTestUtils;

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws Exception {
        uiTestUtils = new UITestUtils(InstrumentationRegistry.getInstrumentation().getTargetContext());
        uiTestUtils.setup();

        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();

        uiTestUtils.addLocalFeedData(false);
        FeedPreferences prefs1 = new FeedPreferences(uiTestUtils.hostedFeeds.get(1).getId(), FeedPreferences.AutoDownloadSetting.GLOBAL, FeedPreferences.AutoDeleteAction.GLOBAL, VolumeAdaptionSetting.OFF, FeedPreferences.NewEpisodesAction.GLOBAL, null, null);
        prefs1.getTags().add("abc");
        DBWriter.setFeedPreferences(prefs1);
        FeedPreferences prefs2 = new FeedPreferences(uiTestUtils.hostedFeeds.get(2).getId(), FeedPreferences.AutoDownloadSetting.GLOBAL, FeedPreferences.AutoDeleteAction.GLOBAL, VolumeAdaptionSetting.OFF, FeedPreferences.NewEpisodesAction.GLOBAL, null, null);
        prefs2.getTags().add("def");
        prefs2.setKeepUpdated(false);
        DBWriter.setFeedPreferences(prefs2);

        UserPreferences.setSubscriptionsFilter(new SubscriptionsFilter(SubscriptionsFilter.ENABLED_UPDATES));

        EspressoTestUtils.setLaunchScreen(SubscriptionFragment.TAG);
        activityRule.launchActivity(new Intent());
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
    }

    @Test
    public void testTagsShown() {
        onView(withId(R.id.tags_recycler)).check(matches(isDisplayed()));
    }

    @Test
    public void testTagAll() {
        clickTag(withText(R.string.tag_all));

        assertFeedsDisplayed(0, 1, 3, 4);
        assertFeedsDoNotExist(2);
    }

    @Test
    public void testTagUntagged() {
        clickTag(withText(R.string.tag_untagged));

        assertFeedsDisplayed(0, 3, 4);
        assertFeedsDoNotExist(1, 2);
    }

    @Test
    public void testTagAbc() {
        clickTag(withText("abc"));

        assertFeedsDisplayed(1);
        assertFeedsDoNotExist(0, 2, 3, 4);
    }

    @Test
    public void testTagDef() {
        clickTag(withText("def"));

        assertFeedsDoNotExist(0, 1, 2, 3, 4);
        onView(allOf(withId(R.id.emptyViewTitle), withText(R.string.no_subscriptions_head_label))).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.emptyViewMessage), withText(R.string.no_subscriptions_filtered_label))).check(matches(isDisplayed()));
    }

    private void clickTag(Matcher<View> m) {
        onView(allOf(withId(R.id.tag_chip), m)).perform(click());
    }

    private void assertFeedsDisplayed(int... ids) {
        for (int id : ids) {
            onView(allOf(withId(R.id.fallbackTitleLabel), withText(uiTestUtils.hostedFeeds.get(id).getTitle()))).check(matches(isDisplayed()));
        }
    }

    private void assertFeedsDoNotExist(int... ids) {
        for (int id : ids) {
            onView(allOf(withId(R.id.fallbackTitleLabel), withText(uiTestUtils.hostedFeeds.get(id).getTitle()))).check(doesNotExist());
        }
    }
}
