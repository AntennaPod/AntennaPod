package instrumentationTest.de.test.antennapod.storage;

import android.content.Context;
import android.test.InstrumentationTestCase;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.PodDBAdapter;
import de.danoeh.antennapod.util.comparator.FeedItemPubdateComparator;

import java.util.*;

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
                null, null, null, "feed", null, null, "url", false);
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

    private List<Feed> saveFeedlist(int numFeeds, int numItems, boolean withMedia) {
        if (numFeeds <= 0) {
            throw new IllegalArgumentException("numFeeds<=0");
        }
        if (numItems < 0) {
            throw new IllegalArgumentException("numItems<0");
        }

        final Context context = getInstrumentation().getTargetContext();
        List<Feed> feeds = new ArrayList<Feed>();
        PodDBAdapter adapter = new PodDBAdapter(context);
        adapter.open();
        for (int i = 0; i < numFeeds; i++) {
            Feed f = new Feed(0, new Date(), "feed " + i, "link" + i, "descr", null, null,
                    null, null, "id" + i, null, null, "url" + i, false);
            f.setItems(new ArrayList<FeedItem>());
            for (int j = 0; j < numItems; j++) {
                FeedItem item = new FeedItem(0, "item " + j, "id" + j, "link" + j, new Date(),
                        true, f);
                if (withMedia) {
                    FeedMedia media = new FeedMedia(item, "url" + j, 1, "audio/mp3");
                    item.setMedia(media);
                }
                f.getItems().add(item);
            }
            Collections.sort(f.getItems(), new FeedItemPubdateComparator());
            adapter.setCompleteFeed(f);
            assertTrue(f.getId() != 0);
            for (FeedItem item : f.getItems()) {
                assertTrue(item.getId() != 0);
            }
            feeds.add(f);
        }
        adapter.close();
        return feeds;
    }

    public void testGetFeedList() {
        final Context context = getInstrumentation().getTargetContext();
        List<Feed> feeds = saveFeedlist(10, 0, false);
        List<Feed> savedFeeds = DBReader.getFeedList(context);
        assertNotNull(savedFeeds);
        assertTrue(savedFeeds.size() == feeds.size());
        for (int i = 0; i < feeds.size(); i++) {
            assertTrue(savedFeeds.get(i).getId() == feeds.get(i).getId());
        }
    }

    public void testFeedListDownloadUrls() {
        final Context context = getInstrumentation().getTargetContext();
        List<Feed> feeds = saveFeedlist(10, 0, false);
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
        List<Feed> feeds = saveFeedlist(numFeeds, numItems, false);
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
        Feed feed = saveFeedlist(numFeeds, numItems, false).get(0);
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
        List<Feed> feeds = saveFeedlist(numItems, numItems, false);
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
        List<Feed> feeds = saveFeedlist(numItems, numItems, true);
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
        List<Feed> feeds = saveFeedlist(numItems, numItems, true);
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
}
