package de.test.antennapod.playback;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.test.antennapod.EspressoTestUtils;
import de.test.antennapod.ui.UITestUtils;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.onDrawerItem;
import static de.test.antennapod.EspressoTestUtils.openNavDrawer;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static de.test.antennapod.NthMatcher.first;
import static de.test.antennapod.NthMatcher.nth;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

/**
 * test cases for starting and ending playback from the MainActivity and AudioPlayerActivity
 */
public abstract class PlaybackTest {

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, false, false);

    private UITestUtils uiTestUtils;
    protected Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.clearDatabase();
        EspressoTestUtils.makeNotFirstRun();

        uiTestUtils = new UITestUtils(context);
        uiTestUtils.setup();
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();

        // shut down playback service
        context.sendBroadcast(new Intent(PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
        Awaitility.await().until(() -> !PlaybackService.isRunning);
    }

    @Test
    public void testContinousPlaybackOffMultipleEpisodes() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addLocalFeedData(true);
        activityTestRule.launchActivity(new Intent());
        List<FeedItem> queue = DBReader.getQueue();
        final FeedItem first = queue.get(0);
        playFromQueue(0);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            if (uiTestUtils.getPlaybackController(getActivity()).getStatus()
                    != PlayerStatus.PLAYING) {
                return true;
            } else if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId()
                        != first.getMedia().getId();
            } else {
                return true;
            }
        });

        Thread.sleep(1000);
        assertNotEquals(PlayerStatus.PLAYING, uiTestUtils.getPlaybackController(getActivity()).getStatus());
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
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
            if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId()
                        == first.getMedia().getId();
            } else {
                return false;
            }
        });
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId()
                        == second.getMedia().getId();
            } else {
                return false;
            }
        });
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

        final int fiIdx = 0;
        final FeedItem feedItem = DBReader.getQueue().get(fiIdx);

        playFromQueue(fiIdx);

        // let playback run a bit then pause
        Awaitility.await()
                .atMost(1000, MILLISECONDS)
                .until(() -> PlayerStatus.PLAYING == uiTestUtils.getPlaybackController(getActivity()).getStatus());
        pauseEpisode();
        Awaitility.await()
                .atMost(1000, MILLISECONDS)
                .until(() -> PlayerStatus.PAUSED == uiTestUtils.getPlaybackController(getActivity()).getStatus());

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
        Intent skipIntent = new Intent(PlaybackService.ACTION_SKIP_CURRENT_EPISODE);
        context.sendBroadcast(skipIntent);
    }

    protected void pauseEpisode() {
        Intent pauseIntent = new Intent(PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE);
        context.sendBroadcast(pauseIntent);
    }

    protected void startLocalPlayback() {
        openNavDrawer();
        onDrawerItem(withText(R.string.episodes_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(R.id.emptyViewTitle), 1000));
        onView(withText(R.string.all_episodes_short_label)).perform(click());

        final List<FeedItem> episodes = DBReader.getRecentlyPublishedEpisodes(0, 10);
        onView(isRoot()).perform(waitForView(withId(R.id.butSecondaryAction), 1000));

        onView(first(withId(R.id.butSecondaryAction))).perform(click());
        long mediaId = episodes.get(0).getMedia().getId();
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> {
            if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId() == mediaId;
            } else {
                return false;
            }
        });
    }

    /**
     *
     * @param itemIdx The 0-based index of the episode to be played in the queue.
     */
    protected void playFromQueue(int itemIdx) {
        final List<FeedItem> queue = DBReader.getQueue();

        onView(nth(withId(R.id.butSecondaryAction), itemIdx + 1)).perform(click());
        onView(isRoot()).perform(waitForView(withId(R.id.butPlay), 1000));
        long mediaId = queue.get(itemIdx).getMedia().getId();
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> {
            if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId() == mediaId;
            } else {
                return false;
            }
        });
    }

    /**
     * Check if an episode can be played twice without problems.
     */
    protected void replayEpisodeCheck(boolean followQueue) throws Exception {
        setContinuousPlaybackPreference(followQueue);
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue().get();
        activityTestRule.launchActivity(new Intent());
        final List<FeedItem> episodes = DBReader.getRecentlyPublishedEpisodes(0, 10);

        startLocalPlayback();
        long mediaId = episodes.get(0).getMedia().getId();
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> {
            if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId() == mediaId;
            } else {
                return false;
            }
        });

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() ->
                uiTestUtils.getCurrentMedia(getActivity()) == null
                || uiTestUtils.getCurrentMedia(getActivity()).getId() != mediaId);

        startLocalPlayback();
        Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> {
            if (uiTestUtils.getCurrentMedia(getActivity()) != null) {
                return uiTestUtils.getCurrentMedia(getActivity()).getId() == mediaId;
            } else {
                return false;
            }
        });
    }

    protected void doTestSmartMarkAsPlayed_Skip_ForEpisode(int itemIdxNegAllowed) throws Exception {
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

        activityTestRule.launchActivity(new Intent());
        playFromQueue(fiIdx);

        skipEpisode();

        //  assert item no longer in queue (needs to wait till skip is asynchronously processed)
        Awaitility.await()
                .atMost(1000, MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat("Ensure smart mark as play will lead to the item removed from the queue",
                            DBReader.getQueue(), not(hasItems(feedItem)));
                });
        assertThat(DBReader.getFeedItem(feedItem.getId()).isPlayed(), is(true));
    }
}
