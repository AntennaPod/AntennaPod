package instrumentationTest.de.test.antennapod.storage;

import android.content.Context;
import android.database.Cursor;
import android.test.InstrumentationTestCase;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.storage.PodDBAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test class for DBWriter
 */
public class DBWriterTest extends InstrumentationTestCase {
    private static final String TEST_FOLDER = "testDBWriter";

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

        FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", dest.getAbsolutePath(), "download_url", true, null);
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

        Feed feed = new Feed ("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());

        // create Feed image
        File imgFile = new File(destFolder, "image");
        assertTrue(imgFile.createNewFile());
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setFeed(feed);
        feed.setImage(image);

        List<File> itemFiles = new ArrayList<File>();
        // create items with downloaded media files
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), true, feed);
            feed.getItems().add(item);

            File enc = new File(destFolder, "file " + i);
            assertTrue(enc.createNewFile());
            itemFiles.add(enc);

            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", true, null);
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

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(5, TimeUnit.SECONDS);

        // check if files still exist
        assertFalse(imgFile.exists());
        for (File f : itemFiles) {
            assertFalse(f.exists());
        }

        adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        c = adapter.getImageOfFeedCursor(image.getId());
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

    public void testDeleteFeedNoImage() throws ExecutionException, InterruptedException, IOException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed ("url", new Date(), "title");
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
            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", true, null);
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

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(5, TimeUnit.SECONDS);

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

        Feed feed = new Feed ("url", new Date(), "title");
        feed.setItems(null);

        // create Feed image
        File imgFile = new File(destFolder, "image");
        assertTrue(imgFile.createNewFile());
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setFeed(feed);
        feed.setImage(image);

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        assertTrue(feed.getImage().getId() != 0);

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(5, TimeUnit.SECONDS);

        // check if files still exist
        assertFalse(imgFile.exists());

        adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        c = adapter.getImageOfFeedCursor(image.getId());
        assertTrue(c.getCount() == 0);
        c.close();
    }

    public void testDeleteFeedNoFeedMedia() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed ("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());

        // create Feed image
        File imgFile = new File(destFolder, "image");
        assertTrue(imgFile.createNewFile());
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setFeed(feed);
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

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(5, TimeUnit.SECONDS);

        // check if files still exist
        assertFalse(imgFile.exists());

        adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        c = adapter.getImageOfFeedCursor(image.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        for (FeedItem item : feed.getItems()) {
            c = adapter.getFeedItemCursor(String.valueOf(item.getId()));
            assertTrue(c.getCount() == 0);
            c.close();
        }
    }

    public void testDeleteFeedWithQueueItems() throws ExecutionException, InterruptedException, TimeoutException {
        File destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);

        Feed feed = new Feed ("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());

        // create Feed image
        File imgFile = new File(destFolder, "image");
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setFeed(feed);
        feed.setImage(image);

        List<File> itemFiles = new ArrayList<File>();
        // create items with downloaded media files
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), true, feed);
            feed.getItems().add(item);

            File enc = new File(destFolder, "file " + i);
            itemFiles.add(enc);

            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", false, null);
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
        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(5, TimeUnit.SECONDS);
        adapter.open();

        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        c = adapter.getImageOfFeedCursor(image.getId());
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

        Feed feed = new Feed ("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());

        // create Feed image
        File imgFile = new File(destFolder, "image");
        FeedImage image = new FeedImage(0, "image", imgFile.getAbsolutePath(), "url", true);
        image.setFeed(feed);
        feed.setImage(image);

        List<File> itemFiles = new ArrayList<File>();
        // create items with downloaded media files
        for (int i = 0; i < 10; i++) {
            FeedItem item = new FeedItem(0, "Item " + i, "Item" + i, "url", new Date(), true, feed);
            feed.getItems().add(item);

            File enc = new File(destFolder, "file " + i);
            itemFiles.add(enc);

            FeedMedia media = new FeedMedia(0, item, 1, 1, 1, "mime_type", enc.getAbsolutePath(), "download_url", false, null);
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

        DBWriter.deleteFeed(getInstrumentation().getTargetContext(), feed.getId()).get(5, TimeUnit.SECONDS);

        adapter = new PodDBAdapter(getInstrumentation().getContext());
        adapter.open();
        Cursor c = adapter.getFeedCursor(feed.getId());
        assertTrue(c.getCount() == 0);
        c.close();
        c = adapter.getImageOfFeedCursor(image.getId());
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

    public void testAddItemToPlaybackHistoryNotPlayedYet() throws ExecutionException, InterruptedException {
        FeedMedia media = new FeedMedia(0, null, 10, 0, 1, "mime", null, "url", false, null);
        DBWriter.addItemToPlaybackHistory(getInstrumentation().getTargetContext(), media).get();
        assertTrue(media.getId() != 0);
        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getTargetContext());
        adapter.open();
        media = DBReader.getFeedMedia(getInstrumentation().getTargetContext(), media.getId());
        adapter.close();

        assertNotNull(media);
        assertNotNull(media.getPlaybackCompletionDate());
    }

    public void testAddItemToPlaybackHistoryAlreadyPlayed() throws ExecutionException, InterruptedException {
        final long OLD_DATE = 0;
        FeedMedia media = new FeedMedia(0, null, 10, 0, 1, "mime", null, "url", false, new Date(OLD_DATE));
        DBWriter.addItemToPlaybackHistory(getInstrumentation().getTargetContext(), media).get();
        assertTrue(media.getId() != 0);
        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getTargetContext());
        adapter.open();
        media = DBReader.getFeedMedia(getInstrumentation().getTargetContext(), media.getId());
        adapter.close();

        assertNotNull(media);
        assertNotNull(media.getPlaybackCompletionDate());
        assertFalse(OLD_DATE == media.getPlaybackCompletionDate().getTime());
    }

}
