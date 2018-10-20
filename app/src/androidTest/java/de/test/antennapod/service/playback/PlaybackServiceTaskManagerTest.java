package de.test.antennapod.service.playback;

import android.content.Context;
import android.test.InstrumentationTestCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.event.QueueEvent;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.service.playback.PlaybackServiceTaskManager;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.greenrobot.event.EventBus;

/**
 * Test class for PlaybackServiceTaskManager
 */
public class PlaybackServiceTaskManagerTest extends InstrumentationTestCase {

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        PodDBAdapter.deleteDatabase();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create new database
        PodDBAdapter.init(getInstrumentation().getTargetContext());
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();
    }

    public void testInit() {
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(getInstrumentation().getTargetContext(), defaultPSTM);
        pstm.shutdown();
    }

    private List<FeedItem> writeTestQueue(String pref) {
        final Context c = getInstrumentation().getTargetContext();
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

    public void testGetQueueWriteBeforeCreation() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
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

    public void testGetQueueWriteAfterCreation() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();

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

    public void testStartPositionSaver() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
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

    public void testIsPositionSaverActive() {
        final Context c = getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.startPositionSaver();
        assertTrue(pstm.isPositionSaverActive());
        pstm.shutdown();
    }

    public void testCancelPositionSaver() {
        final Context c = getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.startPositionSaver();
        pstm.cancelPositionSaver();
        assertFalse(pstm.isPositionSaverActive());
        pstm.shutdown();
    }

    public void testStartWidgetUpdater() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
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

    public void testIsWidgetUpdaterActive() {
        final Context c = getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.startWidgetUpdater();
        assertTrue(pstm.isWidgetUpdaterActive());
        pstm.shutdown();
    }

    public void testCancelWidgetUpdater() {
        final Context c = getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.startWidgetUpdater();
        pstm.cancelWidgetUpdater();
        assertFalse(pstm.isWidgetUpdaterActive());
        pstm.shutdown();
    }

    public void testCancelAllTasksNoTasksStarted() {
        final Context c = getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.cancelAllTasks();
        assertFalse(pstm.isPositionSaverActive());
        assertFalse(pstm.isWidgetUpdaterActive());
        assertFalse(pstm.isSleepTimerActive());
        pstm.shutdown();
    }

    public void testCancelAllTasksAllTasksStarted() {
        final Context c = getInstrumentation().getTargetContext();
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

    public void testSetSleepTimer() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
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

    public void testDisableSleepTimer() throws InterruptedException {
        final Context c = getInstrumentation().getTargetContext();
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

    public void testIsSleepTimerActivePositive() {
        final Context c = getInstrumentation().getTargetContext();
        PlaybackServiceTaskManager pstm = new PlaybackServiceTaskManager(c, defaultPSTM);
        pstm.setSleepTimer(10000, false, false);
        assertTrue(pstm.isSleepTimerActive());
        pstm.shutdown();
    }

    public void testIsSleepTimerActiveNegative() {
        final Context c = getInstrumentation().getTargetContext();
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
