package de.test.antennapod.ui;

import android.content.Intent;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.PreferenceMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.fragment.BatchFeedSettingsFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.test.antennapod.EspressoTestUtils;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.clickChildViewWithId;
import static de.test.antennapod.EspressoTestUtils.clickPreference;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static org.hamcrest.Matchers.allOf;

import static java.util.concurrent.TimeUnit.MILLISECONDS;


@RunWith(AndroidJUnit4.class)
public class BatchFeedSettingsTest {
    private UITestUtils uiTestUtils;
    private List<Feed> feeds;

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws Exception {
        uiTestUtils = new UITestUtils(InstrumentationRegistry.getInstrumentation().getTargetContext());
        uiTestUtils.setup();

        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();

        uiTestUtils.addLocalFeedData(false);
        feeds = new ArrayList<>();
        feeds.add(uiTestUtils.hostedFeeds.get(0));
        feeds.add(uiTestUtils.hostedFeeds.get(1));
        feeds.add(uiTestUtils.hostedFeeds.get(2));
        feeds.add(uiTestUtils.hostedFeeds.get(3));

        Intent intent = new Intent();
//        intent.putExtra(MainActivity.EXTRA_FEED_ID, feeds.getId());
        EspressoTestUtils.setLastNavFragment(SubscriptionFragment.TAG);

        activityRule.launchActivity(intent);
        activityRule.getActivity().loadChildFragment(BatchFeedSettingsFragment.newInstance(feeds));
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
    }

    @Test
    public void testClickFeedSettings() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        FeedPreferences defaultFeedPreferences = new FeedPreferences(0, false, FeedPreferences.AutoDeleteAction.GLOBAL,
                VolumeAdaptionSetting.OFF, null, null);
        FeedPreferences expectedFeedPreferences = new FeedPreferences(0, true, FeedPreferences.AutoDeleteAction.GLOBAL,
                VolumeAdaptionSetting.OFF, null, null);
        expectedFeedPreferences.setKeepUpdated(true);
        Feed feed1 = feeds.get(0);

//        Assert.assertEquals(feed1.getPreferences(), );

//        clickPreference(R.string.keep_updated);
//
//        clickPreference(R.string.keep_updated);
        onData(PreferenceMatchers.withKey("keepUpdated")).perform();
        Awaitility.await().atMost(4000, MILLISECONDS)
                .until(() -> {
                    for (Feed feed : feeds) {
                        Assert.assertEquals(expectedFeedPreferences.getKeepUpdated(), true);
                    }
                    return true;
                });
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        clickPreference(R.string.authentication_label);
//        onView(withText(R.string.cancel_label)).perform(click());
//
//        clickPreference(R.string.playback_speed);
//        onView(withText(R.string.cancel_label)).perform(click());
//
//        clickPreference(R.string.pref_feed_skip);
//        onView(withText(R.string.cancel_label)).perform(click());
//
//        clickPreference(R.string.auto_delete_label);
//        onView(withText(R.string.cancel_label)).perform(click());
//
//        clickPreference(R.string.feed_volume_reduction);
//        onView(withText(R.string.cancel_label)).perform(click());
    }
}
