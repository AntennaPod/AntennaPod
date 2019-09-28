package de.test.antennapod.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import android.widget.ListView;

import com.robotium.solo.Solo;
import com.robotium.solo.Timeout;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * test cases for starting and ending playback from the MainActivity and AudioPlayerActivity
 */
@LargeTest
public class PlaybackTest {
    private static final int EPISODES_DRAWER_LIST_INDEX = 1;
    private static final int QUEUE_DRAWER_LIST_INDEX = 0;

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

    private Solo solo;
    private UITestUtils uiTestUtils;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();

        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .clear()
                .putBoolean(UserPreferences.PREF_UNPAUSE_ON_HEADSET_RECONNECT, false)
                .putBoolean(UserPreferences.PREF_PAUSE_ON_HEADSET_DISCONNECT, false)
                .commit();

        solo = new Solo(InstrumentationRegistry.getInstrumentation(), getActivity());

        uiTestUtils = new UITestUtils(context);
        uiTestUtils.setup();

        // create database
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();
    }

    @After
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        uiTestUtils.tearDown();

        // shut down playback service
        skipEpisode();
        context.sendBroadcast(new Intent(PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
    }

    private MainActivity getActivity() {
        return activityTestRule.getActivity();
    }

    private void openNavDrawer() {
        solo.clickOnImageButton(0);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void setContinuousPlaybackPreference(boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(UserPreferences.PREF_FOLLOW_QUEUE, value).commit();
    }

    private void setSkipKeepsEpisodePreference(boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(UserPreferences.PREF_SKIP_KEEPS_EPISODE, value).commit();
    }

    private void setSmartMarkAsPlayedPreference(int smartMarkAsPlayedSecs) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(UserPreferences.PREF_SMART_MARK_AS_PLAYED_SECS,
                Integer.toString(smartMarkAsPlayedSecs, 10))
                .commit();
    }

    private void skipEpisode() {
        Intent skipIntent = new Intent(PlaybackService.ACTION_SKIP_CURRENT_EPISODE);
        context.sendBroadcast(skipIntent);
    }

    private void pauseEpisode() {
        Intent pauseIntent = new Intent(PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE);
        context.sendBroadcast(pauseIntent);
    }

    private void startLocalPlayback() {
        openNavDrawer();
        // if we try to just click on plain old text then
        // we might wind up clicking on the fragment title and not
        // the drawer element like we want.
        ListView drawerView = (ListView)solo.getView(R.id.nav_list);
        // this should be 'Episodes'
        View targetView = drawerView.getChildAt(EPISODES_DRAWER_LIST_INDEX);
        solo.waitForView(targetView);
        solo.clickOnView(targetView);
        solo.waitForText(solo.getString(R.string.all_episodes_short_label));
        solo.clickOnText(solo.getString(R.string.all_episodes_short_label));

        final List<FeedItem> episodes = DBReader.getRecentlyPublishedEpisodes(0, 10);
        assertTrue(solo.waitForView(solo.getView(R.id.butSecondaryAction)));

        solo.clickOnView(solo.getView(R.id.butSecondaryAction));
        long mediaId = episodes.get(0).getMedia().getId();
        boolean playing = solo.waitForCondition(() -> {
            if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId() == mediaId;
            } else {
                return false;
            }
        }, Timeout.getSmallTimeout());
        assertTrue(playing);
    }

    private void startLocalPlaybackFromQueue() {
        gotoQueueScreen();
        playFromQueue(0);
    }

    private void gotoQueueScreen() {
        openNavDrawer();
        // if we try to just click on plain old text then
        // we might wind up clicking on the fragment title and not
        // the drawer element like we want.
        ListView drawerView = (ListView)solo.getView(R.id.nav_list);
        // this should be 'Queue'
        View targetView = drawerView.getChildAt(QUEUE_DRAWER_LIST_INDEX);
        solo.waitForView(targetView);
        solo.clickOnView(targetView);
        assertTrue(solo.waitForView(solo.getView(R.id.butSecondaryAction)));
    }

    /**
     *
     * @param itemIdx The 0-based index of the episode to be played in the queue.
     */
    private void playFromQueue(int itemIdx) {
        final List<FeedItem> queue = DBReader.getQueue();
        solo.clickOnImageButton(itemIdx + 1);
        assertTrue(solo.waitForView(solo.getView(R.id.butPlay)));
        long mediaId = queue.get(itemIdx).getMedia().getId();
        boolean playing = solo.waitForCondition(() -> {
            if(uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId() == mediaId;
            } else {
                return false;
            }
        }, Timeout.getSmallTimeout());

        assertTrue(playing);
    }

    @Test
    public void testStartLocal() throws Exception {
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue().get();
        startLocalPlayback();
    }

    @Test
    public void testContinousPlaybackOffSingleEpisode() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue().get();
        startLocalPlayback();
    }

    @Test
    public void testContinousPlaybackOffMultipleEpisodes() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addLocalFeedData(true);
        List<FeedItem> queue = DBReader.getQueue();
        final FeedItem first = queue.get(0);
        startLocalPlaybackFromQueue();
        boolean stopped = solo.waitForCondition(() -> {
            if (uiTestUtils.getPlaybackController(getActivity()).getStatus()
                    != PlayerStatus.PLAYING) {
                return true;
            } else if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId()
                        != first.getMedia().getId();
            } else {
                return true;
            }
        }, Timeout.getSmallTimeout());
        assertTrue(stopped);
        Thread.sleep(1000);
        PlayerStatus status = uiTestUtils.getPlaybackController(getActivity()).getStatus();
        assertFalse(status.equals(PlayerStatus.PLAYING));
    }

    @Test
    public void testContinuousPlaybackOnMultipleEpisodes() throws Exception {
        setContinuousPlaybackPreference(true);
        uiTestUtils.addLocalFeedData(true);
        List<FeedItem> queue = DBReader.getQueue();
        final FeedItem first = queue.get(0);
        final FeedItem second = queue.get(1);

        startLocalPlaybackFromQueue();
        boolean firstPlaying = solo.waitForCondition(() -> {
            if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId()
                        == first.getMedia().getId();
            } else {
                return false;
            }
        }, Timeout.getSmallTimeout());
        assertTrue(firstPlaying);
        boolean secondPlaying = solo.waitForCondition(() -> {
            if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId()
                        == second.getMedia().getId();
            } else {
                return false;
            }
        }, Timeout.getLargeTimeout());
        assertTrue(secondPlaying);
    }

    /**
     * Check if an episode can be played twice without problems.
     */
    private void replayEpisodeCheck(boolean followQueue) throws Exception {
        setContinuousPlaybackPreference(followQueue);
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue().get();
        final List<FeedItem> episodes = DBReader.getRecentlyPublishedEpisodes(0, 10);

        startLocalPlayback();
        long mediaId = episodes.get(0).getMedia().getId();
        boolean startedPlaying = solo.waitForCondition(() -> {
            if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId() == mediaId;
            } else {
                return false;
            }
        }, Timeout.getSmallTimeout());
        assertTrue(startedPlaying);

        boolean stoppedPlaying = solo.waitForCondition(() ->
                uiTestUtils.getCurrentMedia(getActivity()) == null
                || uiTestUtils.getCurrentMedia(getActivity()).getId() != mediaId, Timeout.getLargeTimeout());
        assertTrue(stoppedPlaying);

        startLocalPlayback();
        boolean startedReplay = solo.waitForCondition(() -> {
            if(uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId() == mediaId;
            } else {
                return false;
            }
        }, Timeout.getLargeTimeout());
        assertTrue(startedReplay);
    }

    @Test
    public void testReplayEpisodeContinuousPlaybackOn() throws Exception {
        replayEpisodeCheck(true);
    }

    @Test
    public void testReplayEpisodeContinuousPlaybackOff() throws Exception {
        replayEpisodeCheck(false);
    }

    @Test
    public void testSmartMarkAsPlayed_Skip_Average() throws Exception {
        doTestSmartMarkAsPlayed_Skip_ForEpisode(0);
    }

    @Test
    public void testSmartMarkAsPlayed_Skip_LastEpisodeInQueue() throws Exception {
        doTestSmartMarkAsPlayed_Skip_ForEpisode(-1);
    }

    private void doTestSmartMarkAsPlayed_Skip_ForEpisode(int itemIdxNegAllowed) throws Exception {
        setSmartMarkAsPlayedPreference(60);
        // ensure when an episode is skipped, it is removed due to smart as played
        setSkipKeepsEpisodePreference(false);

        uiTestUtils.addLocalFeedData(true);

        int fiIdx;
        if (itemIdxNegAllowed >= 0) {
            fiIdx = itemIdxNegAllowed;
        } else { // negative index: count from the end, with -1 being the last one, etc.
            fiIdx = DBReader.getQueue().size() + itemIdxNegAllowed;
        }
        final FeedItem feedItem = DBReader.getQueue().get(fiIdx);

        gotoQueueScreen();
        playFromQueue(fiIdx);

        skipEpisode();
        Thread.sleep(1000); // ensure the skip is processed

        //  assert item no longer in queue
        assertThat("Ensure smart mark as play will lead to the item removed from the queue",
                DBReader.getQueue(), not(hasItems(feedItem)));
        assertThat(DBReader.getFeedItem(feedItem.getId()).isPlayed(), is(true));
    }

    @Test
    public void testSmartMarkAsPlayed_Pause_WontAffectItem() throws Exception {
        setSmartMarkAsPlayedPreference(60);

        uiTestUtils.addLocalFeedData(true);

        final int fiIdx = 0;
        final FeedItem feedItem = DBReader.getQueue().get(fiIdx);

        gotoQueueScreen();
        playFromQueue(fiIdx);

        // let playback run a bit then pause
        Thread.sleep(500);
        pauseEpisode();
        Thread.sleep(1000); // ensure the pause is processed

        //  assert item no longer in queue
        assertThat("Ensure even with smart mark as play, after pause, the item remains in the queue.",
                DBReader.getQueue(), hasItems(feedItem));
        assertThat("Ensure even with smart mark as play, after pause, the item played status remains false.",
                DBReader.getFeedItem(feedItem.getId()).isPlayed(), is(false));
    }

}
