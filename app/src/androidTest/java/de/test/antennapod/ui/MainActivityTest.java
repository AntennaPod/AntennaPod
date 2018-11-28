package de.test.antennapod.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.intent.Intents;
import android.support.test.filters.FlakyTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.ListView;
import com.robotium.solo.Solo;
import com.robotium.solo.Timeout;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.NthMatcher.first;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * User interface tests for MainActivity
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private Solo solo;
    private UITestUtils uiTestUtils;
    private SharedPreferences prefs;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws IOException {
        Intents.init();
        Context context = mActivityRule.getActivity();
        uiTestUtils = new UITestUtils(context);
        uiTestUtils.setup();

        // create new database
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

        // override first launch preference
        // do this BEFORE calling getActivity()!
        prefs = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(MainActivity.PREF_IS_FIRST_LAUNCH, false).commit();

        solo = new Solo(getInstrumentation(), mActivityRule.getActivity());
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
        solo.finishOpenedActivities();
        Intents.release();
        PodDBAdapter.deleteDatabase();
        prefs.edit().clear().commit();
    }

    private void openNavDrawer() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    }

    @Test
    public void testAddFeed() throws Exception {
        uiTestUtils.addHostedFeedData();
        final Feed feed = uiTestUtils.hostedFeeds.get(0);
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.add_feed_label));
        solo.enterText(0, feed.getDownload_url());
        solo.clickOnButton(solo.getString(R.string.confirm_label));
        solo.waitForActivity(OnlineFeedViewActivity.class);
        solo.waitForView(R.id.butSubscribe);
        assertEquals(solo.getString(R.string.subscribe_label), solo.getButton(0).getText().toString());
        solo.clickOnButton(0);
        assertTrue(solo.waitForText(solo.getString(R.string.open_podcast), 0, Timeout.getLargeTimeout(), false));
    }


    @Test
    @FlakyTest
    public void testClickNavDrawer() throws Exception {
        uiTestUtils.addLocalFeedData(false);

        UserPreferences.setHiddenDrawerItems(new ArrayList<>());

        // queue
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.queue_label));
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.queue_label), getActionbarTitle());

        // episodes
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.episodes_label));
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.episodes_label), getActionbarTitle());

        // Subscriptions
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.subscriptions_label));
        solo.waitForView(R.id.subscriptions_grid);
        assertEquals(solo.getString(R.string.subscriptions_label), getActionbarTitle());

        // downloads
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.downloads_label));
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.downloads_label), getActionbarTitle());

        // playback history
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.playback_history_label));
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.playback_history_label), getActionbarTitle());

        // add podcast
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.add_feed_label));
        solo.waitForView(R.id.txtvFeedurl);
        assertEquals(solo.getString(R.string.add_feed_label), getActionbarTitle());

        // podcasts
        ListView list = (ListView) solo.getView(R.id.nav_list);
        for (int i = 0; i < uiTestUtils.hostedFeeds.size(); i++) {
            Feed f = uiTestUtils.hostedFeeds.get(i);
            openNavDrawer();
            solo.scrollListToLine(list, i);
            solo.clickOnText(f.getTitle());
            solo.waitForView(android.R.id.list);
            assertEquals("", getActionbarTitle());
        }
    }

    private String getActionbarTitle() {
        return ((MainActivity) solo.getCurrentActivity()).getSupportActionBar().getTitle().toString();
    }


    @Test
    @FlakyTest
    public void testGoToPreferences() {
        openNavDrawer();
        onView(withText(R.string.settings_label)).perform(click());
        intended(hasComponent(PreferenceActivity.class.getName()));
    }

    @Test
    public void testDrawerPreferencesHideSomeElements() {
        UserPreferences.setHiddenDrawerItems(new ArrayList<>());
        openNavDrawer();
        onView(first(withText(R.string.queue_label))).perform(longClick());
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

    @Test
    public void testBackButtonBehaviorGoToPage() {
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.settings_label));
        solo.clickOnText(solo.getString(R.string.user_interface_label));
        solo.clickOnText(solo.getString(R.string.pref_back_button_behavior_title));
        solo.clickOnText(solo.getString(R.string.back_button_go_to_page));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.subscriptions_label));
        solo.clickOnText(solo.getString(R.string.confirm_label));
        solo.goBackToActivity(MainActivity.class.getSimpleName());
        solo.goBack();
        assertEquals(solo.getString(R.string.subscriptions_label), getActionbarTitle());
    }

    @Test
    public void testBackButtonBehaviorOpenDrawer() {
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.settings_label));
        solo.clickOnText(solo.getString(R.string.user_interface_label));
        solo.clickOnText(solo.getString(R.string.pref_back_button_behavior_title));
        solo.clickOnText(solo.getString(R.string.back_button_open_drawer));
        solo.goBackToActivity(MainActivity.class.getSimpleName());
        solo.goBack();
        assertTrue(((MainActivity)solo.getCurrentActivity()).isDrawerOpen());
    }

    @Test
    public void testBackButtonBehaviorDoubleTap() {
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.settings_label));
        solo.clickOnText(solo.getString(R.string.user_interface_label));
        solo.clickOnText(solo.getString(R.string.pref_back_button_behavior_title));
        solo.clickOnText(solo.getString(R.string.back_button_double_tap));
        solo.goBackToActivity(MainActivity.class.getSimpleName());
        solo.goBack();
        solo.goBack();
        assertTrue(solo.getCurrentActivity().isFinishing());
    }

    @Test
    public void testBackButtonBehaviorPrompt() {
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.settings_label));
        solo.clickOnText(solo.getString(R.string.user_interface_label));
        solo.clickOnText(solo.getString(R.string.pref_back_button_behavior_title));
        solo.clickOnText(solo.getString(R.string.back_button_show_prompt));
        solo.goBackToActivity(MainActivity.class.getSimpleName());
        solo.goBack();
        solo.clickOnText(solo.getString(R.string.yes));
        solo.waitForDialogToClose();
        assertTrue(solo.getCurrentActivity().isFinishing());
    }

    @Test
    public void testBackButtonBehaviorDefault() {
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.settings_label));
        solo.clickOnText(solo.getString(R.string.user_interface_label));
        solo.clickOnText(solo.getString(R.string.pref_back_button_behavior_title));
        solo.clickOnText(solo.getString(R.string.back_button_default));
        solo.goBackToActivity(MainActivity.class.getSimpleName());
        solo.goBack();
        assertTrue(solo.getCurrentActivity().isFinishing());
    }
}
