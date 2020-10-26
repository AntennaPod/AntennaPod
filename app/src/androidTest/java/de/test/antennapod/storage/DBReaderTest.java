package de.test.antennapod.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.LongList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static de.test.antennapod.storage.DBTestUtils.saveFeedlist;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for DBReader
 */
@SmallTest
public class DBReaderTest {

    @After
    public void tearDown() throws Exception {
        assertTrue(PodDBAdapter.deleteDatabase());
    }

    @Before
    public void setUp() throws Exception {
        // create new database
        PodDBAdapter.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();
    }

    @Test
    public void testGetFeedList() {
        List<Feed> feeds = saveFeedlist(10, 0, false);
        List<Feed> savedFeeds = DBReader.getFeedList();
        assertNotNull(savedFeeds);
        assertEquals(feeds.size(), savedFeeds.size());
        for (int i = 0; i < feeds.size(); i++) {
            assertEquals(feeds.get(i).getId(), savedFeeds.get(i).getId());
        }
    }

    @Test
    public void testGetFeedListSortOrder() {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();

        Feed feed1 = new Feed(0, null, "A", "link", "d", null, null, null, "rss", "A", null, "", "", true);
        Feed feed2 = new Feed(0, null, "b", "link", "d", null, null, null, "rss", "b", null, "", "", true);
        Feed feed3 = new Feed(0, null, "C", "link", "d", null, null, null, "rss", "C", null, "", "", true);
        Feed feed4 = new Feed(0, null, "d", "link", "d", null, null, null, "rss", "d", null, "", "", true);
        adapter.setCompleteFeed(feed1);
        adapter.setCompleteFeed(feed2);
        adapter.setCompleteFeed(feed3);
        adapter.setCompleteFeed(feed4);
        assertTrue(feed1.getId() != 0);
        assertTrue(feed2.getId() != 0);
        assertTrue(feed3.getId() != 0);
        assertTrue(feed4.getId() != 0);

        adapter.close();

        List<Feed> saved = DBReader.getFeedList();
        assertNotNull(saved);
        assertEquals("Wrong size: ", 4, saved.size());

        assertEquals("Wrong id of feed 1: ", feed1.getId(), saved.get(0).getId());
        assertEquals("Wrong id of feed 2: ", feed2.getId(), saved.get(1).getId());
        assertEquals("Wrong id of feed 3: ", feed3.getId(), saved.get(2).getId());
        assertEquals("Wrong id of feed 4: ", feed4.getId(), saved.get(3).getId());
    }

    @Test
    public void testFeedListDownloadUrls() {
        List<Feed> feeds = saveFeedlist(10, 0, false);
        List<String> urls = DBReader.getFeedListDownloadUrls();
        assertNotNull(urls);
        assertEquals(feeds.size(), urls.size());
        for (int i = 0; i < urls.size(); i++) {
            assertEquals(urls.get(i), feeds.get(i).getDownload_url());
        }
    }

    @Test
    public void testLoadFeedDataOfFeedItemlist() {
        final int numFeeds = 10;
        final int numItems = 1;
        List<Feed> feeds = saveFeedlist(numFeeds, numItems, false);
        List<FeedItem> items = new ArrayList<>();
        for (Feed f : feeds) {
            for (FeedItem item : f.getItems()) {
                item.setFeed(null);
                item.setFeedId(f.getId());
                items.add(item);
            }
        }
        DBReader.loadAdditionalFeedItemListData(items);
        for (int i = 0; i < numFeeds; i++) {
            for (int j = 0; j < numItems; j++) {
                FeedItem item = feeds.get(i).getItems().get(j);
                assertNotNull(item.getFeed());
                assertEquals(feeds.get(i).getId(), item.getFeed().getId());
                assertEquals(item.getFeed().getId(), item.getFeedId());
            }
        }
    }

    @Test
    public void testGetFeedItemList() {
        final int numFeeds = 1;
        final int numItems = 10;
        Feed feed = saveFeedlist(numFeeds, numItems, false).get(0);
        List<FeedItem> items = feed.getItems();
        feed.setItems(null);
        List<FeedItem> savedItems = DBReader.getFeedItemList(feed);
        assertNotNull(savedItems);
        assertEquals(items.size(), savedItems.size());
        for (int i = 0; i < savedItems.size(); i++) {
            assertEquals(savedItems.get(i).getId(), items.get(i).getId());
        }
    }

    private List<FeedItem> saveQueue(int numItems) {
        if (numItems <= 0) {
            throw new IllegalArgumentException("numItems<=0");
        }
        List<Feed> feeds = saveFeedlist(numItems, numItems, false);
        List<FeedItem> allItems = new ArrayList<>();
        for (Feed f : feeds) {
            allItems.addAll(f.getItems());
        }
        // take random items from every feed
        Random random = new Random();
        List<FeedItem> queue = new ArrayList<>();
        while (queue.size() < numItems) {
            int index = random.nextInt(numItems);
            if (!queue.contains(allItems.get(index))) {
                queue.add(allItems.get(index));
            }
        }
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setQueue(queue);
        adapter.close();
        return queue;
    }

