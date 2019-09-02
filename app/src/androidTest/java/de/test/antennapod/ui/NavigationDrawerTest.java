package de.test.antennapod.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.dialog.RatingDialog;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.test.antennapod.EspressoTestUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static de.test.antennapod.NthMatcher.first;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

/**
 * User interface tests for MainActivity drawer
 */
@RunWith(AndroidJUnit4.class)
public class NavigationDrawerTest {

    private UITestUtils uiTestUtils;

    @Rule
    public IntentsTestRule<MainActivity> mActivityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws IOException {
        uiTestUtils = new UITestUtils(InstrumentationRegistry.getTargetContext());
        uiTestUtils.setup();

        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.makeNotFirstRun();
        EspressoTestUtils.clearDatabase();
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
    }

    private void openNavDrawer() {
        onView(isRoot()).perform(waitForView(withId(R.id.drawer_layout), 1000));
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    }

    private ViewInteraction onDrawerItem(Matcher<View> viewMatcher) {
        return onView(allOf(viewMatcher, withId(R.id.txtvTitle)));
    }

    @Test
    public void testClickNavDrawer() throws Exception {
        uiTestUtils.addLocalFeedData(false);
        UserPreferences.setHiddenDrawerItems(new ArrayList<>());
        mActivityRule.launchActivity(new Intent());
        MainActivity activity = mActivityRule.getActivity();

        // queue
        openNavDrawer();
        onDrawerItem(withText(R.string.queue_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(R.id.recyclerView), 1000));
        assertEquals(activity.getString(R.string.queue_label), activity.getSupportActionBar().getTitle());

        // episodes
        openNavDrawer();
        onDrawerItem(withText(R.string.episodes_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(android.R.id.list), 1000));
        assertEquals(activity.getString(R.string.episodes_label), activity.getSupportActionBar().getTitle());

        // Subscriptions
        openNavDrawer();
        onDrawerItem(withText(R.string.subscriptions_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(R.id.subscriptions_grid), 1000));
        assertEquals(activity.getString(R.string.subscriptions_label), activity.getSupportActionBar().getTitle());

        // downloads
        openNavDrawer();
        onDrawerItem(withText(R.string.downloads_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(android.R.id.list), 1000));
        assertEquals(activity.getString(R.string.downloads_label), activity.getSupportActionBar().getTitle());

        // playback history
        openNavDrawer();
        onDrawerItem(withText(R.string.playback_history_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(android.R.id.list), 1000));
        assertEquals(activity.getString(R.string.playback_history_label), activity.getSupportActionBar().getTitle());

        // add podcast
        openNavDrawer();
        onDrawerItem(withText(R.string.add_feed_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(R.id.txtvFeedurl), 1000));
        assertEquals(activity.getString(R.string.add_feed_label), activity.getSupportActionBar().getTitle());

        // podcasts
        for (int i = 0; i < uiTestUtils.hostedFeeds.size(); i++) {
            Feed f = uiTestUtils.hostedFeeds.get(i);
            openNavDrawer();
            onDrawerItem(withText(f.getTitle())).perform(scrollTo(), click());
            onView(isRoot()).perform(waitForView(withId(android.R.id.list), 1000));
            assertEquals("", activity.getSupportActionBar().getTitle());
        }
    }

    @Test
    public void testGoToPreferences() {
        mActivityRule.launchActivity(new Intent());
        openNavDrawer();
        onView(withText(R.string.settings_label)).perform(click());
        intended(hasComponent(PreferenceActivity.class.getName()));
    }

    @Test
    public void testDrawerPreferencesHideSomeElements() {
        UserPreferences.setHiddenDrawerItems(new ArrayList<>());
        mActivityRule.launchActivity(new Intent());
        openNavDrawer();
        onDrawerItem(withText(R.string.queue_label)).perform(longClick());
        onView(withText(R.string.episodes_label)).perform(click());
        onView(withText(R.string.playback_history_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());

        List<String> hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(2, hidden.size());
        assertTrue(hidden.contains(EpisodesFragment.TAG));
        assertTrue(hidden.contains(PlaybackHistoryFragment.TAG));
    }

    @Test
    public void testDrawerPreferencesUnhideSomeElements() {
        List<String> hidden = Arrays.asList(PlaybackHistoryFragment.TAG, DownloadsFragment.TAG);
        UserPreferences.setHiddenDrawerItems(hidden);
        mActivityRule.launchActivity(new Intent());
        openNavDrawer();
        onView(first(withText(R.string.queue_label))).perform(longClick());

        onView(withText(R.string.downloads_label)).perform(click());
        onView(withText(R.string.queue_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());

        hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(2, hidden.size());
        assertTrue(hidden.contains(QueueFragment.TAG));
        assertTrue(hidden.contains(PlaybackHistoryFragment.TAG));
    }


    @Test
    public void testDrawerPreferencesHideAllElements() {
        UserPreferences.setHiddenDrawerItems(new ArrayList<>());
        mActivityRule.launchActivity(new Intent());
        String[] titles = mActivityRule.getActivity().getResources().getStringArray(R.array.nav_drawer_titles);

        openNavDrawer();
        onView(first(withText(R.string.queue_label))).perform(longClick());
        for (String title : titles) {
            onView(first(withText(title))).perform(click());
        }
        onView(withText(R.string.confirm_label)).perform(click());

        List<String> hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(titles.length, hidden.size());
        for (String tag : MainActivity.NAV_DRAWER_TAGS) {
            assertTrue(hidden.contains(tag));
        }
    }

    @Test
    public void testDrawerPreferencesHideCurrentElement() {
        UserPreferences.setHiddenDrawerItems(new ArrayList<>());
        mActivityRule.launchActivity(new Intent());
        openNavDrawer();
        onView(withText(R.string.downloads_label)).perform(click());
        openNavDrawer();

        onView(first(withText(R.string.queue_label))).perform(longClick());
        onView(first(withText(R.string.downloads_label))).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());

        List<String> hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(1, hidden.size());
        assertTrue(hidden.contains(DownloadsFragment.TAG));
    }
}
