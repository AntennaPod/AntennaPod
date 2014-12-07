package de.test.antennapod.storage;

import android.content.Context;
import android.database.Cursor;
import android.test.InstrumentationTestCase;
import android.util.Log;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.SimpleChapter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test class for DBWriter
 */
public class DBWriterTest extends InstrumentationTestCase {
    private static final String TAG = "DBWriterTest";
    private static final String TEST_FOLDER = "testDBWriter";
    private static final long TIMEOUT = 5L;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        final Context context = getInstrumentation().getTargetContext();
        assertTrue(PodDBAdapter.deleteDatabase(getInstrumentation().getTargetContext()));

        File testDir = context.getExternalFilesDir(TEST_FOLDER);
        assertNotNull(testDir);
        for (File f : testDir.listFiles()) {
            f.delete();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final Context context = getInstrumentation().getTargetContext();
        context.deleteDatabase(PodDBAdapter.DATABASE_NAME);
        // make sure database is created
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.close();
    }

    public void testDeleteFeedMediaOfItemFileExists() throws IOException, ExecutionException, InterruptedException {
        File dest = new File(getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER), "testFile");

        assertTrue(dest.createNewFile());

        Feed feed = new Feed("url", new Date(), "title");
        List<FeedItem> items = new ArrayList<FeedItem>();
        feed.setItems(items);
        FeedItem item = new FeedItem(0, "Item", "Item", "url", new Date(), true, feed);

        FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", dest.getAbsolutePath(), "download_url", true, null, 0);
        item.setMedia(media);

        items.add(item);

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getTargetContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();
        assertTrue(media.getId() != 0);
        assertTrue(item.getId() != 0);