    @Test
    public void testGetQueueIDList() {
        final int numItems = 10;
        List<FeedItem> queue = saveQueue(numItems);
        LongList ids = DBReader.getQueueIDList();
        assertNotNull(ids);
        assertEquals(ids.size(), queue.size());
        for (int i = 0; i < queue.size(); i++) {
            assertTrue(ids.get(i) != 0);
            assertEquals(ids.get(i), queue.get(i).getId());
        }
    }

    @Test
    public void testGetQueue() {
        final int numItems = 10;
        List<FeedItem> queue = saveQueue(numItems);
        List<FeedItem> savedQueue = DBReader.getQueue();
        assertNotNull(savedQueue);
        assertEquals(savedQueue.size(), queue.size());
        for (int i = 0; i < queue.size(); i++) {
            assertTrue(savedQueue.get(i).getId() != 0);
            assertEquals(savedQueue.get(i).getId(), queue.get(i).getId());
        }
    }

    private List<FeedItem> saveDownloadedItems(int numItems) {
        if (numItems <= 0) {
            throw new IllegalArgumentException("numItems<=0");
        }
        List<Feed> feeds = saveFeedlist(numItems, numItems, true);
        List<FeedItem> items = new ArrayList<>();
        for (Feed f : feeds) {
            items.addAll(f.getItems());
        }
        List<FeedItem> downloaded = new ArrayList<>();
        Random random = new Random();

        while (downloaded.size() < numItems) {
            int i = random.nextInt(numItems);
            if (!downloaded.contains(items.get(i))) {
                FeedItem item = items.get(i);
                item.getMedia().setDownloaded(true);
                item.getMedia().setFile_url("file" + i);
                downloaded.add(item);
            }
        }
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setFeedItemlist(downloaded);
        adapter.close();
        return downloaded;
    }

    @Test
    public void testGetDownloadedItems() {
        final int numItems = 10;
        List<FeedItem> downloaded = saveDownloadedItems(numItems);
        List<FeedItem> downloaded_saved = DBReader.getDownloadedItems();
        assertNotNull(downloaded_saved);
        assertEquals(downloaded.size(), downloaded_saved.size());
        for (FeedItem item : downloaded_saved) {
            assertNotNull(item.getMedia());
            assertTrue(item.getMedia().isDownloaded());
            assertNotNull(item.getMedia().getDownload_url());
        }
    }

    private List<FeedItem> saveNewItems(int numItems) {
        List<Feed> feeds = saveFeedlist(numItems, numItems, true);
        List<FeedItem> items = new ArrayList<>();
        for (Feed f : feeds) {
            items.addAll(f.getItems());
        }
        List<FeedItem> newItems = new ArrayList<>();
        Random random = new Random();

        while (newItems.size() < numItems) {
            int i = random.nextInt(numItems);
            if (!newItems.contains(items.get(i))) {
                FeedItem item = items.get(i);
                item.setNew();
                newItems.add(item);
            }
        }
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setFeedItemlist(newItems);
        adapter.close();
        return newItems;
    }

