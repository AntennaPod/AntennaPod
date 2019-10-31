package de.test.antennapod.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.Consumer;
import de.danoeh.antennapod.core.util.FeedItemUtil;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for DBWriter
 */
@MediumTest
public class DBWriterTest {

    private static final String TAG = "DBWriterTest";
    private static final String TEST_FOLDER = "testDBWriter";
    private static final long TIMEOUT = 5L;

    @After
    public void tearDown() throws Exception {
        assertTrue(PodDBAdapter.deleteDatabase());

        final Context context = InstrumentationRegistry.getTargetContext();
        File testDir = context.getExternalFilesDir(TEST_FOLDER);
        assertNotNull(testDir);
        for (File f : testDir.listFiles()) {
            f.delete();
        }
    }

    @Before
    public void setUp() throws Exception {
        // create new database
        PodDBAdapter.init(InstrumentationRegistry.getTargetContext());
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

        Context context = InstrumentationRegistry.getTargetContext();
        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit();
        prefEdit.putBoolean(UserPreferences.PREF_DELETE_REMOVES_FROM_QUEUE, true).commit();

        UserPreferences.init(context);
    }

    @Test
    public void testSetFeedMediaPlaybackInformation()
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        final int POSITION = 50;
        final long LAST_PLAYED_TIME = 1000;
        final int PLAYED_DURATION = 60;
        final int DURATION = 100;

        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        FeedItem item = new FeedItem(0, "Item", "Item", "url", new Date(), FeedItem.PLAYED, feed);
        items.add(item);
        FeedMedia media = new FeedMedia(0, item, DURATION, 1, 1, "mime_type", "dummy path", "download_url", true, null, 0, 0);
        item.setMedia(media);

        DBWriter.setFeedItem(item).get(TIMEOUT, TimeUnit.SECONDS);

        media.setPosition(POSITION);
        media.setLastPlayedTime(LAST_PLAYED_TIME);
        media.setPlayedDuration(PLAYED_DURATION);

        DBWriter.setFeedMediaPlaybackInformation(item.getMedia()).get(TIMEOUT, TimeUnit.SECONDS);

        FeedItem itemFromDb = DBReader.getFeedItem(item.getId());
        FeedMedia mediaFromDb = itemFromDb.getMedia();