        DBWriter.deleteFeedMediaOfItem(getInstrumentation().getTargetContext(), media.getId()).get();
        media = DBReader.getFeedMedia(getInstrumentation().getTargetContext(), media.getId());
        assertNotNull(media);
        assertFalse(dest.exists());
        assertFalse(media.isDownloaded());
        assertNull(media.getFile_url());
    }

    public void testDeleteFeed() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());

        // create Feed image
        File imgFile = new File(destFolder, "image");
        assertTrue(imgFile.createNewFile());
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setOwner(feed);
        feed.setImage(image);

        List<File> itemFiles = new ArrayList<File>();
        // create items with downloaded media files
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), true, feed, true);
            feed.getItems().add(item);

            File enc = new File(destFolder, "file " + i);
            assertTrue(enc.createNewFile());
            itemFiles.add(enc);

            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", true, null, 0);
            item.setMedia(media);

            item.setChapters(new ArrayList<Chapter>());
            item.getChapters().add(new SimpleChapter(0, "item " + i, item, "example.com"));
        }

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        assertTrue(feed.getImage().getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
            assertTrue(item.getChapters().get(0).getId() != 0);
        }

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);

        // check if files still exist
        assertFalse(imgFile.exists());
        for (File f : itemFiles) {
            assertFalse(f.exists());
        }

        adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertEquals(0, c.getCount());
        c.close();
        c = adapter.getImageCursor(image.getId());
        assertEquals(0, c.getCount());
        c.close();
        for (FeedItem item : feed.getItems()) {
            c = adapter.getFeedItemCursor(String.valueOf(item.getId()));
            assertEquals(0, c.getCount());
            c.close();
            c = adapter.getSingleFeedMediaCursor(item.getMedia().getId());
            assertEquals(0, c.getCount());
            c.close();
            c = adapter.getSimpleChaptersOfFeedItemCursor(item);
            assertEquals(0, c.getCount());
        }
    }

    public void testDeleteFeedNoImage() throws ExecutionException, InterruptedException, IOException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());

        feed.setImage(null);

        List<File> itemFiles = new ArrayList<File>();
        // create items with downloaded media files
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), true, feed);
            feed.getItems().add(item);

            File enc = new File(destFolder, "file " + i);
            assertTrue(enc.createNewFile());

            itemFiles.add(enc);
            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", true, null, 0);
            item.setMedia(media);
        }

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getContext());
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

        adapter = new PodDBAdapter(getInstrumentation().getContext());
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
    }

    public void testDeleteFeedNoItems() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(null);

        // create Feed image
        File imgFile = new File(destFolder, "image");
        assertTrue(imgFile.createNewFile());
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setOwner(feed);
        feed.setImage(image);

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        assertTrue(feed.getImage().getId() != 0);

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);

        // check if files still exist
        assertFalse(imgFile.exists());

        adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        c = adapter.getImageCursor(image.getId());
        assertTrue(c.getCount() == 0);
        c.close();
    }

    public void testDeleteFeedNoFeedMedia() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());

        // create Feed image
        File imgFile = new File(destFolder, "image");
        assertTrue(imgFile.createNewFile());
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setOwner(feed);
        feed.setImage(image);

        // create items
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), true, feed);
            feed.getItems().add(item);

        }

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        assertTrue(feed.getImage().getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);

        // check if files still exist
        assertFalse(imgFile.exists());

        adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        c = adapter.getImageCursor(image.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        for (FeedItem item : feed.getItems()) {
            c = adapter.getFeedItemCursor(String.valueOf(item.getId()));
            assertTrue(c.getCount() == 0);
            c.close();
        }
    }

    public void testDeleteFeedWithItemImages() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());

        // create Feed image
        File imgFile = new File(destFolder, "image");
        assertTrue(imgFile.createNewFile());
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setOwner(feed);
        feed.setImage(image);

        // create items with images
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), true, feed);
            feed.getItems().add(item);
            File itemImageFile = new File(destFolder, "item-image-" + i);
            FeedImage itemImage = new FeedImage(0, "item-image" + i, itemImageFile.getAbsolutePath(), "url", true);
            item.setImage(itemImage);
        }

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        assertTrue(feed.getImage().getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getImage().getId() != 0);
        }

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);

        // check if files still exist
        assertFalse(imgFile.exists());

        adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        c = adapter.getImageCursor(image.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        for (FeedItem item : feed.getItems()) {
            c = adapter.getFeedItemCursor(String.valueOf(item.getId()));
            assertTrue(c.getCount() == 0);
            c.close();
            c = adapter.getImageCursor(item.getImage().getId());
            assertEquals(0, c.getCount());
            c.close();
        }
    }

    public void testDeleteFeedWithQueueItems() throws ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());

        // create Feed image
        File imgFile = new File(destFolder, "image");
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setOwner(feed);
        feed.setImage(image);

        List<File> itemFiles = new ArrayList<File>();
        // create items with downloaded media files
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), true, feed);
            feed.getItems().add(item);

            File enc = new File(destFolder, "file " + i);
            itemFiles.add(enc);

            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", false, null, 0);
            item.setMedia(media);
        }

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        assertTrue(feed.getImage().getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }


        List<FeedItem> queue = new ArrayList<FeedItem>();
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
        c = adapter.getImageCursor(image.getId());
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

    public void testDeleteFeedNoDownloadedFiles() throws ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());

        // create Feed image
        File imgFile = new File(destFolder, "image");
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setOwner(feed);
        feed.setImage(image);

        List<File> itemFiles = new ArrayList<File>();
        // create items with downloaded media files
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), true, feed);
            feed.getItems().add(item);

            File enc = new File(destFolder, "file " + i);
            itemFiles.add(enc);

            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", false, null, 0);
            item.setMedia(media);
        }

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        assertTrue(feed.getImage().getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);

        adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        c = adapter.getImageCursor(image.getId());
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
    }

    private FeedMedia playbackHistorySetup(Date playbackCompletionDate) {
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());
        FeedItem item = new FeedItem(0, "title", "id", "link", new Date(), true, feed);
        FeedMedia media = new FeedMedia(0, item, 10, 0, 1, "mime", null, "url", false, playbackCompletionDate, 0);
        feed.getItems().add(item);
        item.setMedia(media);
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();
        assertTrue(media.getId() != 0);
        return media;
    }

    public void testAddItemToPlaybackHistoryNotPlayedYet() throws ExecutionException, InterruptedException {
        final Context context = getInstrumentation().getTargetContext();

        FeedMedia media = playbackHistorySetup(null);
        DBWriter.addItemToPlaybackHistory(context, media).get();
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        media = DBReader.getFeedMedia(context, media.getId());
        adapter.close();

        assertNotNull(media);
        assertNotNull(media.getPlaybackCompletionDate());
    }

    public void testAddItemToPlaybackHistoryAlreadyPlayed() throws ExecutionException, InterruptedException {
        final long OLD_DATE = 0;
        final Context context = getInstrumentation().getTargetContext();

        FeedMedia media = playbackHistorySetup(new Date(OLD_DATE));
        DBWriter.addItemToPlaybackHistory(getInstrumentation().getTargetContext(), media).get();
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        media = DBReader.getFeedMedia(context, media.getId());
        adapter.close();

        assertNotNull(media);
        assertNotNull(media.getPlaybackCompletionDate());
        assertFalse(OLD_DATE == media.getPlaybackCompletionDate().getTime());
    }

    private Feed queueTestSetupMultipleItems(final int NUM_ITEMS) throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title " + i, "id " + i, "link " + i, new Date(), true, feed);
            feed.getItems().add(item);
        }

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }
        List<Future<?>> futures = new ArrayList<Future<?>>();
        for (FeedItem item : feed.getItems()) {
            futures.add(DBWriter.addQueueItem(context, item.getId()));
        }
        for (Future<?> f : futures) {
            f.get(TIMEOUT, TimeUnit.SECONDS);
        }
        return feed;
    }

    public void testAddQueueItemSingleItem() throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());
        FeedItem item = new FeedItem(0, "title", "id", "link", new Date(), true, feed);
        feed.getItems().add(item);

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(item.getId() != 0);
        DBWriter.addQueueItem(context, item.getId()).get(TIMEOUT, TimeUnit.SECONDS);

        adapter = new PodDBAdapter(context);
        adapter.open();
        Cursor cursor = adapter.getQueueIDCursor();
        assertTrue(cursor.moveToFirst());
        assertTrue(cursor.getLong(0) == item.getId());
        cursor.close();
        adapter.close();
    }

    public void testAddQueueItemSingleItemAlreadyInQueue() throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());
        FeedItem item = new FeedItem(0, "title", "id", "link", new Date(), true, feed);
        feed.getItems().add(item);

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(item.getId() != 0);
        DBWriter.addQueueItem(context, item.getId()).get(TIMEOUT, TimeUnit.SECONDS);

        adapter = new PodDBAdapter(context);
        adapter.open();
        Cursor cursor = adapter.getQueueIDCursor();
        assertTrue(cursor.moveToFirst());
        assertTrue(cursor.getLong(0) == item.getId());
        cursor.close();
        adapter.close();

        DBWriter.addQueueItem(context, item.getId()).get(TIMEOUT, TimeUnit.SECONDS);
        adapter = new PodDBAdapter(context);
        adapter.open();
        cursor = adapter.getQueueIDCursor();
        assertTrue(cursor.moveToFirst());
        assertTrue(cursor.getLong(0) == item.getId());
        assertTrue(cursor.getCount() == 1);
        cursor.close();
        adapter.close();
    }

    public void testAddQueueItemMultipleItems() throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        final int NUM_ITEMS = 10;

        Feed feed = queueTestSetupMultipleItems(NUM_ITEMS);
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        Cursor cursor = adapter.getQueueIDCursor();
        assertTrue(cursor.moveToFirst());
        assertTrue(cursor.getCount() == NUM_ITEMS);
        for (int i = 0; i < NUM_ITEMS; i++) {
            assertTrue(cursor.moveToPosition(i));
            assertTrue(cursor.getLong(0) == feed.getItems().get(i).getId());
        }
        cursor.close();
        adapter.close();
    }

    public void testClearQueue() throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        final int NUM_ITEMS = 10;

        Feed feed = queueTestSetupMultipleItems(NUM_ITEMS);
        DBWriter.clearQueue(context).get(TIMEOUT, TimeUnit.SECONDS);
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        Cursor cursor = adapter.getQueueIDCursor();
        assertFalse(cursor.moveToFirst());
        cursor.close();
        adapter.close();
    }

    public void testRemoveQueueItem() throws InterruptedException, ExecutionException, TimeoutException {
        final int NUM_ITEMS = 10;
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title " + i, "id " + i, "link " + i, new Date(), true, feed);
            feed.getItems().add(item);
        }

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }
        for (int removeIndex = 0; removeIndex < NUM_ITEMS; removeIndex++) {
            final long id = feed.getItems().get(removeIndex).getId();
            adapter = new PodDBAdapter(context);
            adapter.open();
            adapter.setQueue(feed.getItems());
            adapter.close();

            DBWriter.removeQueueItem(context, id, false).get(TIMEOUT, TimeUnit.SECONDS);
            adapter = new PodDBAdapter(context);
            adapter.open();
            Cursor queue = adapter.getQueueIDCursor();
            assertTrue(queue.getCount() == NUM_ITEMS - 1);
            for (int i = 0; i < queue.getCount(); i++) {
                assertTrue(queue.moveToPosition(i));
                final long queueID = queue.getLong(0);
                assertTrue(queueID != id);  // removed item is no longer in queue
                boolean idFound = false;
                for (FeedItem item : feed.getItems()) { // items that were not removed are still in the queue
                    idFound = idFound | (item.getId() == queueID);
                }
                assertTrue(idFound);
            }

            queue.close();
            adapter.close();
        }
    }

    public void testMoveQueueItem() throws InterruptedException, ExecutionException, TimeoutException {
        final int NUM_ITEMS = 10;
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title " + i, "id " + i, "link " + i, new Date(), true, feed);
            feed.getItems().add(item);
        }

        PodDBAdapter adapter = new PodDBAdapter(context);
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

                adapter = new PodDBAdapter(context);
                adapter.open();
                adapter.setQueue(feed.getItems());
                adapter.close();

                DBWriter.moveQueueItem(context, from, to, false).get(TIMEOUT, TimeUnit.SECONDS);
                adapter = new PodDBAdapter(context);
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

    public void testMarkFeedRead() throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        final int NUM_ITEMS = 10;
        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title " + i, "id " + i, "link " + i, new Date(), false, feed);
            feed.getItems().add(item);
        }

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }

        DBWriter.markFeedRead(context, feed.getId()).get(TIMEOUT, TimeUnit.SECONDS);
        List<FeedItem> loadedItems = DBReader.getFeedItemList(context, feed);
        for (FeedItem item : loadedItems) {
            assertTrue(item.isRead());
        }
    }

    public void testMarkAllItemsReadSameFeed() throws InterruptedException, ExecutionException, TimeoutException {
        final Context context = getInstrumentation().getTargetContext();
        final int NUM_ITEMS = 10;
        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title " + i, "id " + i, "link " + i, new Date(), false, feed);
            feed.getItems().add(item);
        }

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
        }

        DBWriter.markAllItemsRead(context).get(TIMEOUT, TimeUnit.SECONDS);
        List<FeedItem> loadedItems = DBReader.getFeedItemList(context, feed);
        for (FeedItem item : loadedItems) {
            assertTrue(item.isRead());
        }
    }

}
