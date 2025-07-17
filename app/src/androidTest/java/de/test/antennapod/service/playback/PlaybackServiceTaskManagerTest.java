package de.test.antennapod.service.playback;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;

import de.danoeh.antennapod.playback.service.internal.PlaybackServiceTaskManager;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.ui.widget.WidgetUpdater;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playback.Playable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for PlaybackServiceTaskManager
 */
@LargeTest
public class PlaybackServiceTaskManagerTest {

    @After
    public void tearDown() {
        PodDBAdapter.deleteDatabase();
    }

    @Before
    public void setUp() {
        // create new database
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();
        SleepTimerPreferences.setShakeToReset(false);
        SleepTimerPreferences.setVibrate(false);
    }

    @Test
    public void testInit() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(context, defaultPSTM);
        pstm.shutdown();
    }

    private List<FeedItem> writeTestQueue(String pref) {
        final int NUM_ITEMS = 10;
        Feed f = new Feed(0, null, "title", "link", "d", null, null, null, null, "id",
                null, "null", "url", System.currentTimeMillis());
        f.setItems(new ArrayList<>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            f.getItems().add(new FeedItem(0, pref + i, pref + i, "link", new Date(), FeedItem.PLAYED, f));
        }
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(f);
        adapter.setQueue(f.getItems());
        adapter.close();

        for (FeedItem item : f.getItems()) {
            assertTrue(item.getId() != 0);
        }
        return f.getItems();
    }

    @Test
    public void testStartPositionSaver() throws InterruptedException {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final int NUM_COUNTDOWNS = 2;
        final int TIMEOUT = 3 * PlaybackServiceTaskManager.POSITION_SAVER_WAITING_INTERVAL;
        final CountDownLatch countDownLatch = new CountDownLatch(NUM_COUNTDOWNS);
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, new PlaybackServiceTaskManager.PSTMCallback() {
            @Override
            public void positionSaverTick() {
                countDownLatch.countDown();
            }

            @Override
            public WidgetUpdater.WidgetState requestWidgetState() {
                return null;
            }

            @Override
            public void onChapterLoaded(Playable media) {

            }
        });
        pstm.startPositionSaver();
        countDownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        pstm.shutdown();
    }

    @Test
    public void testIsPositionSaverActive() {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.startPositionSaver();
        assertTrue(pstm.isPositionSaverActive());
        pstm.shutdown();
    }

    @Test
    public void testCancelPositionSaver() {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.startPositionSaver();
        pstm.cancelPositionSaver();
        assertFalse(pstm.isPositionSaverActive());
        pstm.shutdown();
    }

    @Test
    public void testStartWidgetUpdater() throws InterruptedException {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final int NUM_COUNTDOWNS = 2;
        final int TIMEOUT = 3 * PlaybackServiceTaskManager.WIDGET_UPDATER_NOTIFICATION_INTERVAL;
        final CountDownLatch countDownLatch = new CountDownLatch(NUM_COUNTDOWNS);
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, new PlaybackServiceTaskManager.PSTMCallback() {
            @Override
            public void positionSaverTick() {

            }

            @Override
            public WidgetUpdater.WidgetState requestWidgetState() {
                countDownLatch.countDown();
                return null;
            }

            @Override
            public void onChapterLoaded(Playable media) {

            }
        });
        pstm.startWidgetUpdater();
        countDownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        pstm.shutdown();
    }

    @Test
    public void testStartWidgetUpdaterAfterShutdown() {
        // Should not throw.
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.shutdown();
        pstm.startWidgetUpdater();
    }

    @Test
    public void testIsWidgetUpdaterActive() {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.startWidgetUpdater();
        assertTrue(pstm.isWidgetUpdaterActive());
        pstm.shutdown();
    }

    @Test
    public void testCancelWidgetUpdater() {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.startWidgetUpdater();
        pstm.cancelWidgetUpdater();
        assertFalse(pstm.isWidgetUpdaterActive());
        pstm.shutdown();
    }

    @Test
    public void testCancelAllTasksNoTasksStarted() {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.cancelAllTasks();
        assertFalse(pstm.isPositionSaverActive());
        assertFalse(pstm.isWidgetUpdaterActive());
        pstm.shutdown();
    }

    @Test
    @UiThreadTest
    public void testCancelAllTasksAllTasksStarted() {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.startWidgetUpdater();
        pstm.startPositionSaver();
        pstm.cancelAllTasks();
        assertFalse(pstm.isPositionSaverActive());
        assertFalse(pstm.isWidgetUpdaterActive());
        pstm.shutdown();
    }

    private final PlaybackServiceTaskManager.PSTMCallback defaultPSTM = new PlaybackServiceTaskManager.PSTMCallback() {
        @Override
        public void positionSaverTick() {

        }

        @Override
        public WidgetUpdater.WidgetState requestWidgetState() {
            return null;
        }

        @Override
        public void onChapterLoaded(Playable media) {

        }
    };
}
