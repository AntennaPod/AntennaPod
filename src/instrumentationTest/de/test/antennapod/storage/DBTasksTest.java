package instrumentationTest.de.test.antennapod.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.PodDBAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, new Date(NUM_ITEMS - i)));
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
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, new Date(NUM_ITEMS - i)));
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
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true, new Date(NUM_ITEMS - i)));
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
}
