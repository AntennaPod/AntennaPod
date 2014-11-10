package de.test.antennapod.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.flattr.FlattrStatus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static de.test.antennapod.storage.DBTestUtils.*;

/**
 * Test class for DBTasks
 */
public class DBTasksTest extends InstrumentationTestCase {
    private static final String TEST_FOLDER = "testDBTasks";
    private static final int EPISODE_CACHE_SIZE = 5;

    private File destFolder;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        final Context context = getInstrumentation().getTargetContext();
        assertTrue(PodDBAdapter.deleteDatabase(context));

        for (File f : destFolder.listFiles()) {
            assertTrue(f.delete());
        }
        assertTrue(destFolder.delete());

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        destFolder = getInstrumentation().getTargetContext().getExternalFilesDir(TEST_FOLDER);
        assertNotNull(destFolder);
        assertTrue(destFolder.exists());
        assertTrue(destFolder.canWrite());

        final Context context = getInstrumentation().getTargetContext();
        context.deleteDatabase(PodDBAdapter.DATABASE_NAME);
        // make sure database is created
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.close();

        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(getInstrumentation().getTargetContext().getApplicationContext()).edit();
        prefEdit.putString(UserPreferences.PREF_EPISODE_CACHE_SIZE, Integer.toString(EPISODE_CACHE_SIZE));
        prefEdit.commit();
    }

    public void testPerformAutoCleanupShouldDelete() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", new Date(), "title");
        List<FeedItem> items = new ArrayList<FeedItem>();
        feed.setItems(items);
        List<File> files = new ArrayList<File>();
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title", "id", "link", new Date(), true, feed);

            File f = new File(destFolder, "file " + i);
            assertTrue(f.createNewFile());
            files.add(f);
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, new Date(NUM_ITEMS - i), 0));
            items.add(item);
        }

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getTargetContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : items) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }
        DBTasks.performAutoCleanup(getInstrumentation().getTargetContext());
        for (int i = 0; i < files.size(); i++) {
            if (i < EPISODE_CACHE_SIZE) {
                assertTrue(files.get(i).exists());
            } else {
                assertFalse(files.get(i).exists());
            }
        }
    }

    public void testPerformAutoCleanupShouldNotDeleteBecauseUnread() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", new Date(), "title");
        List<FeedItem> items = new ArrayList<FeedItem>();
        feed.setItems(items);
        List<File> files = new ArrayList<File>();
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title", "id", "link", new Date(), false, feed);

            File f = new File(destFolder, "file " + i);
            assertTrue(f.createNewFile());
            assertTrue(f.exists());
            files.add(f);
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, new Date(NUM_ITEMS - i), 0));
            items.add(item);
        }

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getTargetContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : items) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }
        DBTasks.performAutoCleanup(getInstrumentation().getTargetContext());
        for (File file : files) {
            assertTrue(file.exists());
        }
    }

    public void testPerformAutoCleanupShouldNotDeleteBecauseInQueue() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", new Date(), "title");
        List<FeedItem> items = new ArrayList<FeedItem>();
        feed.setItems(items);
        List<File> files = new ArrayList<File>();
        for (int i = 0; i < NUM_ITEMS; i++) {
            FeedItem item = new FeedItem(0, "title", "id", "link", new Date(), true, feed);

            File f = new File(destFolder, "file " + i);
            assertTrue(f.createNewFile());
            assertTrue(f.exists());
            files.add(f);
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, new Date(NUM_ITEMS - i), 0));
            items.add(item);
        }

        PodDBAdapter adapter = new PodDBAdapter(getInstrumentation().getTargetContext());
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.setQueue(items);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : items) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }
        DBTasks.performAutoCleanup(getInstrumentation().getTargetContext());
        for (File file : files) {
            assertTrue(file.exists());
        }
    }

    /**
     * Reproduces a bug where DBTasks.performAutoCleanup(android.content.Context) would use the ID of the FeedItem in the
     * call to DBWriter.deleteFeedMediaOfItem instead of the ID of the FeedMedia. This would cause the wrong item to be deleted.
     * @throws IOException
     */
    public void testPerformAutoCleanupShouldNotDeleteBecauseInQueue_withFeedsWithNoMedia() throws IOException {
        final Context context = getInstrumentation().getTargetContext();
        // add feed with no enclosures so that item ID != media ID
        saveFeedlist(context, 1, 10, false);

        // add candidate for performAutoCleanup
        List<Feed> feeds = saveFeedlist(getInstrumentation().getTargetContext(), 1, 1, true);
        FeedMedia m = feeds.get(0).getItems().get(0).getMedia();
        m.setDownloaded(true);
        m.setFile_url("file");
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setMedia(m);
        adapter.close();

        testPerformAutoCleanupShouldNotDeleteBecauseInQueue();
    }

    public void testUpdateFeedNewFeed() {
        final Context context = getInstrumentation().getTargetContext();
        final int NUM_ITEMS = 10;

        Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            feed.getItems().add(new FeedItem(0, "item " + i, "id " + i, "link " + i, new Date(), false, feed));
        }
        Feed newFeed = DBTasks.updateFeed(context, feed)[0];

        assertTrue(newFeed == feed);
        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertFalse(item.isRead());
            assertTrue(item.getId() != 0);
        }
    }

    /** Two feeds with the same title, but different download URLs should be treated as different feeds. */
    public void testUpdateFeedSameTitle() {
        final Context context = getInstrumentation().getTargetContext();

        Feed feed1 = new Feed("url1", new Date(), "title");
        Feed feed2 = new Feed("url2", new Date(), "title");

        feed1.setItems(new ArrayList<FeedItem>());
        feed2.setItems(new ArrayList<FeedItem>());

        Feed savedFeed1 = DBTasks.updateFeed(context, feed1)[0];
        Feed savedFeed2 = DBTasks.updateFeed(context, feed2)[0];

        assertTrue(savedFeed1.getId() != savedFeed2.getId());
    }

    public void testUpdateFeedUpdatedFeed() {
        final Context context = getInstrumentation().getTargetContext();
        final int NUM_ITEMS_OLD = 10;
        final int NUM_ITEMS_NEW = 10;

        final Feed feed = new Feed("url", new Date(), "title");
        feed.setItems(new ArrayList<FeedItem>());
        for (int i = 0; i < NUM_ITEMS_OLD; i++) {
            feed.getItems().add(new FeedItem(0, "item " + i, "id " + i, "link " + i, new Date(i), true, feed));
        }
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        // ensure that objects have been saved in db, then reset
        assertTrue(feed.getId() != 0);
        final long feedID = feed.getId();
        feed.setId(0);
        List<Long> itemIDs = new ArrayList<Long>();
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
            itemIDs.add(item.getId());
            item.setId(0);
        }

        for (int i = NUM_ITEMS_OLD; i < NUM_ITEMS_NEW + NUM_ITEMS_OLD; i++) {
            feed.getItems().add(0, new FeedItem(0, "item " + i, "id " + i, "link " + i, new Date(i), true, feed));
        }

        final Feed newFeed = DBTasks.updateFeed(context, feed)[0];
        assertTrue(feed != newFeed);

        updatedFeedTest(newFeed, feedID, itemIDs, NUM_ITEMS_OLD, NUM_ITEMS_NEW);

        final Feed feedFromDB = DBReader.getFeed(context, newFeed.getId());
        assertNotNull(feedFromDB);
        assertTrue(feedFromDB.getId() == newFeed.getId());
        updatedFeedTest(feedFromDB, feedID, itemIDs, NUM_ITEMS_OLD, NUM_ITEMS_NEW);
    }

    private void updatedFeedTest(final Feed newFeed, long feedID, List<Long> itemIDs, final int NUM_ITEMS_OLD, final int NUM_ITEMS_NEW) {
        assertTrue(newFeed.getId() == feedID);
        assertTrue(newFeed.getItems().size() == NUM_ITEMS_NEW + NUM_ITEMS_OLD);
        Collections.reverse(newFeed.getItems());
        Date lastDate = new Date(0);
        for (int i = 0; i < NUM_ITEMS_OLD; i++) {
            FeedItem item = newFeed.getItems().get(i);
            assertTrue(item.getFeed() == newFeed);
            assertTrue(item.getId() == itemIDs.get(i));
            assertTrue(item.isRead());
            assertTrue(item.getPubDate().getTime() >= lastDate.getTime());
            lastDate = item.getPubDate();
        }
        for (int i = NUM_ITEMS_OLD; i < NUM_ITEMS_NEW + NUM_ITEMS_OLD; i++) {
            FeedItem item = newFeed.getItems().get(i);
            assertTrue(item.getFeed() == newFeed);
            assertTrue(item.getId() != 0);
            assertFalse(item.isRead());
            assertTrue(item.getPubDate().getTime() >= lastDate.getTime());
            lastDate = item.getPubDate();
        }
    }

    private void expiredFeedListTestHelper(long lastUpdate, long expirationTime, boolean shouldReturn) {
        final Context context = getInstrumentation().getTargetContext();
        UserPreferences.setUpdateInterval(context, expirationTime);
        Feed feed = new Feed(0, new Date(lastUpdate), "feed", "link", "descr", null,
                null, null, null, "feed", null, null, "url", false, new FlattrStatus(), false, null);
        feed.setItems(new ArrayList<FeedItem>());
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        List<Feed> expiredFeeds = DBTasks.getExpiredFeeds(context);
        assertNotNull(expiredFeeds);
        if (shouldReturn) {
            assertTrue(expiredFeeds.size() == 1);
            assertTrue(expiredFeeds.get(0).getId() == feed.getId());
        } else {
            assertTrue(expiredFeeds.isEmpty());
        }
    }

    public void testGetExpiredFeedsTestShouldReturn() {
        final long expirationTime = 1000 * 60 * 60;
        expiredFeedListTestHelper(System.currentTimeMillis() - expirationTime - 1, expirationTime, true);
    }

    public void testGetExpiredFeedsTestShouldNotReturn() {
        final long expirationTime = 1000 * 60 * 60;
        expiredFeedListTestHelper(System.currentTimeMillis() - expirationTime / 2, expirationTime, false);
    }
}
