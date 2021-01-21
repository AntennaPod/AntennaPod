package de.danoeh.antennapod.core.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.test.platform.app.InstrumentationRegistry;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the APNullCleanupAlgorithm is working correctly.
 */
@RunWith(RobolectricTestRunner.class)
public class DbNullCleanupAlgorithmTest {

    private static final int EPISODE_CACHE_SIZE = 5;

    private Context context;

    private File destFolder;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        destFolder = context.getExternalCacheDir();
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

        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext()).edit();
        prefEdit.putString(UserPreferences.PREF_EPISODE_CACHE_SIZE, Integer.toString(EPISODE_CACHE_SIZE));
        prefEdit.putString(UserPreferences.PREF_EPISODE_CLEANUP,
                Integer.toString(UserPreferences.EPISODE_CLEANUP_NULL));
        prefEdit.commit();

        UserPreferences.init(context);
    }

    @After
    public void tearDown() {
        DBWriter.tearDownTests();
        PodDBAdapter.deleteDatabase();
        PodDBAdapter.tearDownTests();

        cleanupDestFolder(destFolder);
        assertTrue(destFolder.delete());
    }

    private void cleanupDestFolder(File destFolder) {
        //noinspection ConstantConditions
        for (File f : destFolder.listFiles()) {
            assertTrue(f.delete());
        }
    }

    /**
     * A test with no items in the queue, but multiple items downloaded.
     * The null algorithm should never delete any items, even if they're played and not in the queue.
     */
    @Test
    public void testPerformAutoCleanupShouldNotDelete() throws IOException {
        final int numItems = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        for (int i = 0; i < numItems; i++) {
            FeedItem item = new FeedItem(0, "title", "id", "link", new Date(), FeedItem.PLAYED, feed);

            File f = new File(destFolder, "file " + i);
            assertTrue(f.createNewFile());
            files.add(f);
            item.setMedia(new FeedMedia(0, item, 1, 0, 1L, "m", f.getAbsolutePath(), "url", true,
                    new Date(numItems - i), 0, 0));
            items.add(item);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        for (FeedItem item : items) {
            assertTrue(item.getId() != 0);
            //noinspection ConstantConditions
            assertTrue(item.getMedia().getId() != 0);
        }
        DBTasks.performAutoCleanup(context);
        for (int i = 0; i < files.size(); i++) {
            assertTrue(files.get(i).exists());
        }
    }
}
