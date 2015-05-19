package de.test.antennapod.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Condition;
import com.robotium.solo.Solo;
import com.robotium.solo.Timeout;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

/**
 * Test cases for starting and ending playback from the MainActivity and AudioPlayerActivity
 */
public class PlaybackTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;
    private UITestUtils uiTestUtils;

    private Context context;

    public PlaybackTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
        context = getInstrumentation().getContext();

        uiTestUtils = new UITestUtils(context);
        uiTestUtils.setup();

        // create database
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.close();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putBoolean(UserPreferences.PREF_UNPAUSE_ON_HEADSET_RECONNECT, false)
                .putBoolean(UserPreferences.PREF_PAUSE_ON_HEADSET_DISCONNECT, false)
                .putString(UserPreferences.PREF_HIDDEN_DRAWER_ITEMS, "")
                .commit();
    }

    @Override
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
        solo.finishOpenedActivities();
        PodDBAdapter.deleteDatabase(context);

        // shut down playback service
        skipEpisode();
        context.sendBroadcast(new Intent(PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));

        super.tearDown();
    }

    private void openNavDrawer() {
        solo.clickOnScreen(50, 50);
    }

    private void setContinuousPlaybackPreference(boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(UserPreferences.PREF_FOLLOW_QUEUE, value).commit();
    }

    private void skipEpisode() {
        Intent skipIntent = new Intent(PlaybackService.ACTION_SKIP_CURRENT_EPISODE);
        context.sendBroadcast(skipIntent);
    }

    private void startLocalPlayback() {
        openNavDrawer();

        solo.clickOnText(solo.getString(R.string.all_episodes_label));
        final List<FeedItem> episodes = DBReader.getRecentlyPublishedEpisodes(context, 10);
        assertTrue(solo.waitForView(solo.getView(R.id.butSecondaryAction)));

        solo.clickOnView(solo.getView(R.id.butSecondaryAction));
        assertTrue(solo.waitForView(solo.getView(R.id.butPlay)));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return episodes.get(0).getMedia().isCurrentlyPlaying();
            }
        }, Timeout.getLargeTimeout());
    }

    private void startLocalPlaybackFromQueue() {
        assertTrue(solo.waitForView(solo.getView(R.id.butSecondaryAction)));
        final List<FeedItem> queue = DBReader.getQueue(context);
        solo.clickOnImageButton(1);
        assertTrue(solo.waitForView(solo.getView(R.id.butPlay)));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return queue.get(0).getMedia().isCurrentlyPlaying();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testStartLocal() throws Exception {
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue(context).get();
        startLocalPlayback();
    }

    public void testContinousPlaybackOffSingleEpisode() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue(context).get();
        startLocalPlayback();
    }


    public void testContinousPlaybackOffMultipleEpisodes() throws Exception {
        setContinuousPlaybackPreference(false);
        uiTestUtils.addLocalFeedData(true);
        List<FeedItem> queue = DBReader.getQueue(context);
        final FeedItem first = queue.get(0);
        final FeedItem second = queue.get(1);

        startLocalPlaybackFromQueue();
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return first.getMedia().isCurrentlyPlaying() == false;
            }
        }, 10000);
        Thread.sleep(1000);
        assertTrue(second.getMedia().isCurrentlyPlaying() == false);
    }

    public void testContinuousPlaybackOnMultipleEpisodes() throws Exception {
        setContinuousPlaybackPreference(true);
        uiTestUtils.addLocalFeedData(true);
        List<FeedItem> queue = DBReader.getQueue(context);
        final FeedItem first = queue.get(0);
        final FeedItem second = queue.get(1);

        startLocalPlaybackFromQueue();
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return first.getMedia().isCurrentlyPlaying() == false;
            }
        }, 10000);
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return second.getMedia().isCurrentlyPlaying() == true;
            }
        }, 10000);
    }

    /**
     * Check if an episode can be played twice without problems.
     */
    private void replayEpisodeCheck(boolean followQueue) throws Exception {
        setContinuousPlaybackPreference(followQueue);
        uiTestUtils.addLocalFeedData(true);
        DBWriter.clearQueue(context).get();
        final List<FeedItem> episodes = DBReader.getRecentlyPublishedEpisodes(context, 10);

        startLocalPlayback();
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return false == episodes.get(0).getMedia().isCurrentlyPlaying();
            }
        }, Timeout.getLargeTimeout());

        startLocalPlayback();
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return false == episodes.get(0).getMedia().isCurrentlyPlaying();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testReplayEpisodeContinuousPlaybackOn() throws Exception {
        replayEpisodeCheck(true);
    }

    public void testReplayEpisodeContinuousPlaybackOff() throws Exception {
        replayEpisodeCheck(false);
    }


}
