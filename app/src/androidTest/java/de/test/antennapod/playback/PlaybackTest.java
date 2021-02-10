package de.test.antennapod.playback;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.view.View;

import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import de.danoeh.antennapod.core.feed.FeedItemFilter;
import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.test.antennapod.EspressoTestUtils;
import de.test.antennapod.IgnoreOnCi;
import de.test.antennapod.ui.UITestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.clickChildViewWithId;
import static de.test.antennapod.EspressoTestUtils.onDrawerItem;
import static de.test.antennapod.EspressoTestUtils.openNavDrawer;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for starting and ending playback from the MainActivity and AudioPlayerActivity.
 */
@LargeTest
@IgnoreOnCi
@RunWith(Parameterized.class)
public class PlaybackTest {
    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, false, false);

    @Parameterized.Parameter(value = 0)
    public String playerToUse;
    private UITestUtils uiTestUtils;
    protected Context context;
    private PlaybackController controller;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> initParameters() {
        return Arrays.asList(new Object[][] { { "exoplayer" }, { "builtin" }, { "sonic" } });
    }

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(UserPreferences.PREF_MEDIA_PLAYER, playerToUse).apply();

        uiTestUtils = new UITestUtils(context);
        uiTestUtils.setup();
    }

    @After
    public void tearDown() throws Exception {
        activityTestRule.finishActivity();
        EspressoTestUtils.tryKillPlaybackService();
        uiTestUtils.tearDown();
        if (controller != null) {
            controller.release();
        }
    }

    private void setupPlaybackController() {
        controller = new PlaybackController(activityTestRule.getActivity());
        controller.init();
    }

    @Test
    public void testContinousPlaybackOffMultipleEpisodes() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addLocalFeedData(true);
        activityTestRule.launchActivity(new Intent());
        setupPlaybackController();
        playFromQueue(0);
        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> controller.getStatus() == PlayerStatus.INITIALIZED);
    }

    @Test
    public void testContinuousPlaybackOnMultipleEpisodes() throws Exception {
        setContinuousPlaybackPreference(true);
        uiTestUtils.addLocalFeedData(true);
        activityTestRule.launchActivity(new Intent());

        List<FeedItem> queue = DBReader.getQueue();
        final FeedItem first = queue.get(0);
        final FeedItem second = queue.get(1);

        playFromQueue(0);
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(
                () -> first.getMedia().getId() == PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
        Awaitility.await().atMost(6, TimeUnit.SECONDS).until(
                () -> second.getMedia().getId() == PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
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

    @Test
    public void testSmartMarkAsPlayed_Pause_WontAffectItem() throws Exception {
        setSmartMarkAsPlayedPreference(60);

        uiTestUtils.addLocalFeedData(true);
        activityTestRule.launchActivity(new Intent());
        setupPlaybackController();

        final int fiIdx = 0;
        final FeedItem feedItem = DBReader.getQueue().get(fiIdx);

        playFromQueue(fiIdx);

        // let playback run a bit then pause
        Awaitility.await()
                .atMost(1000, MILLISECONDS)
                .until(() -> PlayerStatus.PLAYING == controller.getStatus());
        pauseEpisode();
        Awaitility.await()
                .atMost(1000, MILLISECONDS)
                .until(() -> PlayerStatus.PAUSED == controller.getStatus());

        assertThat("Ensure even with smart mark as play, after pause, the item remains in the queue.",
                DBReader.getQueue(), hasItems(feedItem));
        assertThat("Ensure even with smart mark as play, after pause, the item played status remains false.",
                DBReader.getFeedItem(feedItem.getId()).isPlayed(), is(false));
    }

    @Test
    public void testStartLocal() throws Exception {
        uiTestUtils.addLocalFeedData(true);
        activityTestRule.launchActivity(new Intent());
        DBWriter.clearQueue().get();
        startLocalPlayback();
    }

    @Test
    public void testPlayingItemAddsToQueue() throws Exception {
        uiTestUtils.addLocalFeedData(true);
        activityTestRule.launchActivity(new Intent());
        DBWriter.clearQueue().get();
        List<FeedItem> queue = DBReader.getQueue();
        assertEquals(0, queue.size());
        startLocalPlayback();
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(
                () -> 1 == DBReader.getQueue().size());
    }

    @Test
    public void testContinousPlaybackOffSingleEpisode() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addLocalFeedData(true);
        activityTestRule.launchActivity(new Intent());
        DBWriter.clearQueue().get();
        startLocalPlayback();
    }

    protected MainActivity getActivity() {
        return activityTestRule.getActivity();
    }

    protected void setContinuousPlaybackPreference(boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(UserPreferences.PREF_FOLLOW_QUEUE, value).commit();
    }

    protected void setSkipKeepsEpisodePreference(boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(UserPreferences.PREF_SKIP_KEEPS_EPISODE, value).commit();
    }

    protected void setSmartMarkAsPlayedPreference(int smartMarkAsPlayedSecs) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(UserPreferences.PREF_SMART_MARK_AS_PLAYED_SECS,
                Integer.toString(smartMarkAsPlayedSecs, 10))
                .commit();
    }

    private void skipEpisode() {
        IntentUtils.sendLocalBroadcast(context, PlaybackService.ACTION_SKIP_CURRENT_EPISODE);
    }

    protected void pauseEpisode() {
        IntentUtils.sendLocalBroadcast(context, PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE);
    }

    protected void startLocalPlayback() {
        openNavDrawer();
        onDrawerItem(withText(R.string.episodes_label)).perform(click());
        onView(isRoot()).perform(waitForView(withText(R.string.all_episodes_short_label), 1000));
        onView(withText(R.string.all_episodes_short_label)).perform(click());

        final List<FeedItem> episodes = DBReader.getRecentlyPublishedEpisodes(0, 10, FeedItemFilter.unfiltered());
        Matcher<View> allEpisodesMatcher = allOf(withId(android.R.id.list), isDisplayed(), hasMinimumChildCount(2));
        onView(isRoot()).perform(waitForView(allEpisodesMatcher, 1000));
        onView(allEpisodesMatcher).perform(actionOnItemAtPosition(0, clickChildViewWithId(R.id.secondaryActionButton)));

        FeedMedia media = episodes.get(0).getMedia();
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(
                () -> media.getId() == PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
    }

    /**
     *
     * @param itemIdx The 0-based index of the episode to be played in the queue.
     */
    protected void playFromQueue(int itemIdx) {
        final List<FeedItem> queue = DBReader.getQueue();

        Matcher<View> queueMatcher = allOf(withId(R.id.recyclerView), isDisplayed(), hasMinimumChildCount(2));
        onView(isRoot()).perform(waitForView(queueMatcher, 1000));
        onView(queueMatcher).perform(actionOnItemAtPosition(itemIdx, clickChildViewWithId(R.id.secondaryActionButton)));

        FeedMedia media = queue.get(itemIdx).getMedia();
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(
                () -> media.getId() == PlaybackPreferences.getCurrentlyPlayingFeedMediaId());

    }

    /**
     * Check if an episode can be played twice without problems.
     */
    protected void replayEpisodeCheck(boolean followQueue) throws Exception {
        setContinuousPlaybackPreference(followQueue);
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue().get();
        activityTestRule.launchActivity(new Intent());
        final List<FeedItem> episodes = DBReader.getRecentlyPublishedEpisodes(0, 10, FeedItemFilter.unfiltered());

        startLocalPlayback();
        FeedMedia media = episodes.get(0).getMedia();
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(
                () -> media.getId() == PlaybackPreferences.getCurrentlyPlayingFeedMediaId());

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(
                () -> media.getId() != PlaybackPreferences.getCurrentlyPlayingFeedMediaId());

        startLocalPlayback();

        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(
                () -> media.getId() == PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
    }

    protected void doTestSmartMarkAsPlayed_Skip_ForEpisode(int itemIdxNegAllowed) throws Exception {
        setSmartMarkAsPlayedPreference(60);
        // ensure when an episode is skipped, it is removed due to smart as played
        setSkipKeepsEpisodePreference(false);
        uiTestUtils.setMediaFileName("30sec.mp3");
        uiTestUtils.addLocalFeedData(true);

        LongList queue = DBReader.getQueueIDList();
        int fiIdx;
        if (itemIdxNegAllowed >= 0) {
            fiIdx = itemIdxNegAllowed;
        } else { // negative index: count from the end, with -1 being the last one, etc.
            fiIdx = queue.size() + itemIdxNegAllowed;
        }
        final long feedItemId = queue.get(fiIdx);
        queue.removeIndex(fiIdx);
        assertFalse(queue.contains(feedItemId)); // Verify that episode is in queue only once

        activityTestRule.launchActivity(new Intent());
        playFromQueue(fiIdx);

        skipEpisode();

        //  assert item no longer in queue (needs to wait till skip is asynchronously processed)
        Awaitility.await()
                .atMost(5000, MILLISECONDS)
                .until(() -> !DBReader.getQueueIDList().contains(feedItemId));
        assertTrue(DBReader.getFeedItem(feedItemId).isPlayed());
    }
}
