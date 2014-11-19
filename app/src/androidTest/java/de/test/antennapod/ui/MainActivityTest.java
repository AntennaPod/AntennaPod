package de.test.antennapod.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import com.robotium.solo.Solo;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.DefaultOnlineFeedViewActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.PreferenceActivityGingerbread;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.preferences.PreferenceController;

/**
 * User interface tests for MainActivity
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;
    private UITestUtils uiTestUtils;

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
        SharedPreferences prefs = getInstrumentation().getTargetContext().getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(MainActivity.PREF_IS_FIRST_LAUNCH, false).commit();
    }

    @Override
    protected void tearDown() throws Exception {
        uiTestUtils.tearDown();
        solo.finishOpenedActivities();
        PodDBAdapter.deleteDatabase(getInstrumentation().getTargetContext());
        super.tearDown();
    }

    public void testAddFeed() throws Exception {
        uiTestUtils.addHostedFeedData();
        final Feed feed = uiTestUtils.hostedFeeds.get(0);
        solo.setNavigationDrawer(Solo.OPENED);
        solo.clickOnText(solo.getString(R.string.add_feed_label));
        solo.enterText(0, feed.getDownload_url());
        solo.clickOnButton(0);
        solo.waitForActivity(DefaultOnlineFeedViewActivity.class);
        solo.waitForView(R.id.butSubscribe);
        assertEquals(solo.getString(R.string.subscribe_label), solo.getButton(0).getText().toString());
        solo.clickOnButton(0);
        solo.waitForText(solo.getString(R.string.subscribed_label));
    }

    public void testClickNavDrawer() throws Exception {
        uiTestUtils.addLocalFeedData(false);
        final View home = solo.getView(UITestUtils.HOME_VIEW);

        // all episodes
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.all_episodes_label), getActionbarTitle());
        // queue
        solo.clickOnView(home);
        solo.clickOnText(solo.getString(R.string.queue_label));
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.queue_label), getActionbarTitle());

        // downloads
        solo.clickOnView(home);
        solo.clickOnText(solo.getString(R.string.downloads_label));
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.downloads_label), getActionbarTitle());

        // playback history
        solo.clickOnView(home);
        solo.clickOnText(solo.getString(R.string.playback_history_label));
        solo.waitForView(android.R.id.list);
        assertEquals(solo.getString(R.string.playback_history_label), getActionbarTitle());

        // add podcast
        solo.clickOnView(home);
        solo.clickOnText(solo.getString(R.string.add_feed_label));
        solo.waitForView(R.id.txtvFeedurl);
        assertEquals(solo.getString(R.string.add_feed_label), getActionbarTitle());

        // podcasts
        for (int i = 0; i < uiTestUtils.hostedFeeds.size(); i++) {
            Feed f = uiTestUtils.hostedFeeds.get(i);
            solo.clickOnView(home);
            solo.clickOnText(f.getTitle());
            solo.waitForView(android.R.id.list);
            assertEquals("", getActionbarTitle());
        }
    }

    private String getActionbarTitle() {
        return ((MainActivity)solo.getCurrentActivity()).getMainActivtyActionBar().getTitle().toString();
    }

    public void testGoToPreferences() {
        solo.setNavigationDrawer(Solo.CLOSED);
        solo.clickOnMenuItem(solo.getString(R.string.settings_label));
        solo.waitForActivity(PreferenceController.getPreferenceActivity());
    }
}
