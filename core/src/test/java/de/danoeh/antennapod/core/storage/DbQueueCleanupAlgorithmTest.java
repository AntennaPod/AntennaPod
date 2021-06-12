package de.danoeh.antennapod.core.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the APQueueCleanupAlgorithm is working correctly.
 */
@RunWith(RobolectricTestRunner.class)
public class DbQueueCleanupAlgorithmTest extends DbCleanupTests {

    public DbQueueCleanupAlgorithmTest() {
        setCleanupAlgorithm(UserPreferences.EPISODE_CLEANUP_QUEUE);
    }

    /**
     * For APQueueCleanupAlgorithm we expect even unplayed episodes to be deleted if needed
     * if they aren't in the queue.
     */
    @Test
    public void testPerformAutoCleanupHandleUnplayed() throws IOException {
        final int numItems = EPISODE_CACHE_SIZE * 2;

        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        populateItems(numItems, feed, items, files, FeedItem.UNPLAYED, false, false);

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
