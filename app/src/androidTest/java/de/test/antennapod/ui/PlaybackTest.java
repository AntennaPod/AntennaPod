package de.test.antennapod.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;
import com.robotium.solo.Solo;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AudioplayerActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

import java.util.List;

/**
 * Test cases for starting and ending playback from the MainActivity and AudioPlayerActivity
 */
public class PlaybackTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;
    private UITestUtils uiTestUtils;

    public PlaybackTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
        uiTestUtils = new UITestUtils(getInstrumentation().getTargetContext());
        uiTestUtils.setup();
        // create database
        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getTargetContext());
        adapter.open();
        adapter.close();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getInstrumentation().getTargetContext());
        prefs.edit().putBoolean(UserPreferences.PREF_UNPAUSE_ON_HEADSET_RECONNECT, false).commit();
        prefs.edit().putBoolean(UserPreferences.PREF_PAUSE_ON_HEADSET_DISCONNECT, false).commit();
    }

    @Override
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
        solo.finishOpenedActivities();
        PodDBAdapter.deleteDatabase(getInstrumentation().getTargetContext());

        // shut down playback service
        skipEpisode();
        getInstrumentation().getTargetContext().sendBroadcast(
                new Intent(PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));

        super.tearDown();
    }

    private void setContinuousPlaybackPreference(boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getInstrumentation().getTargetContext());
        prefs.edit().putBoolean(UserPreferences.PREF_FOLLOW_QUEUE, value).commit();
    }

    private void skipEpisode() {
        Intent skipIntent = new Intent(PlaybackService.ACTION_SKIP_CURRENT_EPISODE);
        getInstrumentation().getTargetContext().sendBroadcast(skipIntent);
    }

    private void startLocalPlayback() {
        assertTrue(solo.waitForActivity(MainActivity.class));
        solo.setNavigationDrawer(Solo.CLOSED);
        solo.clickOnView(solo.getView(R.id.butSecondaryAction));
        assertTrue(solo.waitForActivity(AudioplayerActivity.class));
        assertTrue(solo.waitForView(solo.getView(R.id.butPlay)));
    }

    private void startLocalPlaybackFromQueue() {
        assertTrue(solo.waitForActivity(MainActivity.class));
        solo.clickOnView(solo.getView(UITestUtils.HOME_VIEW));
        solo.clickOnText(solo.getString(R.string.queue_label));
        assertTrue(solo.waitForView(solo.getView(R.id.butSecondaryAction)));
        solo.clickOnImageButton(0);
        assertTrue(solo.waitForActivity(AudioplayerActivity.class));
        assertTrue(solo.waitForView(solo.getView(R.id.butPlay)));
    }

    public void testStartLocal() throws Exception {
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue(getInstrumentation().getTargetContext()).get();
        startLocalPlayback();

        solo.clickOnView(solo.getView(R.id.butPlay));
    }

    public void testContinousPlaybackOffSingleEpisode() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue(getInstrumentation().getTargetContext()).get();
        startLocalPlayback();
        assertTrue(solo.waitForActivity(MainActivity.class));
    }


    public void testContinousPlaybackOffMultipleEpisodes() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addLocalFeedData(true);
        List<FeedItem> queue = DBReader.getQueue(getInstrumentation().getTargetContext());
        FeedItem second = queue.get(1);

        startLocalPlaybackFromQueue();
        assertTrue(solo.waitForText(second.getTitle()));
    }

    public void testContinuousPlaybackOnMultipleEpisodes() throws Exception {
        setContinuousPlaybackPreference(true);
        uiTestUtils.addLocalFeedData(true);
        List<FeedItem> queue = DBReader.getQueue(getInstrumentation().getTargetContext());
        FeedItem second = queue.get(1);

        startLocalPlaybackFromQueue();
        assertTrue(solo.waitForText(second.getTitle()));
    }

    /**
     * Check if an episode can be played twice without problems.
     */
    private void replayEpisodeCheck(boolean followQueue) throws Exception {
        setContinuousPlaybackPreference(followQueue);
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue(getInstrumentation().getTargetContext()).get();
        String title = ((TextView) solo.getView(R.id.txtvTitle)).getText().toString();
        startLocalPlayback();
        assertTrue(solo.waitForText(title));
        assertTrue(solo.waitForActivity(MainActivity.class));
        startLocalPlayback();
        assertTrue(solo.waitForText(title));
        assertTrue(solo.waitForActivity(MainActivity.class));
    }

    public void testReplayEpisodeContinuousPlaybackOn() throws Exception {
        replayEpisodeCheck(true);
    }

    public void testReplayEpisodeContinuousPlaybackOff() throws Exception {
        replayEpisodeCheck(false);
    }
}
