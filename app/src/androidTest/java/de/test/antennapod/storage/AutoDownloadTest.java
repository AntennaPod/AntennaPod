package de.test.antennapod.storage;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import de.danoeh.antennapodSA.core.ClientConfig;
import de.danoeh.antennapodSA.core.DBTasksCallbacks;
import de.danoeh.antennapodSA.core.feed.FeedItem;
import de.danoeh.antennapodSA.core.feed.FeedMedia;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.core.service.playback.PlaybackService;
import de.danoeh.antennapodSA.core.storage.AutomaticDownloadAlgorithm;
import de.danoeh.antennapodSA.core.storage.DBReader;
import de.danoeh.antennapodSA.core.storage.DBTasks;
import de.danoeh.antennapodSA.core.storage.EpisodeCleanupAlgorithm;
import de.danoeh.antennapodSA.core.storage.PodDBAdapter;
import de.danoeh.antennapodSA.core.util.playback.Playable;
import de.test.antennapod.ui.UITestUtils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AutoDownloadTest {

    private Context context;
    private UITestUtils stubFeedsServer;

    private DBTasksCallbacks dbTasksCallbacksOrig;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();

        stubFeedsServer = new UITestUtils(context);
        stubFeedsServer.setup();

        dbTasksCallbacksOrig = ClientConfig.dbTasksCallbacks;

        // create new database
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();
    }

    @After
    public void tearDown() throws Exception {
        stubFeedsServer.tearDown();
        ClientConfig.dbTasksCallbacks = dbTasksCallbacksOrig;

        context.sendBroadcast(new Intent(PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE));
        Awaitility.await().until(() -> !PlaybackService.isRunning);
    }

    /**
     * A cross-functional test, ensuring playback's behavior works with Auto Download in boundary condition.
     *
     * Scenario:
     * - For setting enqueue location AFTER_CURRENTLY_PLAYING
     * - when playback of an episode is complete and the app advances to the next episode (continuous playback on)
     * - when automatic download kicks in,
     * - ensure the next episode is the current playing one, needed for AFTER_CURRENTLY_PLAYING enqueue location.
     */
    @Test
    public void downloadsEnqueuedToAfterCurrent_CurrentAdvancedToNextOnPlaybackComplete() throws Exception {
        UserPreferences.setFollowQueue(true); // continuous playback

        // Setup: feeds and queue
        // downloads 3 of them, leave some in new state (auto-downloadable)
        stubFeedsServer.addLocalFeedData(false);
        List<FeedItem> queue = DBReader.getQueue();
        assertTrue(queue.size() > 1);
        FeedItem item0 = queue.get(0);
        FeedItem item1 = queue.get(1);

        // Setup: enable automatic download
        // it is not needed, as the actual automatic download is stubbed.
        StubDownloadAlgorithm stubDownloadAlgorithm = new StubDownloadAlgorithm();
        useDownloadAlgorithm(stubDownloadAlgorithm);

        // Actual test
        // Play the first one in the queue
        playEpisode(item0);

        try {
            // when playback is complete, advances to the next one, and auto download kicks in,
            // ensure that currently playing has been advanced to the next one by this point.
            Awaitility.await("advanced to the next episode")
                    .atMost(6000, MILLISECONDS) // the test mp3 media is 3-second long. twice should be enough
                    .until(() -> item1.equals(stubDownloadAlgorithm.getCurrentlyPlayingAtDownload()));
        } catch (ConditionTimeoutException cte) {
            FeedItem actual = stubDownloadAlgorithm.getCurrentlyPlayingAtDownload();
            fail("when auto download is triggered, the next episode should be playing: ("
                    + item1.getId() + ", "  + item1.getTitle() + ") . "
                    + "Actual playing: ("
                    + (actual == null ? "" : actual.getId() + ", " + actual.getTitle()) + ")"
            );
        }

    }

    private void playEpisode(@NonNull FeedItem item) {
        FeedMedia media = item.getMedia();
        DBTasks.playMedia(context, media, false, true, true);
        Awaitility.await("episode is playing")
                .atMost(1000, MILLISECONDS)
                .until(() -> item.equals(getCurrentlyPlaying()));
    }

    private FeedItem getCurrentlyPlaying() {
        Playable playable = Playable.PlayableUtils.createInstanceFromPreferences(context);
        if (playable == null) {
            return null;
        }
        return ((FeedMedia)playable).getItem();
    }

    private void useDownloadAlgorithm(final AutomaticDownloadAlgorithm downloadAlgorithm) {
        ClientConfig.dbTasksCallbacks = new DBTasksCallbacks() {
            @Override
            public AutomaticDownloadAlgorithm getAutomaticDownloadAlgorithm() {
                return downloadAlgorithm;
            }

            @Override
            public EpisodeCleanupAlgorithm getEpisodeCacheCleanupAlgorithm() {
                return dbTasksCallbacksOrig.getEpisodeCacheCleanupAlgorithm();
            }
        };
    }

    private class StubDownloadAlgorithm implements AutomaticDownloadAlgorithm {
        @Nullable
        private FeedItem currentlyPlaying;

        @Override
        public Runnable autoDownloadUndownloadedItems(Context context) {
            return () -> {
                if (currentlyPlaying == null) {
                    currentlyPlaying = getCurrentlyPlaying();
                } else {
                    throw new AssertionError("Stub automatic download should be invoked once and only once");
                }
            };
        }

        @Nullable
        FeedItem getCurrentlyPlayingAtDownload() {
            return currentlyPlaying;
        }
    }
}
