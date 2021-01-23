package de.test.antennapod.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static de.test.antennapod.storage.DBTestUtils.saveFeedlist;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for DBTasks
 */
@SmallTest
public class DBCleanupTests {
    static final int EPISODE_CACHE_SIZE = 5;
    private int cleanupAlgorithm;

    Context context;

    private File destFolder;

    public DBCleanupTests() {
        setCleanupAlgorithm(UserPreferences.EPISODE_CLEANUP_DEFAULT);
    }

    protected void setCleanupAlgorithm(int cleanupAlgorithm) {
        this.cleanupAlgorithm = cleanupAlgorithm;
    }

    @After
    public void tearDown() throws Exception {
        assertTrue(PodDBAdapter.deleteDatabase());

        cleanupDestFolder(destFolder);
        assertTrue(destFolder.delete());
    }

    private void cleanupDestFolder(File destFolder) {
        for (File f : destFolder.listFiles()) {
            assertTrue(f.delete());
        }
    }

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        destFolder = new File(context.getCacheDir(), "DDCleanupTests");
        destFolder.mkdir();
        cleanupDestFolder(destFolder);
        assertNotNull(destFolder);
        assertTrue(destFolder.exists());
        assertTrue(destFolder.canWrite());

        // create new database
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit();
        prefEdit.putString(UserPreferences.PREF_EPISODE_CACHE_SIZE, Integer.toString(EPISODE_CACHE_SIZE));
        prefEdit.putString(UserPreferences.PREF_EPISODE_CLEANUP, Integer.toString(cleanupAlgorithm));
        prefEdit.putBoolean(UserPreferences.PREF_ENABLE_AUTODL, true);
        prefEdit.commit();

        UserPreferences.init(context);
    }

    @Test
    public void testPerformAutoCleanupShouldDelete() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        populateItems(NUM_ITEMS, feed, items, files, FeedItem.PLAYED, false, false);

        DBTasks.performAutoCleanup(context);
        for (int i = 0; i < files.size(); i++) {
            if (i < EPISODE_CACHE_SIZE) {
                assertTrue(files.get(i).exists());
            } else {
                assertFalse(files.get(i).exists());
            }
        }
    }

    void populateItems(final int numItems, Feed feed, List<FeedItem> items,
                       List<File> files, int itemState, boolean addToQueue,
                       boolean addToFavorites) throws IOException {
        for (int i = 0; i < numItems; i++) {
            Date itemDate = new Date(numItems - i);
            Date playbackCompletionDate = null;
            if (itemState == FeedItem.PLAYED) {
                playbackCompletionDate = itemDate;
            }
            FeedItem item = new FeedItem(0, "title", "id", "link", itemDate, itemState, feed);

            File f = new File(destFolder, "file " + i);
            assertTrue(f.createNewFile());
            files.add(f);
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, playbackCompletionDate, 0, 0));
            items.add(item);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        if (addToQueue) {
            adapter.setQueue(items);
        }
        if (addToFavorites) {
            adapter.setFavorites(items);
        }
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : items) {
            assertTrue(item.getId() != 0);
            assertTrue(item.getMedia().getId() != 0);
        }
    }

    @Test
    public void testPerformAutoCleanupHandleUnplayed() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        populateItems(NUM_ITEMS, feed, items, files, FeedItem.UNPLAYED, false, false);

        DBTasks.performAutoCleanup(context);
        for (File file : files) {
            assertTrue(file.exists());
        }
    }

    @Test
    public void testPerformAutoCleanupShouldNotDeleteBecauseInQueue() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        populateItems(NUM_ITEMS, feed, items, files, FeedItem.PLAYED, true, false);

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
    @Test
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

    @Test
    public void testPerformAutoCleanupShouldNotDeleteBecauseFavorite() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        populateItems(NUM_ITEMS, feed, items, files, FeedItem.PLAYED, false, true);

        DBTasks.performAutoCleanup(context);
        for (File file : files) {
            assertTrue(file.exists());
        }
    }
}
