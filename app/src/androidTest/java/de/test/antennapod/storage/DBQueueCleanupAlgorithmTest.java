package de.test.antennapod.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.test.filters.SmallTest;
import de.danoeh.antennapodSA.core.feed.Feed;
import de.danoeh.antennapodSA.core.feed.FeedItem;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.core.storage.DBTasks;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the APQueueCleanupAlgorithm is working correctly.
 */
@SmallTest
public class DBQueueCleanupAlgorithmTest extends DBCleanupTests {

    private static final String TAG = "DBQueueCleanupAlgorithmTest";

    public DBQueueCleanupAlgorithmTest() {
        setCleanupAlgorithm(UserPreferences.EPISODE_CLEANUP_QUEUE);
    }

    /**
     * For APQueueCleanupAlgorithm we expect even unplayed episodes to be deleted if needed
     * if they aren't in the queue
     */
    @Test
    public void testPerformAutoCleanupHandleUnplayed() throws IOException {
        final int NUM_ITEMS = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        populateItems(NUM_ITEMS, feed, items, files, FeedItem.UNPLAYED, false, false);

        DBTasks.performAutoCleanup(context);
        for (int i = 0; i < files.size(); i++) {
            if (i < EPISODE_CACHE_SIZE) {
                assertTrue(files.get(i).exists());
            } else {
                assertFalse(files.get(i).exists());
            }
        }
    }
}
