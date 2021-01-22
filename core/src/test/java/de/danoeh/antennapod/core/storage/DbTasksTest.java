package de.danoeh.antennapod.core.storage;

import android.app.Application;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.ApplicationCallbacks;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;

import static de.danoeh.antennapod.core.util.FeedItemUtil.getIdList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link DBTasks}.
 */
@RunWith(RobolectricTestRunner.class)
public class DbTasksTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);
        PlaybackPreferences.init(context);

        Application app = (Application) context;
        ClientConfig.applicationCallbacks = mock(ApplicationCallbacks.class);
        when(ClientConfig.applicationCallbacks.getApplicationInstance()).thenReturn(app);

        // create new database
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();
    }

    @After
    public void tearDown() {
        DBWriter.tearDownTests();
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void testUpdateFeedNewFeed() {
        final int numItems = 10;

        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < numItems; i++) {
            feed.getItems().add(new FeedItem(0, "item " + i, "id " + i, "link " + i,
                    new Date(), FeedItem.UNPLAYED, feed));
        }
        Feed newFeed = DBTasks.updateFeed(context, feed, false);

        assertEquals(feed.getId(), newFeed.getId());
        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertFalse(item.isPlayed());
            assertTrue(item.getId() != 0);
        }
    }

    /** Two feeds with the same title, but different download URLs should be treated as different feeds. */
    @Test
    public void testUpdateFeedSameTitle() {

        Feed feed1 = new Feed("url1", null, "title");
        Feed feed2 = new Feed("url2", null, "title");

        feed1.setItems(new ArrayList<>());
        feed2.setItems(new ArrayList<>());

        Feed savedFeed1 = DBTasks.updateFeed(context, feed1, false);
        Feed savedFeed2 = DBTasks.updateFeed(context, feed2, false);

        assertTrue(savedFeed1.getId() != savedFeed2.getId());
    }

    @Test
    public void testUpdateFeedUpdatedFeed() {
        final int numItemsOld = 10;
        final int numItemsNew = 10;

        final Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < numItemsOld; i++) {
            feed.getItems().add(new FeedItem(0, "item " + i, "id " + i, "link " + i,
                    new Date(i), FeedItem.PLAYED, feed));
        }
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        // ensure that objects have been saved in db, then reset
        assertTrue(feed.getId() != 0);
        final long feedID = feed.getId();
        feed.setId(0);
        List<Long> itemIDs = new ArrayList<>();
        for (FeedItem item : feed.getItems()) {
            assertTrue(item.getId() != 0);
            itemIDs.add(item.getId());
            item.setId(0);
        }

        for (int i = numItemsOld; i < numItemsNew + numItemsOld; i++) {
            feed.getItems().add(0, new FeedItem(0, "item " + i, "id " + i, "link " + i,
                    new Date(i), FeedItem.UNPLAYED, feed));
        }

        final Feed newFeed = DBTasks.updateFeed(context, feed, false);
        assertNotSame(newFeed, feed);

        updatedFeedTest(newFeed, feedID, itemIDs, numItemsOld, numItemsNew);

        final Feed feedFromDB = DBReader.getFeed(newFeed.getId());
        assertNotNull(feedFromDB);
        assertEquals(newFeed.getId(), feedFromDB.getId());
        updatedFeedTest(feedFromDB, feedID, itemIDs, numItemsOld, numItemsNew);
    }

    @Test
    public void testUpdateFeedMediaUrlResetState() {
        final Feed feed = new Feed("url", null, "title");
        FeedItem item = new FeedItem(0, "item", "id", "link", new Date(), FeedItem.PLAYED, feed);
        feed.setItems(singletonList(item));

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        // ensure that objects have been saved in db, then reset
        assertTrue(feed.getId() != 0);
        assertTrue(item.getId() != 0);

        FeedMedia media = new FeedMedia(item, "url", 1024, "mime/type");
        item.setMedia(media);
        List<FeedItem> list = new ArrayList<>();
        list.add(item);
        feed.setItems(list);

        final Feed newFeed = DBTasks.updateFeed(context, feed, false);
        assertNotSame(newFeed, feed);

        final Feed feedFromDB = DBReader.getFeed(newFeed.getId());
        final FeedItem feedItemFromDB = feedFromDB.getItems().get(0);
        assertTrue("state: " + feedItemFromDB.getState(), feedItemFromDB.isNew());
    }

    @Test
    public void testUpdateFeedRemoveUnlistedItems() {
        final Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < 10; i++) {
            feed.getItems().add(
                    new FeedItem(0, "item " + i, "id " + i, "link " + i, new Date(i), FeedItem.PLAYED, feed));
        }
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        // delete some items
        feed.getItems().subList(0, 2).clear();
        Feed newFeed = DBTasks.updateFeed(context, feed, true);
        assertEquals(8, newFeed.getItems().size()); // 10 - 2 = 8 items

        Feed feedFromDB = DBReader.getFeed(newFeed.getId());
        assertEquals(8, feedFromDB.getItems().size()); // 10 - 2 = 8 items
    }

    @SuppressWarnings("SameParameterValue")
    private void updatedFeedTest(final Feed newFeed, long feedID, List<Long> itemIDs,
                                 int numItemsOld, int numItemsNew) {
        assertEquals(feedID, newFeed.getId());
        assertEquals(numItemsNew + numItemsOld, newFeed.getItems().size());
        Collections.reverse(newFeed.getItems());
        Date lastDate = new Date(0);
        for (int i = 0; i < numItemsOld; i++) {
            FeedItem item = newFeed.getItems().get(i);
            assertSame(newFeed, item.getFeed());
            assertEquals((long) itemIDs.get(i), item.getId());
            assertTrue(item.isPlayed());
            assertTrue(item.getPubDate().getTime() >= lastDate.getTime());
            lastDate = item.getPubDate();
        }
        for (int i = numItemsOld; i < numItemsNew + numItemsOld; i++) {
            FeedItem item = newFeed.getItems().get(i);
            assertSame(newFeed, item.getFeed());
            assertTrue(item.getId() != 0);
            assertFalse(item.isPlayed());
            assertTrue(item.getPubDate().getTime() >= lastDate.getTime());
            lastDate = item.getPubDate();
        }
    }

    @Test
    public void testAddQueueItemsInDownload_EnqueueEnabled() throws Exception {
        // Setup test data / environment
        UserPreferences.setEnqueueDownloadedEpisodes(true);
        UserPreferences.setEnqueueLocation(UserPreferences.EnqueueLocation.BACK);

        List<FeedItem> fis1 = createSavedFeed("Feed 1", 2).getItems();
        List<FeedItem> fis2 = createSavedFeed("Feed 2", 3).getItems();

        DBWriter.addQueueItem(context, fis1.get(0), fis2.get(0)).get();
        // the first item fis1.get(0) is already in the queue
        FeedItem[] itemsToDownload = new FeedItem[]{ fis1.get(0), fis1.get(1), fis2.get(2), fis2.get(1) };

        // Expectations:
        List<FeedItem> expectedEnqueued = Arrays.asList(fis1.get(1), fis2.get(2), fis2.get(1));
        List<FeedItem> expectedQueue = new ArrayList<>();
        expectedQueue.addAll(DBReader.getQueue());
        expectedQueue.addAll(expectedEnqueued);

        // Run actual test and assert results
        List<? extends FeedItem> actualEnqueued =
                DBTasks.enqueueFeedItemsToDownload(context, Arrays.asList(itemsToDownload));

        assertEqualsByIds("Only items not in the queue are enqueued", expectedEnqueued, actualEnqueued);
        assertEqualsByIds("Queue has new items appended", expectedQueue, DBReader.getQueue());
    }

    @Test
    public void testAddQueueItemsInDownload_EnqueueDisabled() throws Exception {
        // Setup test data / environment
        UserPreferences.setEnqueueDownloadedEpisodes(false);

        List<FeedItem> fis1 = createSavedFeed("Feed 1", 2).getItems();
        List<FeedItem> fis2 = createSavedFeed("Feed 2", 3).getItems();

        DBWriter.addQueueItem(context, fis1.get(0), fis2.get(0)).get();
        FeedItem[] itemsToDownload = new FeedItem[]{ fis1.get(0), fis1.get(1), fis2.get(2), fis2.get(1) };

        // Expectations:
        List<FeedItem> expectedEnqueued = Collections.emptyList();
        List<FeedItem> expectedQueue = DBReader.getQueue();

        // Run actual test and assert results
        List<? extends FeedItem> actualEnqueued =
                DBTasks.enqueueFeedItemsToDownload(context, Arrays.asList(itemsToDownload));

        assertEqualsByIds("No item is enqueued", expectedEnqueued, actualEnqueued);
        assertEqualsByIds("Queue is unchanged", expectedQueue, DBReader.getQueue());
    }

    private void assertEqualsByIds(String msg, List<? extends FeedItem> expected, List<? extends FeedItem> actual) {
        // assert only the IDs, so that any differences are easily to spot.
        List<Long> expectedIds = getIdList(expected);
        List<Long> actualIds = getIdList(actual);
        assertEquals(msg, expectedIds, actualIds);
    }

    private Feed createSavedFeed(String title, int numFeedItems) {
        final Feed feed = new Feed("url", null, title);

        if (numFeedItems > 0) {
            List<FeedItem> items = new ArrayList<>(numFeedItems);
            for (int i = 1; i <= numFeedItems; i++) {
                FeedItem item = new FeedItem(0, "item " + i + " of " + title, "id", "link",
                        new Date(), FeedItem.UNPLAYED, feed);
                items.add(item);
            }
            feed.setItems(items);
        }

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();
        return feed;
    }

}
