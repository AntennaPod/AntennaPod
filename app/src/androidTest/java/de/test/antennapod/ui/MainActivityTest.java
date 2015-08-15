package de.test.antennapod.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.ActivityInstrumentationTestCase2;
import android.test.FlakyTest;
import android.widget.ListView;

import com.robotium.solo.Solo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.DefaultOnlineFeedViewActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.fragment.AllEpisodesFragment;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.NewEpisodesFragment;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.preferences.PreferenceController;

/**
 * User interface tests for MainActivity
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;
    private UITestUtils uiTestUtils;

    private SharedPreferences prefs;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
        uiTestUtils = new UITestUtils(getInstrumentation().getTargetContext());
        uiTestUtils.setup();
        // create database
        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getTargetContext());
        adapter.open();
        adapter.close();

        // override first launch preference
        prefs = getInstrumentation().getTargetContext().getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(MainActivity.PREF_IS_FIRST_LAUNCH, false).commit();
    }

    @Override
    protected void tearDown() throws Exception {
        uiTestUtils.tearDown();
        solo.finishOpenedActivities();

        PodDBAdapter.deleteDatabase(getInstrumentation().getTargetContext());

        // reset preferences
        prefs.edit().clear().commit();

        super.tearDown();
    }

    private void openNavDrawer() {
        solo.clickOnScreen(50, 50);
    }

    public void testAddFeed() throws Exception {
        uiTestUtils.addHostedFeedData();
        final Feed feed = uiTestUtils.hostedFeeds.get(0);
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.add_feed_label));
        solo.enterText(0, feed.getDownload_url());
        solo.clickOnButton(solo.getString(R.string.confirm_label));
        solo.waitForActivity(DefaultOnlineFeedViewActivity.class);
        solo.waitForView(R.id.butSubscribe);
        assertEquals(solo.getString(R.string.subscribe_label), solo.getButton(0).getText().toString());
        solo.clickOnButton(0);
        solo.waitForText(solo.getString(R.string.subscribed_label));
    }

    @FlakyTest(tolerance = 3)
    public void testClickNavDrawer() throws Exception {
        uiTestUtils.addLocalFeedData(false);

        UserPreferences.setHiddenDrawerItems(getInstrumentation().getTargetContext(), new ArrayList<String>());

        // queue
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.queue_label));
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.queue_label), getActionbarTitle());

        // new episodes
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.new_episodes_label));
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.new_episodes_label), getActionbarTitle());

        // all episodes
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.all_episodes_label));
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.all_episodes_label), getActionbarTitle());

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
        ListView list = (ListView)solo.getView(R.id.nav_list);
        for (int i = 0; i < uiTestUtils.hostedFeeds.size(); i++) {
            Feed f = uiTestUtils.hostedFeeds.get(i);
            solo.clickOnScreen(50, 50); // open nav drawer
            solo.scrollListToLine(list, i);
            solo.clickOnText(f.getTitle());
            solo.waitForView(android.R.id.list);
            assertEquals("", getActionbarTitle());
        }
    }

    private String getActionbarTitle() {
        return ((MainActivity)solo.getCurrentActivity()).getMainActivtyActionBar().getTitle().toString();
    }

    @FlakyTest(tolerance = 3)
    public void testGoToPreferences() {
        openNavDrawer();
        solo.clickOnText(solo.getString(R.string.settings_label));
        solo.waitForActivity(PreferenceController.getPreferenceActivity());
    }

    public void testDrawerPreferencesHideSomeElements() {
        UserPreferences.setHiddenDrawerItems(getInstrumentation().getTargetContext(), new ArrayList<String>());
        openNavDrawer();
        solo.clickLongOnText(solo.getString(R.string.queue_label));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.all_episodes_label));
        solo.clickOnText(solo.getString(R.string.playback_history_label));
        solo.clickOnText(solo.getString(R.string.confirm_label));
        solo.waitForDialogToClose();
        List<String> hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(2, hidden.size());
        assertTrue(hidden.contains(AllEpisodesFragment.TAG));
        assertTrue(hidden.contains(PlaybackHistoryFragment.TAG));
    }

    public void testDrawerPreferencesUnhideSomeElements() {
        List<String> hidden = Arrays.asList(NewEpisodesFragment.TAG, DownloadsFragment.TAG);
        UserPreferences.setHiddenDrawerItems(getInstrumentation().getTargetContext(), hidden);
        openNavDrawer();
        solo.clickLongOnText(solo.getString(R.string.queue_label));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.downloads_label));
        solo.clickOnText(solo.getString(R.string.queue_label));
        solo.clickOnText(solo.getString(R.string.confirm_label));
        solo.waitForDialogToClose();
        hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(2, hidden.size());
        assertTrue(hidden.contains(QueueFragment.TAG));
        assertTrue(hidden.contains(NewEpisodesFragment.TAG));
    }

    public void testDrawerPreferencesHideAllElements() {
        UserPreferences.setHiddenDrawerItems(getInstrumentation().getTargetContext(), new ArrayList<String>());
        String[] titles = getInstrumentation().getTargetContext().getResources().getStringArray(R.array.nav_drawer_titles);

        openNavDrawer();
        solo.clickLongOnText(solo.getString(R.string.queue_label));
        solo.waitForDialogToOpen();
        for(String title : titles) {
            solo.clickOnText(title);
        }
        solo.clickOnText(solo.getString(R.string.confirm_label));
        solo.waitForDialogToClose();
        List<String> hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(6, hidden.size());
        for(String tag : MainActivity.NAV_DRAWER_TAGS) {
            assertTrue(hidden.contains(tag));
        }
    }

    public void testDrawerPreferencesHideCurrentElement() {
        UserPreferences.setHiddenDrawerItems(getInstrumentation().getTargetContext(), new ArrayList<String>());

        openNavDrawer();
        String downloads = solo.getString(R.string.downloads_label);
        solo.clickOnText(downloads);
        solo.waitForView(android.R.id.list);
        openNavDrawer();
        solo.clickLongOnText(downloads);
        solo.waitForDialogToOpen();
        solo.clickOnText(downloads);
        solo.clickOnText(solo.getString(R.string.confirm_label));
        solo.waitForDialogToClose();
        List<String> hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(1, hidden.size());
        assertTrue(hidden.contains(DownloadsFragment.TAG));
    }
}
