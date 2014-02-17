package instrumentationTest.de.test.antennapod.storage;

import android.content.Context;
import android.test.InstrumentationTestCase;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.PodDBAdapter;
import de.danoeh.antennapod.util.flattr.FlattrStatus;
import static instrumentationTest.de.test.antennapod.storage.DBTestUtils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static instrumentationTest.de.test.antennapod.storage.DBTestUtils.saveFeedlist;

/**
 * Test class for DBReader
 */
public class DBReaderTest extends InstrumentationTestCase {

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        final Context context = getInstrumentation().getTargetContext();
        assertTrue(PodDBAdapter.deleteDatabase(context));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final Context context = getInstrumentation().getTargetContext();
        context.deleteDatabase(PodDBAdapter.DATABASE_NAME);
        // make sure database is created
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.close();
    }

    private void expiredFeedListTestHelper(long lastUpdate, long expirationTime, boolean shouldReturn) {
        final Context context = getInstrumentation().getTargetContext();
        Feed feed = new Feed(0, new Date(lastUpdate), "feed", "link", "descr", null,
                null, null, null, "feed", null, null, "url", false, new FlattrStatus());
        feed.setItems(new ArrayList<FeedItem>());
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();

        assertTrue(feed.getId() != 0);
        List<Feed> expiredFeeds = DBReader.getExpiredFeedsList(context, expirationTime);
        assertNotNull(expiredFeeds);
        if (shouldReturn) {
            assertTrue(expiredFeeds.size() == 1);
            assertTrue(expiredFeeds.get(0).getId() == feed.getId());
        } else {
            assertTrue(expiredFeeds.isEmpty());
        }
    }

    public void testGetExpiredFeedsListShouldReturnFeed() {
        final long expirationTime = 1000 * 60 * 60; // 1 hour
        expiredFeedListTestHelper(System.currentTimeMillis() - expirationTime - 1, expirationTime, true);
    }

    public void testGetExpiredFeedsListShouldNotReturnFeed() {
        final long expirationTime = 1000 * 60 * 60; // 1 hour
        expiredFeedListTestHelper(System.currentTimeMillis() - expirationTime / 2, expirationTime, false);
    }


    public void testGetFeedList() {
        final Context context = getInstrumentation().getTargetContext();
        List<Feed> feeds = saveFeedlist(context, 10, 0, false);
        List<Feed> savedFeeds = DBReader.getFeedList(context);
        assertNotNull(savedFeeds);
        assertTrue(savedFeeds.size() == feeds.size());
        for (int i = 0; i < feeds.size(); i++) {
            assertTrue(savedFeeds.get(i).getId() == feeds.get(i).getId());
        }
    }

    public void testGetFeedListSortOrder() {
        final Context context = getInstrumentation().getTargetContext();
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();

        Feed feed1 = new Feed(0, new Date(), "A", "link", "d", null, null, null, "rss", "A", null, "", "", true);
        Feed feed2 = new Feed(0, new Date(), "b", "link", "d", null, null, null, "rss", "b", null, "", "", true);
        Feed feed3 = new Feed(0, new Date(), "C", "link", "d", null, null, null, "rss", "C", null, "", "", true);
        Feed feed4 = new Feed(0, new Date(), "d", "link", "d", null, null, null, "rss", "d", null, "", "", true);
        adapter.setCompleteFeed(feed1);
        adapter.setCompleteFeed(feed2);
        adapter.setCompleteFeed(feed3);
        adapter.setCompleteFeed(feed4);
        assertTrue(feed1.getId() != 0);
        assertTrue(feed2.getId() != 0);
        assertTrue(feed3.getId() != 0);
        assertTrue(feed4.getId() != 0);

        adapter.close();

        List<Feed> saved = DBReader.getFeedList(context);
        assertNotNull(saved);
        assertEquals("Wrong size: ", 4, saved.size());

        assertEquals("Wrong id of feed 1: ", feed1.getId(), saved.get(0).getId());
        assertEquals("Wrong id of feed 2: ", feed2.getId(), saved.get(1).getId());
        assertEquals("Wrong id of feed 3: ", feed3.getId(), saved.get(2).getId());
        assertEquals("Wrong id of feed 4: ", feed4.getId(), saved.get(3).getId());
    }

    public void testFeedListDownloadUrls() {
        final Context context = getInstrumentation().getTargetContext();
        List<Feed> feeds = saveFeedlist(context, 10, 0, false);
        List<String> urls = DBReader.getFeedListDownloadUrls(context);
        assertNotNull(urls);
        assertTrue(urls.size() == feeds.size());
        for (int i = 0; i < urls.size(); i++) {
            assertEquals(urls.get(i), feeds.get(i).getDownload_url());
        }
    }

    public void testLoadFeedDataOfFeedItemlist() {
        final Context context = getInstrumentation().getTargetContext();
        final int numFeeds = 10;
        final int numItems = 1;
        List<Feed> feeds = saveFeedlist(context, numFeeds, numItems, false);
        List<FeedItem> items = new ArrayList<FeedItem>();
        for (Feed f : feeds) {
            for (FeedItem item : f.getItems()) {
                item.setFeed(null);
                item.setFeedId(f.getId());
                items.add(item);
            }
        }
        DBReader.loadFeedDataOfFeedItemlist(context, items);
        for (int i = 0; i < numFeeds; i++) {
            for (int j = 0; j < numItems; j++) {
                FeedItem item = feeds.get(i).getItems().get(j);
                assertNotNull(item.getFeed());
                assertTrue(item.getFeed().getId() == feeds.get(i).getId());
                assertTrue(item.getFeedId() == item.getFeed().getId());
            }
        }
    }

    public void testGetFeedItemList() {
        final Context context = getInstrumentation().getTargetContext();
        final int numFeeds = 1;
        final int numItems = 10;
        Feed feed = saveFeedlist(context, numFeeds, numItems, false).get(0);
        List<FeedItem> items = feed.getItems();
        feed.setItems(null);
        List<FeedItem> savedItems = DBReader.getFeedItemList(context, feed);
        assertNotNull(savedItems);
        assertTrue(savedItems.size() == items.size());
        for (int i = 0; i < savedItems.size(); i++) {
            assertTrue(items.get(i).getId() == savedItems.get(i).getId());
        }
    }

    private List<FeedItem> saveQueue(int numItems) {
        if (numItems <= 0) {
            throw new IllegalArgumentException("numItems<=0");
        }
        final Context context = getInstrumentation().getTargetContext();
        List<Feed> feeds = saveFeedlist(context, numItems, numItems, false);
        List<FeedItem> allItems = new ArrayList<FeedItem>();
        for (Feed f : feeds) {
            allItems.addAll(f.getItems());
        }
        // take random items from every feed
        Random random = new Random();
        List<FeedItem> queue = new ArrayList<FeedItem>();
        while (queue.size() < numItems) {
            int index = random.nextInt(numItems);
            if (!queue.contains(allItems.get(index))) {
                queue.add(allItems.get(index));
            }
        }
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setQueue(queue);
        adapter.close();
        return queue;
    }

    public void testGetQueueIDList() {
        final Context context = getInstrumentation().getTargetContext();
        final int numItems = 10;
        List<FeedItem> queue = saveQueue(numItems);
        List<Long> ids = DBReader.getQueueIDList(context);
        assertNotNull(ids);
        assertTrue(queue.size() == ids.size());
        for (int i = 0; i < queue.size(); i++) {
            assertTrue(ids.get(i) != 0);
            assertTrue(queue.get(i).getId() == ids.get(i));
        }
    }

    public void testGetQueue() {
        final Context context = getInstrumentation().getTargetContext();
        final int numItems = 10;
        List<FeedItem> queue = saveQueue(numItems);
        List<FeedItem> savedQueue = DBReader.getQueue(context);
        assertNotNull(savedQueue);
        assertTrue(queue.size() == savedQueue.size());
        for (int i = 0; i < queue.size(); i++) {
            assertTrue(savedQueue.get(i).getId() != 0);
            assertTrue(queue.get(i).getId() == savedQueue.get(i).getId());
        }
    }

    private List<FeedItem> saveDownloadedItems(int numItems) {
        if (numItems <= 0) {
            throw new IllegalArgumentException("numItems<=0");
        }
        final Context context = getInstrumentation().getTargetContext();
        List<Feed> feeds = saveFeedlist(context, numItems, numItems, true);
        List<FeedItem> items = new ArrayList<FeedItem>();
        for (Feed f : feeds) {
            items.addAll(f.getItems());
        }
        List<FeedItem> downloaded = new ArrayList<FeedItem>();
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
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setFeedItemlist(downloaded);
        adapter.close();
        return downloaded;
    }

    public void testGetDownloadedItems() {
        final Context context = getInstrumentation().getTargetContext();
        final int numItems = 10;
        List<FeedItem> downloaded = saveDownloadedItems(numItems);
        List<FeedItem> downloaded_saved = DBReader.getDownloadedItems(context);
        assertNotNull(downloaded_saved);
        assertTrue(downloaded_saved.size() == downloaded.size());
        for (FeedItem item : downloaded_saved) {
            assertNotNull(item.getMedia());
            assertTrue(item.getMedia().isDownloaded());
            assertNotNull(item.getMedia().getDownload_url());
        }
    }

    private List<FeedItem> saveUnreadItems(int numItems) {
        if (numItems <= 0) {
            throw new IllegalArgumentException("numItems<=0");
        }
        final Context context = getInstrumentation().getTargetContext();
        List<Feed> feeds = saveFeedlist(context, numItems, numItems, true);
        List<FeedItem> items = new ArrayList<FeedItem>();
        for (Feed f : feeds) {
            items.addAll(f.getItems());
        }
        List<FeedItem> unread = new ArrayList<FeedItem>();
        Random random = new Random();

        while (unread.size() < numItems) {
            int i = random.nextInt(numItems);
            if (!unread.contains(items.get(i))) {
                FeedItem item = items.get(i);
                item.setRead(false);
                unread.add(item);
            }
        }
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        adapter.setFeedItemlist(unread);
        adapter.close();
        return unread;
    }

    public void testGetUnreadItemsList() {
        final Context context = getInstrumentation().getTargetContext();
        final int numItems = 10;

        List<FeedItem> unread = saveUnreadItems(numItems);
        List<FeedItem> unreadSaved = DBReader.getUnreadItemsList(context);
        assertNotNull(unreadSaved);
        assertTrue(unread.size() == unreadSaved.size());
        for (FeedItem item : unreadSaved) {
            assertFalse(item.isRead());
        }
    }

    public void testGetUnreadItemIds() {
        final Context context = getInstrumentation().getTargetContext();
        final int numItems = 10;

        List<FeedItem> unread = saveUnreadItems(numItems);
        long[] unreadIds = new long[unread.size()];
        for (int i = 0; i < unread.size(); i++) {
            unreadIds[i] = unread.get(i).getId();
        }
        long[] unreadSaved = DBReader.getUnreadItemIds(context);
        assertNotNull(unreadSaved);
        assertTrue(unread.size() == unreadSaved.length);
        for (long savedId : unreadSaved) {
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

    public void testGetPlaybackHistory() {
        final Context context = getInstrumentation().getTargetContext();
        final int numItems = 10;
        final int playedItems = 5;
        final int numFeeds = 1;

        Feed feed = DBTestUtils.saveFeedlist(context, numFeeds, numItems, true).get(0);
        long[] ids = new long[playedItems];

        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        for (int i = 0; i < playedItems; i++) {
            FeedMedia m = feed.getItems().get(i).getMedia();
            m.setPlaybackCompletionDate(new Date(i + 1));
            adapter.setFeedMediaPlaybackCompletionDate(m);
            ids[ids.length - 1 - i] = m.getItem().getId();
        }
        adapter.close();

        List<FeedItem> saved = DBReader.getPlaybackHistory(context);
        assertNotNull(saved);
        assertEquals("Wrong size: ", playedItems, saved.size());
        for (int i = 0; i < playedItems; i++) {
            FeedItem item = saved.get(i);
            assertNotNull(item.getMedia().getPlaybackCompletionDate());
            assertEquals("Wrong sort order: ", item.getId(), ids[i]);
        }
    }
}
