package de.test.antennapod.playback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.View;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.playback.service.Media3PlaybackService;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.LongList;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;
import de.test.antennapod.EspressoTestUtils;
import de.test.antennapod.IgnoreOnCi;
import de.test.antennapod.ui.UITestUtils;
import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static de.test.antennapod.EspressoTestUtils.clickBottomNavItem;
import static de.test.antennapod.EspressoTestUtils.clickBottomNavOverflow;
import static de.test.antennapod.EspressoTestUtils.clickChildViewWithId;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for starting and ending playback from the MainActivity and AudioPlayerActivity.
 */
@LargeTest
@IgnoreOnCi
@RunWith(AndroidJUnit4.class)
public class PlaybackTest {
    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, false, false);

    private UITestUtils uiTestUtils;
    protected Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();

        uiTestUtils = new UITestUtils(context);
        uiTestUtils.setup();
    }

    @After
    public void tearDown() throws Exception {
        activityTestRule.finishActivity();
        EspressoTestUtils.tryKillPlaybackService();
        uiTestUtils.tearDown();
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
    public void testContinuousPlaybackDoesNotAddNextEpisodeToHistory() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addHostedFeedData();
        for (Feed feed : uiTestUtils.hostedFeeds) {
            for (FeedItem item : feed.getItems()) {
                if (item.getMedia() != null) {
                    item.getMedia().setId(0);
                }
            }
        }
        uiTestUtils.addLocalFeedData(true);

        List<FeedItem> queue = DBReader.getQueue();
        final FeedItem second = queue.get(2);
        final long secondMediaId = second.getMedia().getId();

        DBWriter.deleteFromPlaybackHistory(second).get();
        assertNull(DBReader.getFeedMedia(secondMediaId).getLastPlayedTimeHistory());

        activityTestRule.launchActivity(new Intent());

        SessionToken sessionToken = new SessionToken(context,
                new ComponentName(context, Media3PlaybackService.class));
        MediaController mediaController = new MediaController.Builder(context, sessionToken)
                .buildAsync().get(5, TimeUnit.SECONDS);

        final AtomicBoolean episodeBReady = new AtomicBoolean(false);
        final AtomicReference<Date> historyAtReady = new AtomicReference<>();

        try {
            mediaController.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (mediaController.getCurrentMediaItem() != null
                            && String.valueOf(secondMediaId).equals(mediaController.getCurrentMediaItem().mediaId)) {
                        if (playbackState == Player.STATE_READY && !episodeBReady.get()) {
                            FeedMedia media = DBReader.getFeedMedia(secondMediaId);
                            if (media != null) {
                                historyAtReady.set(media.getLastPlayedTimeHistory());
                            }
                            episodeBReady.set(true);
                        }
                    }
                }
            });

            clickBottomNavItem(R.string.queue_label);
            playFromQueue(1);

            Awaitility.await().atMost(10, TimeUnit.SECONDS).until(episodeBReady::get);

            assertNull("Next episode should not be added to playback history when reaching STATE_READY",
                    historyAtReady.get());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(mediaController::release);
        }
    }

    @Test
    public void testLoadedPausedEpisodeNotAddedToHistoryWhenSwitchingToAnotherEpisode() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addHostedFeedData();
        for (Feed feed : uiTestUtils.hostedFeeds) {
            for (FeedItem item : feed.getItems()) {
                if (item.getMedia() != null) {
                    item.getMedia().setId(0);
                }
            }
        }
        uiTestUtils.addLocalFeedData(true);

        List<FeedItem> queue = DBReader.getQueue();
        final FeedItem second = queue.get(2);
        final long secondMediaId = second.getMedia().getId();
        final FeedItem third = queue.get(3);
        final long thirdMediaId = third.getMedia().getId();

        DBWriter.deleteFromPlaybackHistory(second).get();
        assertNull(DBReader.getFeedMedia(secondMediaId).getLastPlayedTimeHistory());

        activityTestRule.launchActivity(new Intent());

        SessionToken sessionToken = new SessionToken(context,
                new ComponentName(context, Media3PlaybackService.class));
        MediaController mediaController = new MediaController.Builder(context, sessionToken)
                .buildAsync().get(5, TimeUnit.SECONDS);

        final AtomicBoolean episodeBReady = new AtomicBoolean(false);
        final AtomicBoolean episodeCReady = new AtomicBoolean(false);

        try {
            mediaController.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (mediaController.getCurrentMediaItem() != null) {
                        String currentMediaId = mediaController.getCurrentMediaItem().mediaId;
                        if (String.valueOf(secondMediaId).equals(currentMediaId) && playbackState == Player.STATE_READY) {
                            episodeBReady.set(true);
                        } else if (String.valueOf(thirdMediaId).equals(currentMediaId) && playbackState == Player.STATE_READY) {
                            episodeCReady.set(true);
                        }
                    }
                }
            });

            clickBottomNavItem(R.string.queue_label);
            playFromQueue(1);

            Awaitility.await().atMost(10, TimeUnit.SECONDS).until(episodeBReady::get);
            assertNull(DBReader.getFeedMedia(secondMediaId).getLastPlayedTimeHistory());

            playFromQueue(2);

            Awaitility.await().atMost(10, TimeUnit.SECONDS).until(episodeCReady::get);

            assertNull("Episode B should not be in history after switching away when it never played",
                    DBReader.getFeedMedia(secondMediaId).getLastPlayedTimeHistory());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(mediaController::release);
        }
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
        context.sendBroadcast(MediaButtonStarter.createIntent(context, KeyEvent.KEYCODE_MEDIA_NEXT));
    }

    protected void pauseEpisode() {
        context.sendBroadcast(MediaButtonStarter.createIntent(context, KeyEvent.KEYCODE_MEDIA_PAUSE));
    }

    protected void startLocalPlayback() {
        clickBottomNavOverflow(R.string.episodes_label);

        final List<FeedItem> episodes = DBReader.getEpisodes(0, 10,
                FeedItemFilter.unfiltered(), SortOrder.DATE_NEW_OLD);
        Matcher<View> allEpisodesMatcher = allOf(withId(R.id.recyclerView), isDisplayed(), hasMinimumChildCount(2));
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
        final List<FeedItem> episodes = DBReader.getEpisodes(0, 10,
                FeedItemFilter.unfiltered(), SortOrder.DATE_NEW_OLD);

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
