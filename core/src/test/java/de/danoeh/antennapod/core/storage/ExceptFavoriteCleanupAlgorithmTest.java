package de.danoeh.antennapod.core.storage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.preferences.UserPreferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the APFavoriteCleanupAlgorithm is working correctly.
 */
@RunWith(RobolectricTestRunner.class)
public class ExceptFavoriteCleanupAlgorithmTest extends DbCleanupTests {
    private final int numberOfItems = EPISODE_CACHE_SIZE * 2;

    public ExceptFavoriteCleanupAlgorithmTest() {
        setCleanupAlgorithm(UserPreferences.EPISODE_CLEANUP_EXCEPT_FAVORITE);
    }

    @Test
    public void testPerformAutoCleanupHandleUnplayed() throws IOException {
        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        populateItems(numberOfItems, feed, items, files, FeedItem.UNPLAYED, false, false);

        DBTasks.performAutoCleanup(context);
        for (int i = 0; i < files.size(); i++) {
            if (i < EPISODE_CACHE_SIZE) {
                assertTrue("Only enough items should be deleted", files.get(i).exists());
            } else {
                assertFalse("Expected episode to be deleted", files.get(i).exists());
            }
        }
    }

    @Test
    public void testPerformAutoCleanupDeletesQueued() throws IOException {
        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        populateItems(numberOfItems, feed, items, files, FeedItem.UNPLAYED, true, false);

        DBTasks.performAutoCleanup(context);
        for (int i = 0; i < files.size(); i++) {
            if (i < EPISODE_CACHE_SIZE) {
                assertTrue("Only enough items should be deleted", files.get(i).exists());
            } else {
                assertFalse("Queued episodes should be deleted", files.get(i).exists());
            }
        }
    }

    @Test
    public void testPerformAutoCleanupSavesFavorited() throws IOException {
        Feed feed = new Feed("url", null, "title");
        List<FeedItem> items = new ArrayList<>();
        feed.setItems(items);
        List<File> files = new ArrayList<>();
        populateItems(numberOfItems, feed, items, files, FeedItem.UNPLAYED, false, true);

        DBTasks.performAutoCleanup(context);
        for (int i = 0; i < files.size(); i++) {
            assertTrue("Favorite episodes should should not be deleted", files.get(i).exists());
        }
    }

    @Override
    public void testPerformAutoCleanupShouldNotDeleteBecauseInQueue() throws IOException {
        // Yes it should
    }

    @Override
    public void testPerformAutoCleanupShouldNotDeleteBecauseInQueue_withFeedsWithNoMedia() throws IOException {
        // Yes it should
    }
}
