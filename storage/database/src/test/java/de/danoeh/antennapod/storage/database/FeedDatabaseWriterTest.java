package de.danoeh.antennapod.storage.database;

import android.content.Context;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueueStub;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class FeedDatabaseWriterTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        UserPreferences.init(context);
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();
        SynchronizationQueue.setInstance(new SynchronizationQueueStub());
    }

    @Test
    public void testStoreNewFeed() {
        Feed feed = createFeed();
        for (int i = 0; i < 3; i++) {
            feed.getItems().add(createItem("item-" + i, "Item " + i, feed));
        }
        Feed updatedFeed = FeedDatabaseWriter.updateFeed(context, feed, false);
        List<FeedItem> storedItems = DBReader.getFeedItemList(updatedFeed, FeedItemFilter.unfiltered(),
                SortOrder.EPISODE_TITLE_A_Z, 0, Integer.MAX_VALUE);
        assertEquals(3, storedItems.size());
        for (int i = 0; i < 3; i++) {
            assertEquals("item-" + i, storedItems.get(i).getItemIdentifier());
        }
    }

    @Test
    public void testAddItemsToExistingFeed() {
        Feed feed = createFeed();
        for (int i = 0; i < 3; i++) {
            feed.getItems().add(createItem("item-" + i, "Item " + i, feed));
        }
        feed = FeedDatabaseWriter.updateFeed(context, feed, false);

        Feed updatedFeed = createFeed();
        updatedFeed.setId(feed.getId());
        for (int i = 3; i < 6; i++) {
            updatedFeed.getItems().add(createItem("item-" + i, "Item " + i, feed));
        }
        FeedDatabaseWriter.updateFeed(context, updatedFeed, false);

        List<FeedItem> dbItems = DBReader.getFeedItemList(feed, FeedItemFilter.unfiltered(),
                SortOrder.EPISODE_TITLE_A_Z, 0, Integer.MAX_VALUE);
        assertEquals(6, dbItems.size());
        for (int i = 0; i < 6; i++) {
            assertEquals("item-" + i, dbItems.get(i).getItemIdentifier());
        }
    }

    @Test
    public void testAddOrUpdateItems() throws ExecutionException, InterruptedException {
        Feed feed = createFeed();
        for (int i = 0; i < 3; i++) {
            feed.getItems().add(createItem("item-" + i, "Item " + i, feed));
        }
        feed = FeedDatabaseWriter.updateFeed(context, feed, false);
        DBReader.getFeedItemList(feed, FeedItemFilter.unfiltered(),
                SortOrder.EPISODE_TITLE_A_Z, 0, Integer.MAX_VALUE);
        DBWriter.markItemPlayed(FeedItem.PLAYED, false, feed.getItems().get(2)).get();

        Feed updatedFeed = createFeed();
        updatedFeed.setId(feed.getId());
        for (int i = 2; i < 5; i++) {
            updatedFeed.getItems().add(createItem("item-" + i, "Item " + i, feed));
        }
        FeedDatabaseWriter.updateFeed(context, updatedFeed, false);

        List<FeedItem> dbItems = DBReader.getFeedItemList(feed, FeedItemFilter.unfiltered(),
                SortOrder.EPISODE_TITLE_A_Z, 0, Integer.MAX_VALUE);
        assertEquals(5, dbItems.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("item-" + i, dbItems.get(i).getItemIdentifier());
        }
        assertEquals(FeedItem.PLAYED, dbItems.get(2).getPlayState());
    }

    @Test
    public void testDuplicateItemsInFeed() {
        Feed feed = createFeed();
        feed.getItems().add(createItem("id1", "Duplicate Title", feed));
        feed.getItems().add(createItem("id2", "Duplicate Title", feed));
        FeedDatabaseWriter.updateFeed(context, feed, false); // First update just takes the feed without complaining
        FeedDatabaseWriter.updateFeed(context, feed, false);

        List<DownloadResult> downloadLog = DBReader.getDownloadLog();
        assertEquals(1, downloadLog.size());
        assertEquals(DownloadError.ERROR_PARSER_EXCEPTION_DUPLICATE, downloadLog.get(0).getReason());
    }

    @Test
    public void testGuidUpdated() {
        Feed feed = createFeed();
        feed.getItems().add(createItem("old-id", "Unique Title", feed));
        FeedDatabaseWriter.updateFeed(context, feed, false);

        Feed newFeed = createFeed();
        newFeed.getItems().add(createItem("new-id", "Unique Title", newFeed));
        Feed stored = FeedDatabaseWriter.updateFeed(context, newFeed, false);

        assertEquals(1, stored.getItems().size());
        assertEquals("new-id", stored.getItems().get(0).getItemIdentifier());
    }

    @Test
    public void testUpdateFeedNewFeed() {
        final int numItems = 10;

        Feed feed = createFeed();
        for (int i = 0; i < numItems; i++) {
            feed.getItems().add(new FeedItem(0, "item " + i, "id " + i, "link " + i,
                    new Date(), FeedItem.UNPLAYED, feed));
        }
        Feed newFeed = FeedDatabaseWriter.updateFeed(context, feed, false);

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
        Feed feed1 = createFeed();
        Feed feed2 = createFeed();
        feed2.setDownloadUrl("different url");

        Feed savedFeed1 = FeedDatabaseWriter.updateFeed(context, feed1, false);
        Feed savedFeed2 = FeedDatabaseWriter.updateFeed(context, feed2, false);

        assertTrue(savedFeed1.getId() != savedFeed2.getId());
    }

    @Test
    public void testUpdateFeedUpdatedFeed() {
        final int numItemsOld = 10;
        final int numItemsNew = 10;

        final Feed feed = createFeed();
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

        final Feed newFeed = FeedDatabaseWriter.updateFeed(context, feed, false);
        assertNotSame(newFeed, feed);

        updatedFeedTest(newFeed, feedID, itemIDs, numItemsOld, numItemsNew);

        final Feed feedFromDB = DBReader.getFeed(newFeed.getId(), false, 0, Integer.MAX_VALUE);
        assertNotNull(feedFromDB);
        assertEquals(newFeed.getId(), feedFromDB.getId());
        updatedFeedTest(feedFromDB, feedID, itemIDs, numItemsOld, numItemsNew);
    }

    @Test
    public void testUpdateFeedMediaUrlResetState() {
        final Feed feed = createFeed();
        FeedItem item = new FeedItem(0, "item", "id", "link", new Date(), FeedItem.PLAYED, feed);
        feed.setItems(Collections.singletonList(item));

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

        final Feed newFeed = FeedDatabaseWriter.updateFeed(context, feed, false);
        assertNotSame(newFeed, feed);

        final Feed feedFromDB = DBReader.getFeed(newFeed.getId(), false, 0, Integer.MAX_VALUE);
        final FeedItem feedItemFromDB = feedFromDB.getItems().get(0);
        assertTrue(feedItemFromDB.isNew());
    }

    @Test
    public void testUpdateFeedRemoveUnlistedItems() {
        final Feed feed = createFeed();
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
        Feed newFeed = FeedDatabaseWriter.updateFeed(context, feed, true);
        assertEquals(8, newFeed.getItems().size()); // 10 - 2 = 8 items

        Feed feedFromDB = DBReader.getFeed(newFeed.getId(), false, 0, Integer.MAX_VALUE);
        assertEquals(8, feedFromDB.getItems().size()); // 10 - 2 = 8 items
    }

    @Test
    public void testUpdateFeedSetDuplicate() {
        final Feed feed = createFeed();
        for (int i = 0; i < 10; i++) {
            FeedItem item =
                    new FeedItem(0, "item " + i, "id " + i, "link " + i, new Date(i), FeedItem.PLAYED, feed);
            FeedMedia media = new FeedMedia(item, "download url " + i, 123, "media/mp3");
            item.setMedia(media);
            feed.getItems().add(item);
        }
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        // change the guid of the first item, but leave the download url the same
        FeedItem item = feed.getItemAtIndex(0);
        item.setItemIdentifier("id 0-duplicate");
        item.setTitle("item 0 duplicate");
        Feed newFeed = FeedDatabaseWriter.updateFeed(context, feed, false);
        assertEquals(10, newFeed.getItems().size()); // id 1-duplicate replaces because the stream url is the same

        Feed feedFromDB = DBReader.getFeed(newFeed.getId(), false, 0, Integer.MAX_VALUE);
        assertEquals(10, feedFromDB.getItems().size()); // id1-duplicate should override id 1

        FeedItem updatedItem = feedFromDB.getItemAtIndex(9);
        assertEquals("item 0 duplicate", updatedItem.getTitle());
        assertEquals("id 0-duplicate", updatedItem.getItemIdentifier()); // Should use the new ID for sync etc
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

    private Feed createFeed() {
        Feed feed = new Feed("url", null, null);
        feed.setItems(new ArrayList<>());
        return feed;
    }

    private FeedItem createItem(String identifier, String title, Feed feed) {
        FeedItem item = new FeedItem();
        item.setItemIdentifier(identifier);
        item.setTitle(title);
        item.setMedia(new FeedMedia(item, "url-" + title, 2, "mime"));
        item.setFeed(feed);
        return item;
    }
}
