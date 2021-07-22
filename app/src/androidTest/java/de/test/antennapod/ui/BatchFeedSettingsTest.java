package de.test.antennapod.ui;

import android.content.Intent;
import android.view.View;

import androidx.appcompat.widget.SwitchCompat;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.PreferenceMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
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
import static de.danoeh.antennapod.model.feed.FeedPreferences.SPEED_USE_GLOBAL;
import static de.test.antennapod.EspressoTestUtils.clickChildViewWithId;
import static de.test.antennapod.EspressoTestUtils.clickPreference;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static org.hamcrest.Matchers.allOf;

import static java.util.concurrent.TimeUnit.MILLISECONDS;


@RunWith(AndroidJUnit4.class)
public class BatchFeedSettingsTest {
    private UITestUtils uiTestUtils;
    private List<Feed> feeds;
    FeedPreferences defaultFeedPreferences = new FeedPreferences(0, false, FeedPreferences.AutoDeleteAction.GLOBAL,
            VolumeAdaptionSetting.OFF, null, null);
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
        feeds.get(0).setPreferences((defaultFeedPreferences));
        feeds.add(uiTestUtils.hostedFeeds.get(1));
        feeds.get(1).setPreferences((defaultFeedPreferences));
        feeds.add(uiTestUtils.hostedFeeds.get(2));
        feeds.get(2).setPreferences((defaultFeedPreferences));
        feeds.add(uiTestUtils.hostedFeeds.get(3));
        feeds.get(3).setPreferences((defaultFeedPreferences));

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
    public void testClick_keepUpdatedPreference() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        FeedPreferences expectedFeedPreferences = new FeedPreferences(0, true, FeedPreferences.AutoDeleteAction.GLOBAL,
                VolumeAdaptionSetting.OFF, null, null);
        expectedFeedPreferences.setKeepUpdated(false);
//        clickPreference(R.string.keep_updated);
        onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0, new CustomViewAction()));

        Awaitility.await().atMost(4000, MILLISECONDS)
                .until(() -> {
                    for (Feed feed : feeds) {
                        Assert.assertEquals(expectedFeedPreferences.getKeepUpdated(), feed.getPreferences().getKeepUpdated());
                    }
                    return true;
                });

        expectedFeedPreferences.setKeepUpdated(true);
        onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0, new CustomViewAction()));

        Awaitility.await().atMost(4000, MILLISECONDS)
                .until(() -> {
                    for (Feed feed : feeds) {
                        Assert.assertEquals(expectedFeedPreferences.getKeepUpdated(), feed.getPreferences().getKeepUpdated());
                    }
                    return true;
                });
        try {
            Thread.sleep(1000);
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


    @Test
    public void testClick_playBackSpeed() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        clickPreference(R.string.playback_speed);
//        final String[] speeds = activityRule.getActivity().getResources().getStringArray(R.array.playback_speed_values);
//        String[] values = new String[speeds.length + 1];
//        values[0] = BatchFeedSettingsFragment.FeedSettingsPreferenceFragment.SPEED_FORMAT.format(SPEED_USE_GLOBAL);
        onData(allOf()).perform(click());
//        onView(withText(R.string.cancel_label)).perform(click());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class CustomViewAction implements ViewAction {

        @Override
        public Matcher<View> getConstraints() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public void perform(UiController uiController, View view) {
            SwitchCompat switchCompat = view.findViewById(android.R.id.switch_widget);
            view.performClick();
        }
    }
}
