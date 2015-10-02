package de.test.antennapod.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.FlakyTest;
import android.test.InstrumentationTestCase;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

import static de.test.antennapod.storage.DBTestUtils.saveFeedlist;

/**
 * Test class for DBTasks
 */
public class DBCleanupTests extends InstrumentationTestCase {

    private static final String TAG = "DBTasksTest";
    protected static final int EPISODE_CACHE_SIZE = 5;
    private final int cleanupAlgorithm;

    protected Context context;

    protected File destFolder;

    public DBCleanupTests() {
        this.cleanupAlgorithm = UserPreferences.EPISODE_CLEANUP_DEFAULT;
    }

    public DBCleanupTests(int cleanupAlgorithm) {
        this.cleanupAlgorithm = cleanupAlgorithm;
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        assertTrue(PodDBAdapter.deleteDatabase());

        cleanupDestFolder(destFolder);
        assertTrue(destFolder.delete());
    }

    private void cleanupDestFolder(File destFolder) {
        for (File f : destFolder.listFiles()) {
            assertTrue(f.delete());
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getTargetContext();
        destFolder = context.getExternalCacheDir();
        cleanupDestFolder(destFolder);
        assertNotNull(destFolder);
        assertTrue(destFolder.exists());
        assertTrue(destFolder.canWrite());

        // create new database
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit();
        prefEdit.putString(UserPreferences.PREF_EPISODE_CACHE_SIZE, Integer.toString(EPISODE_CACHE_SIZE));
        prefEdit.putInt(UserPreferences.PREF_EPISODE_CLEANUP, cleanupAlgorithm);
        prefEdit.commit();

        UserPreferences.init(context);
    }

    @FlakyTest(tolerance = 3)
    public void testPerformAutoCleanupShouldDelete() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", new Date(), "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        for (int i = 0; i < NUM_ITEMS; i++) {
            Date itemDate = new Date(NUM_ITEMS - i);
            FeedItem item = new FeedItem(0, "title", "id", "link", itemDate, FeedItem.PLAYED, feed);

            File f = new File(destFolder, "file " + i);
            assertTrue(f.createNewFile());
            files.add(f);
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, itemDate, 0));
            items.add(item);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : items) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }
        DBTasks.performAutoCleanup(context);
        for (int i = 0; i < files.size(); i++) {
            if (i < EPISODE_CACHE_SIZE) {
                assertTrue(files.get(i).exists());
            } else {
                assertFalse(files.get(i).exists());
            }
        }
    }

    @FlakyTest(tolerance = 3)
    public void testPerformAutoCleanupHandleUnplayed() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", new Date(), "title");
        List<FeedItem> items = new ArrayList<FeedItem>();
        feed.setItems(items);
        List<File> files = new ArrayList<File>();
        for (int i = 0; i < NUM_ITEMS; i++) {
            Date itemDate = new Date(NUM_ITEMS - i);
            FeedItem item = new FeedItem(0, "title", "id", "link", itemDate, FeedItem.UNPLAYED, feed);

            File f = new File(destFolder, "file " + i);
            assertTrue(f.createNewFile());
            assertTrue(f.exists());
            files.add(f);
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, itemDate, 0));
            items.add(item);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : items) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }
        DBTasks.performAutoCleanup(context);
        for (File file : files) {
            assertTrue(file.exists());
        }
    }

    @FlakyTest(tolerance = 3)
    public void testPerformAutoCleanupShouldNotDeleteBecauseInQueue() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", new Date(), "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        for (int i = 0; i < NUM_ITEMS; i++) {
            Date itemDate = new Date(NUM_ITEMS - i);
            FeedItem item = new FeedItem(0, "title", "id", "link", itemDate, FeedItem.PLAYED, feed);

            File f = new File(destFolder, "file " + i);
            assertTrue(f.createNewFile());
            assertTrue(f.exists());
            files.add(f);
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, itemDate, 0));
            items.add(item);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.setQueue(items);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : items) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }
        DBTasks.performAutoCleanup(context);
        for (File file : files) {
            assertTrue(file.exists());
        }
    }

    /**
     * Reproduces a bug where DBTasks.performAutoCleanup(android.content.Context) would use the ID of the FeedItem in the
     * call to DBWriter.deleteFeedMediaOfItem instead of the ID of the FeedMedia. This would cause the wrong item to be deleted.
     * @throws IOException
     */
    @FlakyTest(tolerance = 3)
    public void testPerformAutoCleanupShouldNotDeleteBecauseInQueue_withFeedsWithNoMedia() throws IOException {
        // add feed with no enclosures so that item ID != media ID
        saveFeedlist(1, 10, false);

        // add candidate for performAutoCleanup
        List<Feed> feeds = saveFeedlist(1, 1, true);
        FeedMedia m = feeds.get(0).getItems().get(0).getMedia();
        m.setDownloaded(true);
        m.setFile_url("file");
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setMedia(m);
        adapter.close();

        testPerformAutoCleanupShouldNotDeleteBecauseInQueue();
    }

    @FlakyTest(tolerance = 3)
    public void testPerformAutoCleanupShouldNotDeleteBecauseFavorite() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", new Date(), "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        for (int i = 0; i < NUM_ITEMS; i++) {
            Date itemDate = new Date(NUM_ITEMS - i);
            FeedItem item = new FeedItem(0, "title", "id", "link", itemDate, FeedItem.PLAYED, feed);
            File f = new File(destFolder, "file " + i);
            assertTrue(f.createNewFile());
            assertTrue(f.exists());
            files.add(f);
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, itemDate, 0));
            items.add(item);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.setFavorites(items);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : items) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }
        DBTasks.performAutoCleanup(context);
        for (File file : files) {
            assertTrue(file.exists());
        }
    }
}