    @Test
    public void testGetNewItemIds() {
        final int numItems = 10;

        List<FeedItem> newItems = saveNewItems(numItems);
        long[] unreadIds = new long[newItems.size()];
        for (int i = 0; i < newItems.size(); i++) {
            unreadIds[i] = newItems.get(i).getId();
        }
        List<FeedItem> newItemsSaved = DBReader.getNewItemsList(0, Integer.MAX_VALUE);
        assertNotNull(newItemsSaved);
        assertEquals(newItemsSaved.size(), newItems.size());
        for (FeedItem feedItem : newItemsSaved) {
            long savedId = feedItem.getId();
            boolean found = false;
            for (long id : unreadIds) {
                if (id == savedId) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    @Test
    public void testGetPlaybackHistory() {
        final int numItems = (DBReader.PLAYBACK_HISTORY_SIZE + 1) * 2;
        final int playedItems = DBReader.PLAYBACK_HISTORY_SIZE + 1;
        final int numReturnedItems = Math.min(playedItems, DBReader.PLAYBACK_HISTORY_SIZE);
        final int numFeeds = 1;

        Feed feed = DBTestUtils.saveFeedlist(numFeeds, numItems, true).get(0);
        long[] ids = new long[playedItems];

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        for (int i = 0; i < playedItems; i++) {
            FeedMedia m = feed.getItems().get(i).getMedia();
            m.setPlaybackCompletionDate(new Date(i + 1));
            adapter.setFeedMediaPlaybackCompletionDate(m);
            ids[ids.length - 1 - i] = m.getItem().getId();
        }
        adapter.close();

        List<FeedItem> saved = DBReader.getPlaybackHistory();
        assertNotNull(saved);
        assertEquals("Wrong size: ", numReturnedItems, saved.size());
        for (int i = 0; i < numReturnedItems; i++) {
            FeedItem item = saved.get(i);
            assertNotNull(item.getMedia().getPlaybackCompletionDate());
            assertEquals("Wrong sort order: ", item.getId(), ids[i]);
        }
    }

    @Test
    public void testGetNavDrawerDataQueueEmptyNoUnreadItems() {
        final int NUM_FEEDS = 10;
        final int NUM_ITEMS = 10;
        DBTestUtils.saveFeedlist(NUM_FEEDS, NUM_ITEMS, true);
        DBReader.NavDrawerData navDrawerData = DBReader.getNavDrawerData();
        assertEquals(NUM_FEEDS, navDrawerData.feeds.size());
        assertEquals(0, navDrawerData.numNewItems);
        assertEquals(0, navDrawerData.queueSize);
    }

    @Test
    public void testGetNavDrawerDataQueueNotEmptyWithUnreadItems() {
        final int NUM_FEEDS = 10;
        final int NUM_ITEMS = 10;
        final int NUM_QUEUE = 1;
        final int NUM_NEW = 2;
        List<Feed> feeds = DBTestUtils.saveFeedlist(NUM_FEEDS, NUM_ITEMS, true);
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        for (int i = 0; i < NUM_NEW; i++) {
            FeedItem item = feeds.get(0).getItems().get(i);
            item.setNew();
            adapter.setSingleFeedItem(item);
        }
        List<FeedItem> queue = new ArrayList<>();
        for (int i = 0; i < NUM_QUEUE; i++) {
            FeedItem item = feeds.get(1).getItems().get(i);
            queue.add(item);
        }
        adapter.setQueue(queue);

        adapter.close();

        DBReader.NavDrawerData navDrawerData = DBReader.getNavDrawerData();
        assertEquals(NUM_FEEDS, navDrawerData.feeds.size());
        assertEquals(NUM_NEW, navDrawerData.numNewItems);
        assertEquals(NUM_QUEUE, navDrawerData.queueSize);
    }

    @Test
    public void testGetFeedItemlistCheckChaptersFalse() throws Exception {
        List<Feed> feeds = DBTestUtils.saveFeedlist(10, 10, false, false, 0);
        for (Feed feed : feeds) {
            for (FeedItem item : feed.getItems()) {
                assertFalse(item.hasChapters());
            }
        }
    }

    @Test
    public void testGetFeedItemlistCheckChaptersTrue() throws Exception {
        List<Feed> feeds = saveFeedlist(10, 10, false, true, 10);
        for (Feed feed : feeds) {
            for (FeedItem item : feed.getItems()) {
                assertTrue(item.hasChapters());
            }
        }
    }

    @Test
    public void testLoadChaptersOfFeedItemNoChapters() throws Exception {
        List<Feed> feeds = saveFeedlist(1, 3, false, false, 0);
        saveFeedlist(1, 3, false, true, 3);
        for (Feed feed : feeds) {
            for (FeedItem item : feed.getItems()) {
                assertFalse(item.hasChapters());
                item.setChapters(DBReader.loadChaptersOfFeedItem(item));
                assertFalse(item.hasChapters());
                assertNull(item.getChapters());
            }
        }
    }

    @Test
    public void testLoadChaptersOfFeedItemWithChapters() throws Exception {
        final int NUM_CHAPTERS = 3;
        DBTestUtils.saveFeedlist(1, 3, false, false, 0);
        List<Feed> feeds = saveFeedlist(1, 3, false, true, NUM_CHAPTERS);
        for (Feed feed : feeds) {
            for (FeedItem item : feed.getItems()) {
                assertTrue(item.hasChapters());
                item.setChapters(DBReader.loadChaptersOfFeedItem(item));
                assertTrue(item.hasChapters());
                assertNotNull(item.getChapters());
                assertEquals(NUM_CHAPTERS, item.getChapters().size());
            }
        }
    }

    @Test
    public void testGetItemWithChapters() throws Exception {
        final int NUM_CHAPTERS = 3;
        List<Feed> feeds = saveFeedlist(1, 1, false, true, NUM_CHAPTERS);
        FeedItem item1 = feeds.get(0).getItems().get(0);
        FeedItem item2 = DBReader.getFeedItem(item1.getId());
        item2.setChapters(DBReader.loadChaptersOfFeedItem(item2));
        assertTrue(item2.hasChapters());
        assertEquals(item1.getChapters(), item2.getChapters());
    }
}