        assertEquals(POSITION, mediaFromDb.getPosition());
        assertEquals(LAST_PLAYED_TIME, mediaFromDb.getLastPlayedTime());
        assertEquals(PLAYED_DURATION, mediaFromDb.getPlayedDuration());
        assertEquals(DURATION, mediaFromDb.getDuration());
    }

    @Test
    public void testDeleteFeedMediaOfItemFileExists()
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        File dest = new File(getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER), "testFile");

        assertTrue(dest.createNewFile());

        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        FeedItem item = new FeedItem(0, "Item", "Item", "url", new Date(), FeedItem.PLAYED, feed);

        FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", dest.getAbsolutePath(), "download_url", true, null, 0, 0);
        item.setMedia(media);

        items.add(item);

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();
        assertTrue(media.getId() != 0);
        assertTrue(item.getId() != 0);

        DBWriter.deleteFeedMediaOfItem(getInstrumentation().getTargetContext(), media.getId())
                .get(TIMEOUT, TimeUnit.SECONDS);
        media = DBReader.getFeedMedia(media.getId());
        assertNotNull(media);
        assertFalse(dest.exists());
        assertFalse(media.isDownloaded());
        assertNull(media.getFile_url());
    }

    @Test
    public void testDeleteFeedMediaOfItemRemoveFromQueue()
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        assertTrue(UserPreferences.shouldDeleteRemoveFromQueue());

        File dest = new File(getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER), "testFile");

        assertTrue(dest.createNewFile());

        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        List<FeedItem> queue = new ArrayList<>();
        feed.setItems(items);
        FeedItem item = new FeedItem(0, "Item", "Item", "url", new Date(), FeedItem.UNPLAYED, feed);

        FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", dest.getAbsolutePath(), "download_url", true, null, 0, 0);
        item.setMedia(media);

        items.add(item);
        queue.add(item);

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.setQueue(queue);
        adapter.close();
        assertTrue(media.getId() != 0);
        assertTrue(item.getId() != 0);
        queue = DBReader.getQueue();
        assertTrue(queue.size() != 0);

        DBWriter.deleteFeedMediaOfItem(getInstrumentation().getTargetContext(), media.getId());
        Awaitility.await().until(() -> dest.exists() == false);
        media = DBReader.getFeedMedia(media.getId());
        assertNotNull(media);
        assertFalse(dest.exists());
        assertFalse(media.isDownloaded());
        assertNull(media.getFile_url());
        queue = DBReader.getQueue();
        assertTrue(queue.size() == 0);
    }

    @Test
    public void testDeleteFeed() throws ExecutionException, InterruptedException, IOException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());

        List<File> itemFiles = new ArrayList<>();
        // create items with downloaded media files
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), FeedItem.PLAYED, feed);
            feed.getItems().add(item);

            File enc = new File(destFolder, "file " + i);
            assertTrue(enc.createNewFile());

            itemFiles.add(enc);
            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", true, null, 0, 0);
            item.setMedia(media);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);

        // check if files still exist
        for (File f : itemFiles) {
            assertFalse(f.exists());
        }

        adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        for (FeedItem item : feed.getItems()) {
            c = adapter.getFeedItemCursor(String.valueOf(item.getId()));
            assertTrue(c.getCount() == 0);
            c.close();
            c = adapter.getSingleFeedMediaCursor(item.getMedia().getId());
            assertTrue(c.getCount() == 0);
            c.close();
        }
        adapter.close();
    }

    @Test
    public void testDeleteFeedNoItems() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", null, "title");
        feed.setItems(null);
        feed.setImageUrl("url");

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);

        adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        adapter.close();
    }

    @Test
    public void testDeleteFeedNoFeedMedia() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());

        feed.setImageUrl("url");

        // create items
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), FeedItem.PLAYED, feed);
            feed.getItems().add(item);

        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);


        adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        for (FeedItem item : feed.getItems()) {
            c = adapter.getFeedItemCursor(String.valueOf(item.getId()));
            assertTrue(c.getCount() == 0);
            c.close();
        }
        adapter.close();
    }

    @Test
    public void testDeleteFeedWithQueueItems() throws ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());

        feed.setImageUrl("url");

        List<File> itemFiles = new ArrayList<>();
        // create items with downloaded media files
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), FeedItem.PLAYED, feed);
            feed.getItems().add(item);

            File enc = new File(destFolder, "file " + i);
            itemFiles.add(enc);

            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", false, null, 0, 0);
            item.setMedia(media);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }


        List<FeedItem> queue = new ArrayList<>();
        queue.addAll(feed.getItems());
        adapter.open();
        adapter.setQueue(queue);

        Cursor queueCursor = adapter.getQueueIDCursor();
        assertTrue(queueCursor.getCount() == queue.size());
        queueCursor.close();

        adapter.close();
        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);
        adapter.open();

        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        for (FeedItem item : feed.getItems()) {
            c = adapter.getFeedItemCursor(String.valueOf(item.getId()));
            assertTrue(c.getCount() == 0);
            c.close();
            c = adapter.getSingleFeedMediaCursor(item.getMedia().getId());
            assertTrue(c.getCount() == 0);
            c.close();
        }
        c = adapter.getQueueCursor();
        assertTrue(c.getCount() == 0);
        c.close();
        adapter.close();
    }

    @Test
    public void testDeleteFeedNoDownloadedFiles() throws ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());

        feed.setImageUrl("url");

        List<File> itemFiles = new ArrayList<>();
        // create items with downloaded media files
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), FeedItem.PLAYED, feed);
            feed.getItems().add(item);

            File enc = new File(destFolder, "file " + i);
            itemFiles.add(enc);

            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", false, null, 0, 0);
            item.setMedia(media);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);

        adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        for (FeedItem item : feed.getItems()) {
            c = adapter.getFeedItemCursor(String.valueOf(item.getId()));
            assertTrue(c.getCount() == 0);
            c.close();
            c = adapter.getSingleFeedMediaCursor(item.getMedia().getId());
            assertTrue(c.getCount() == 0);
            c.close();
        }
        adapter.close();
    }

    private FeedMedia playbackHistorySetup(Date playbackCompletionDate) {
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        FeedItem item = new FeedItem(0, "title", "id", "link", new Date(), FeedItem.PLAYED, feed);
        FeedMedia media = new FeedMedia(0, item, 10, 0, 1, "mime", null, "url", false, playbackCompletionDate, 0, 0);
        feed.getItems().add(item);
        item.setMedia(media);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();
        assertTrue(media.getId() != 0);
        return media;
    }

    @Test
    public void testAddItemToPlaybackHistoryNotPlayedYet()
            throws ExecutionException, InterruptedException, TimeoutException {
        FeedMedia media = playbackHistorySetup(null);
        DBWriter.addItemToPlaybackHistory(media).get(TIMEOUT, TimeUnit.SECONDS);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        media = DBReader.getFeedMedia(media.getId());
        adapter.close();

        assertNotNull(media);
        assertNotNull(media.getPlaybackCompletionDate());
    }

    @Test
    public void testAddItemToPlaybackHistoryAlreadyPlayed()
            throws ExecutionException, InterruptedException, TimeoutException {
        final long OLD_DATE = 0;

        FeedMedia media = playbackHistorySetup(new Date(OLD_DATE));
        DBWriter.addItemToPlaybackHistory(media).get(TIMEOUT, TimeUnit.SECONDS);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        media = DBReader.getFeedMedia(media.getId());
        adapter.close();

        assertNotNull(media);
        assertNotNull(media.getPlaybackCompletionDate());
        assertFalse(OLD_DATE == media.getPlaybackCompletionDate().getTime());
    }

    private Feed queueTestSetupMultipleItems(final int NUM_ITEMS) throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        UserPreferences.setEnqueueLocation(UserPreferences.EnqueueLocation.BACK);
        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title " + i, "id " + i, "link " + i, new Date(), FeedItem.PLAYED, feed);
            feed.getItems().add(item);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }
        List<Future<?>> futures = new ArrayList<>();
        for (FeedItem item : feed.getItems()) {
            futures.add(DBWriter.addQueueItem(context, item));
        }
        for (Future<?> f : futures) {
            f.get(TIMEOUT, TimeUnit.SECONDS);
        }
        return feed;
    }

    @Test
    public void testAddQueueItemSingleItem() throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        FeedItem item = new FeedItem(0, "title", "id", "link", new Date(), FeedItem.PLAYED, feed);
        feed.getItems().add(item);

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(item.getId() != 0);
        DBWriter.addQueueItem(context, item).get(TIMEOUT, TimeUnit.SECONDS);

        adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor cursor = adapter.getQueueIDCursor();
        assertTrue(cursor.moveToFirst());
        assertTrue(cursor.getLong(0) == item.getId());
        cursor.close();
        adapter.close();
    }

    @Test
    public void testAddQueueItemSingleItemAlreadyInQueue() throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        FeedItem item = new FeedItem(0, "title", "id", "link", new Date(), FeedItem.PLAYED, feed);
        feed.getItems().add(item);

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(item.getId() != 0);
        DBWriter.addQueueItem(context, item).get(TIMEOUT, TimeUnit.SECONDS);

        adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor cursor = adapter.getQueueIDCursor();
        assertTrue(cursor.moveToFirst());
        assertTrue(cursor.getLong(0) == item.getId());
        cursor.close();
        adapter.close();

        DBWriter.addQueueItem(context, item).get(TIMEOUT, TimeUnit.SECONDS);
        adapter = PodDBAdapter.getInstance();
        adapter.open();
        cursor = adapter.getQueueIDCursor();
        assertTrue(cursor.moveToFirst());
        assertTrue(cursor.getLong(0) == item.getId());
        assertTrue(cursor.getCount() == 1);
        cursor.close();
        adapter.close();
    }

    @Test
    public void testAddQueueItemMultipleItems() throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        final int NUM_ITEMS = 10;

        Feed feed = queueTestSetupMultipleItems(NUM_ITEMS);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor cursor = adapter.getQueueIDCursor();
        assertTrue(cursor.moveToFirst());
        assertTrue(cursor.getCount() == NUM_ITEMS);
        List<Long> expectedIds = FeedItemUtil.getIdList(feed.getItems());
        List<Long> actualIds = new ArrayList<>();
        for (int i = 0; i < NUM_ITEMS; i++) {
            assertTrue(cursor.moveToPosition(i));
            actualIds.add(cursor.getLong(0));
        }
        cursor.close();
        adapter.close();
        assertEquals("Bulk add to queue: result order should be the same as the order given",
                expectedIds, actualIds);
    }

    @Test
    public void testClearQueue() throws InterruptedException, ExecutionException, TimeoutException {
        final int NUM_ITEMS = 10;

        Feed feed = queueTestSetupMultipleItems(NUM_ITEMS);
        DBWriter.clearQueue().get(TIMEOUT, TimeUnit.SECONDS);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        Cursor cursor = adapter.getQueueIDCursor();
        assertFalse(cursor.moveToFirst());
        cursor.close();
        adapter.close();
    }

    @Test
    public void testRemoveQueueItem() throws InterruptedException, ExecutionException, TimeoutException {
        final int NUM_ITEMS = 10;
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = createTestFeed(NUM_ITEMS);

        for (int removeIndex = 0; removeIndex < NUM_ITEMS; removeIndex++) {
            final FeedItem item = feed.getItems().get(removeIndex);
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.setQueue(feed.getItems());
            adapter.close();

            DBWriter.removeQueueItem(context, false, item).get(TIMEOUT, TimeUnit.SECONDS);
            adapter = PodDBAdapter.getInstance();
            adapter.open();
            Cursor queue = adapter.getQueueIDCursor();
            assertTrue(queue.getCount() == NUM_ITEMS - 1);
            for (int i = 0; i < queue.getCount(); i++) {
                assertTrue(queue.moveToPosition(i));
                final long queueID = queue.getLong(0);
                assertTrue(queueID != item.getId());  // removed item is no longer in queue
                boolean idFound = false;
                for (FeedItem other : feed.getItems()) { // items that were not removed are still in the queue
                    idFound = idFound | (other.getId() == queueID);
                }
                assertTrue(idFound);
            }
            queue.close();
            adapter.close();
        }
    }

    @Test
    public void testRemoveQueueItemMultipleItems() throws InterruptedException, ExecutionException, TimeoutException {
        // Setup test data
        //
        final int NUM_ITEMS = 5;
        final int NUM_IN_QUEUE = NUM_ITEMS - 1; // the last one not in queue for boundary condition
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = createTestFeed(NUM_ITEMS);

        List<FeedItem> itemsToAdd = feed.getItems().subList(0, NUM_IN_QUEUE);
        withPodDB(adapter -> adapter.setQueue(itemsToAdd) );

        // Actual tests
        //

        // Use array rather than List to make codes more succinct
        Long[] itemIds = toItemIds(feed.getItems()).toArray(new Long[0]);

        DBWriter.removeQueueItem(context, false,
                itemIds[1], itemIds[3]).get(TIMEOUT, TimeUnit.SECONDS);
        assertQueueByItemIds("Average case - 2 items removed successfully",
                itemIds[0], itemIds[2]);

        DBWriter.removeQueueItem(context, false).get(TIMEOUT, TimeUnit.SECONDS);
        assertQueueByItemIds("Boundary case - no items supplied. queue should see no change",
                itemIds[0], itemIds[2]);

        DBWriter.removeQueueItem(context, false,
                itemIds[0], itemIds[4], -1L).get(TIMEOUT, TimeUnit.SECONDS);
        assertQueueByItemIds("Boundary case - items not in queue ignored",
                itemIds[2]);

        DBWriter.removeQueueItem(context, false,
                itemIds[2], -1L).get(TIMEOUT, TimeUnit.SECONDS);
        assertQueueByItemIds("Boundary case - invalid itemIds ignored"); // the queue is empty

    }

    @Test
    public void testMoveQueueItem() throws InterruptedException, ExecutionException, TimeoutException {
        final int NUM_ITEMS = 10;
        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title " + i, "id " + i, "link " + i, new Date(), FeedItem.PLAYED, feed);
            feed.getItems().add(item);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }
        for (int from = 0; from < NUM_ITEMS; from++) {
            for (int to = 0; to < NUM_ITEMS; to++) {
                if (from == to) {
                    continue;
                }
                Log.d(TAG, String.format("testMoveQueueItem: From=%d, To=%d", from, to));
                final long fromID = feed.getItems().get(from).getId();

                adapter = PodDBAdapter.getInstance();
                adapter.open();
                adapter.setQueue(feed.getItems());
                adapter.close();

                DBWriter.moveQueueItem(from, to, false).get(TIMEOUT, TimeUnit.SECONDS);
                adapter = PodDBAdapter.getInstance();
                adapter.open();
                Cursor queue = adapter.getQueueIDCursor();
                assertTrue(queue.getCount() == NUM_ITEMS);
                assertTrue(queue.moveToPosition(from));
                assertFalse(queue.getLong(0) == fromID);
                assertTrue(queue.moveToPosition(to));
                assertTrue(queue.getLong(0) == fromID);

                queue.close();
                adapter.close();
            }
        }
    }

    @Test
    public void testMarkFeedRead() throws InterruptedException, ExecutionException, TimeoutException {
        final int NUM_ITEMS = 10;
        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title " + i, "id " + i, "link " + i, new Date(), FeedItem.UNPLAYED, feed);
            feed.getItems().add(item);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }

        DBWriter.markFeedRead(feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);
        List<FeedItem> loadedItems = DBReader.getFeedItemList(feed);
        for (FeedItem item : loadedItems) {
            assertTrue(item.isPlayed());
        }
    }

    @Test
    public void testMarkAllItemsReadSameFeed() throws InterruptedException, ExecutionException, TimeoutException {
        final int NUM_ITEMS = 10;
        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title " + i, "id " + i, "link " + i, new Date(), FeedItem.UNPLAYED, feed);
            feed.getItems().add(item);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }

        DBWriter.markAllItemsRead().get(TIMEOUT, TimeUnit.SECONDS);
        List<FeedItem> loadedItems = DBReader.getFeedItemList(feed);
        for (FeedItem item : loadedItems) {
            assertTrue(item.isPlayed());
        }
    }

    private static Feed createTestFeed(int numItems) {
        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < numItems; i++) {
            FeedItem item = new FeedItem(0, "title " + i, "id " + i, "link " + i, new Date(), FeedItem.PLAYED, feed);
            feed.getItems().add(item);
        }

        withPodDB(adapter -> adapter.setCompleteFeed(feed));

        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }
        return feed;
    }

    private static void withPodDB(Consumer<PodDBAdapter> action) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        try {
            adapter.open();
            action.accept(adapter);
        } finally {
            adapter.close();
        }
    }

    private static void assertQueueByItemIds(
            String message,
            long... itemIdsExpected
    ) {
        List<FeedItem> queue = DBReader.getQueue();
        List<Long> itemIdsActualList = toItemIds(queue);
        List<Long> itemIdsExpectedList = new ArrayList<Long>(itemIdsExpected.length);
        for (long id : itemIdsExpected) {
            itemIdsExpectedList.add(id);
        }

        assertEquals(message, itemIdsExpectedList, itemIdsActualList);
    }

    private static List<Long> toItemIds(List<FeedItem> items) {
        List<Long> itemIds = new ArrayList<Long>(items.size());
        for(FeedItem item : items) {
            itemIds.add(item.getId());
        }
        return itemIds;
    }

}
