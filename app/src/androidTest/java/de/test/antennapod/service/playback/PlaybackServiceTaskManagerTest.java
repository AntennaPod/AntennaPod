package de.test.antennapod.service.playback;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;

import org.greenrobot.eventbus.EventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.event.QueueEvent;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.playback.PlaybackServiceTaskManager;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.playback.Playable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    }

    @Test
    public void testInit() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(context, defaultPSTM);
        pstm.shutdown();
    }

    private List<FeedItem> writeTestQueue(String pref) {
        final int NUM_ITEMS = 10;
        Feed f = new Feed(0, null, "title", "link", "d", null, null, null, null, "id", null, "null", "url", false);
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
    public void testGetQueueWriteBeforeCreation() throws InterruptedException {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        List<FeedItem> queue = writeTestQueue("a");
        assertNotNull(queue);
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        List<FeedItem> testQueue = pstm.getQueue();
        assertNotNull(testQueue);
        assertTrue(queue.size() == testQueue.size());
        for (int i = 0; i < queue.size(); i++) {
            assertTrue(queue.get(i).getId() == testQueue.get(i).getId());
        }
        pstm.shutdown();
    }

    @Test
    public void testGetQueueWriteAfterCreation() throws InterruptedException {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();

        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        List<FeedItem> testQueue = pstm.getQueue();
        assertNotNull(testQueue);
        assertTrue(testQueue.isEmpty());


        final CountDownLatch countDownLatch = new CountDownLatch(1);
        EventDistributor.EventListener queueListener = new EventDistributor.EventListener() {
            @Override
            public void update(EventDistributor eventDistributor, Integer arg) {
                countDownLatch.countDown();
            }
        };
        EventDistributor.getInstance().register(queueListener);
        List<FeedItem> queue = writeTestQueue("a");
        EventBus.getDefault().post(QueueEvent.setQueue(queue));
        countDownLatch.await(5000, TimeUnit.MILLISECONDS);

        assertNotNull(queue);
        testQueue = pstm.getQueue();
        assertNotNull(testQueue);
        assertTrue(queue.size() == testQueue.size());
        for (int i = 0; i < queue.size(); i++) {
            assertTrue(queue.get(i).getId() == testQueue.get(i).getId());
        }
        pstm.shutdown();
    }

    @Test
    public void testQueueUpdatedUponDownloadComplete() throws Exception {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        { // Setup test data
            List<FeedItem> queue = writeTestQueue("a");
            FeedItem item = DBReader.getFeedItem(queue.get(0).getId());
            FeedMedia media = new FeedMedia(item, "http://abc.test/acme.mp3", 12345, "audio/mp3");
            item.setMedia(media);
            DBWriter.setFeedMedia(media).get();
            DBWriter.setFeedItem(item).get();
        }

        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        final FeedItem testItem = pstm.getQueue().get(0);
        assertThat("The item is not yet downloaded",
                testItem.getMedia().isDownloaded(), is(false));

        { // simulate download complete (in DownloadService.MediaHandlerThread)
            FeedItem item = DBReader.getFeedItem(testItem.getId());
            item.getMedia().setDownloaded(true);
            item.getMedia().setFile_url("file://123");
            item.setAutoDownload(false);
            DBWriter.setFeedItem(item).get();
            DBWriter.setFeedMedia(item.getMedia()).get();
        }

        // an approximation to ensure the item update event has been posted and processed.
        Thread.sleep(10);

        final FeedItem itemUpdated = pstm.getQueue().get(0);
        assertThat("The queue in PlaybackService has been updated item after download is completed",
                itemUpdated.getMedia().isDownloaded(), is(true));

        pstm.shutdown();
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
            public void onSleepTimerAlmostExpired() {

            }

            @Override
            public void onSleepTimerExpired() {

            }

            @Override
            public void onSleepTimerReset() {

            }

            @Override
            public void onWidgetUpdaterTick() {

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
            public void onSleepTimerAlmostExpired() {

            }

            @Override
            public void onSleepTimerExpired() {

            }

            @Override
            public void onSleepTimerReset() {

            }

            @Override
            public void onWidgetUpdaterTick() {
                countDownLatch.countDown();
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
        assertFalse(pstm.isSleepTimerActive());
        pstm.shutdown();
    }

    @Test
    @UiThreadTest
    public void testCancelAllTasksAllTasksStarted() {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.startWidgetUpdater();
        pstm.startPositionSaver();
        pstm.setSleepTimer(100000, false, false);
        pstm.cancelAllTasks();
        assertFalse(pstm.isPositionSaverActive());
        assertFalse(pstm.isWidgetUpdaterActive());
        assertFalse(pstm.isSleepTimerActive());
        pstm.shutdown();
    }

    @Test
    @UiThreadTest
    public void testSetSleepTimer() throws InterruptedException {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final long TIME = 2000;
        final long TIMEOUT = 2 * TIME;
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, new PlaybackServiceTaskManager.PSTMCallback() {
            @Override
            public void positionSaverTick() {

            }

            @Override
            public void onSleepTimerAlmostExpired() {

            }

            @Override
            public void onSleepTimerExpired() {
                if (countDownLatch.getCount() == 0) {
                    fail();
                }
                countDownLatch.countDown();
            }

            @Override
            public void onSleepTimerReset() {

            }

            @Override
            public void onWidgetUpdaterTick() {

            }

            @Override
            public void onChapterLoaded(Playable media) {

            }
        });
        pstm.setSleepTimer(TIME, false, false);
        countDownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        pstm.shutdown();
    }

    @Test
    @UiThreadTest
    public void testDisableSleepTimer() throws InterruptedException {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final long TIME = 1000;
        final long TIMEOUT = 2 * TIME;
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, new PlaybackServiceTaskManager.PSTMCallback() {
            @Override
            public void positionSaverTick() {

            }

            @Override
            public void onSleepTimerAlmostExpired() {

            }

            @Override
            public void onSleepTimerExpired() {
                fail("Sleeptimer expired");
            }

            @Override
            public void onSleepTimerReset() {

            }

            @Override
            public void onWidgetUpdaterTick() {

            }

            @Override
            public void onChapterLoaded(Playable media) {

            }
        });
        pstm.setSleepTimer(TIME, false, false);
        pstm.disableSleepTimer();
        assertFalse(countDownLatch.await(TIMEOUT, TimeUnit.MILLISECONDS));
        pstm.shutdown();
    }

    @Test
    @UiThreadTest
    public void testIsSleepTimerActivePositive() {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.setSleepTimer(10000, false, false);
        assertTrue(pstm.isSleepTimerActive());
        pstm.shutdown();
    }

    @Test
    @UiThreadTest
    public void testIsSleepTimerActiveNegative() {
        final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.setSleepTimer(10000, false, false);
        pstm.disableSleepTimer();
        assertFalse(pstm.isSleepTimerActive());
        pstm.shutdown();
    }

    private final PlaybackServiceTaskManager.PSTMCallback defaultPSTM = new PlaybackServiceTaskManager.PSTMCallback() {
        @Override
        public void positionSaverTick() {

        }

        @Override
        public void onSleepTimerAlmostExpired() {

        }

        @Override
        public void onSleepTimerExpired() {

        }

        @Override
        public void onSleepTimerReset() {

        }

        @Override
        public void onWidgetUpdaterTick() {

        }

        @Override
        public void onChapterLoaded(Playable media) {

        }
    };
}
