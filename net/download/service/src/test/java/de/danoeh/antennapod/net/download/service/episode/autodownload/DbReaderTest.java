package de.danoeh.antennapod.net.download.service.episode.autodownload;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedCounter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedOrder;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.NavDrawerData;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.storage.database.LongList;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RobolectricTestRunner;

import static de.danoeh.antennapod.net.download.service.episode.autodownload.DbTestUtils.saveFeedlist;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for DBReader.
 */
@SuppressWarnings("ConstantConditions")
@RunWith(Enclosed.class)
public class DbReaderTest {
    @Ignore("Not a test")
    public static class TestBase {
        @Before
        public void setUp() {
            Context context = InstrumentationRegistry.getInstrumentation().getContext();
            UserPreferences.init(context);

            PodDBAdapter.init(context);
            PodDBAdapter.deleteDatabase();
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.close();
        }

        @After
        public void tearDown() {
            PodDBAdapter.tearDownTests();
            DBWriter.tearDownTests();
        }
    }

    @RunWith(RobolectricTestRunner.class)
    public static class SingleTests extends TestBase {
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

            final long lastRefreshed = System.currentTimeMillis();
            Feed feed1 = new Feed(0, null, "A", "link", "d", null, null, null, "rss", "A", null, "", "", lastRefreshed);
            Feed feed2 = new Feed(0, null, "b", "link", "d", null, null, null, "rss", "b", null, "", "", lastRefreshed);
            Feed feed3 = new Feed(0, null, "C", "link", "d", null, null, null, "rss", "C", null, "", "", lastRefreshed);
            Feed feed4 = new Feed(0, null, "d", "link", "d", null, null, null, "rss", "d", null, "", "", lastRefreshed);
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
                assertEquals(urls.get(i), feeds.get(i).getDownloadUrl());
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
            List<FeedItem> savedItems = DBReader.getFeedItemList(feed,
                    FeedItemFilter.unfiltered(), SortOrder.DATE_NEW_OLD, 0, Integer.MAX_VALUE);
            assertNotNull(savedItems);
            assertEquals(items.size(), savedItems.size());
            for (int i = 0; i < savedItems.size(); i++) {
                assertEquals(savedItems.get(i).getId(), items.get(i).getId());
            }
        }

        @SuppressWarnings("SameParameterValue")
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
        public void testGetQueueIdList() {
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

        @SuppressWarnings("SameParameterValue")
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
                    item.getMedia().setDownloaded(true, System.currentTimeMillis());
                    item.getMedia().setLocalFileUrl("file" + i);
                    downloaded.add(item);
                }
            }
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            adapter.storeFeedItemlist(downloaded);
            adapter.close();
            return downloaded;
        }

        @Test
        public void testGetDownloadedItems() {
            final int numItems = 10;
            List<FeedItem> downloaded = saveDownloadedItems(numItems);
            List<FeedItem> downloadedSaved = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                    new FeedItemFilter(FeedItemFilter.DOWNLOADED), SortOrder.DATE_NEW_OLD);
            assertNotNull(downloadedSaved);
            assertEquals(downloaded.size(), downloadedSaved.size());
            for (FeedItem item : downloadedSaved) {
                assertNotNull(item.getMedia());
                assertTrue(item.getMedia().isDownloaded());
                assertNotNull(item.getMedia().getDownloadUrl());
            }
        }

        @SuppressWarnings("SameParameterValue")
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
            adapter.storeFeedItemlist(newItems);
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
            List<FeedItem> newItemsSaved = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                    new FeedItemFilter(FeedItemFilter.NEW), SortOrder.DATE_NEW_OLD);
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
        public void testGetPlaybackHistoryLength() {
            final int totalItems = 100;

            Feed feed = DbTestUtils.saveFeedlist(1, totalItems, true).get(0);

            PodDBAdapter adapter = PodDBAdapter.getInstance();
            for (int playedItems : Arrays.asList(0, 1, 20, 100)) {
                adapter.open();
                for (int i = 0; i < playedItems; ++i) {
                    FeedMedia m = feed.getItems().get(i).getMedia();
                    m.setPlaybackCompletionDate(new Date(i + 1));

                    adapter.setFeedMediaPlaybackCompletionDate(m);
                }
                adapter.close();

                long len = DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.IS_IN_HISTORY));
                assertEquals("Wrong size: ", (int) len, playedItems);
            }

        }

        @Test
        public void testGetNavDrawerDataQueueEmptyNoUnreadItems() {
            final int numFeeds = 10;
            final int numItems = 10;
            DbTestUtils.saveFeedlist(numFeeds, numItems, true);
            NavDrawerData navDrawerData = DBReader.getNavDrawerData(
                    UserPreferences.getSubscriptionsFilter(), FeedOrder.COUNTER, FeedCounter.SHOW_NEW);
            assertEquals(numFeeds, navDrawerData.items.size());
            assertEquals(0, navDrawerData.numNewItems);
            assertEquals(0, navDrawerData.queueSize);
        }

        @Test
        public void testGetNavDrawerDataQueueNotEmptyWithUnreadItems() {
            final int numFeeds = 10;
            final int numItems = 10;
            final int numQueue = 1;
            final int numNew = 2;
            List<Feed> feeds = DbTestUtils.saveFeedlist(numFeeds, numItems, true);
            PodDBAdapter adapter = PodDBAdapter.getInstance();
            adapter.open();
            for (int i = 0; i < numNew; i++) {
                FeedItem item = feeds.get(0).getItems().get(i);
                item.setNew();
                adapter.setSingleFeedItem(item);
            }
            List<FeedItem> queue = new ArrayList<>();
            for (int i = 0; i < numQueue; i++) {
                FeedItem item = feeds.get(1).getItems().get(i);
                queue.add(item);
            }
            adapter.setQueue(queue);

            adapter.close();

            NavDrawerData navDrawerData = DBReader.getNavDrawerData(
                    UserPreferences.getSubscriptionsFilter(), FeedOrder.COUNTER, FeedCounter.SHOW_NEW);
            assertEquals(numFeeds, navDrawerData.items.size());
            assertEquals(numNew, navDrawerData.numNewItems);
            assertEquals(numQueue, navDrawerData.queueSize);
        }

        @Test
        public void testGetFeedItemlistCheckChaptersFalse() {
            List<Feed> feeds = DbTestUtils.saveFeedlist(10, 10, false, false, 0);
            for (Feed feed : feeds) {
                for (FeedItem item : feed.getItems()) {
                    assertFalse(item.hasChapters());
                }
            }
        }

        @Test
        public void testGetFeedItemlistCheckChaptersTrue() {
            List<Feed> feeds = saveFeedlist(10, 10, false, true, 10);
            for (Feed feed : feeds) {
                for (FeedItem item : feed.getItems()) {
                    assertTrue(item.hasChapters());
                }
            }
        }

        @Test
        public void testLoadChaptersOfFeedItemNoChapters() {
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
        public void testLoadChaptersOfFeedItemWithChapters() {
            final int numChapters = 3;
            DbTestUtils.saveFeedlist(1, 3, false, false, 0);
            List<Feed> feeds = saveFeedlist(1, 3, false, true, numChapters);
            for (Feed feed : feeds) {
                for (FeedItem item : feed.getItems()) {
                    assertTrue(item.hasChapters());
                    item.setChapters(DBReader.loadChaptersOfFeedItem(item));
                    assertTrue(item.hasChapters());
                    assertNotNull(item.getChapters());
                    assertEquals(numChapters, item.getChapters().size());
                }
            }
        }

        @Test
        public void testGetItemWithChapters() {
            final int numChapters = 3;
            List<Feed> feeds = saveFeedlist(1, 1, false, true, numChapters);
            FeedItem item1 = feeds.get(0).getItems().get(0);
            FeedItem item2 = DBReader.getFeedItem(item1.getId());
            item2.setChapters(DBReader.loadChaptersOfFeedItem(item2));
            assertTrue(item2.hasChapters());
            assertEquals(item1.getChapters().size(), item2.getChapters().size());
            for (int i = 0; i < item1.getChapters().size(); i++) {
                assertEquals(item1.getChapters().get(i).getId(), item2.getChapters().get(i).getId());
            }
        }

        @Test
        public void testGetItemByEpisodeUrl() {
            List<Feed> feeds = saveFeedlist(1, 1, true);
            FeedItem item1 = feeds.get(0).getItems().get(0);
            FeedItem feedItemByEpisodeUrl = DBReader.getFeedItemByGuidOrEpisodeUrl(null,
                    item1.getMedia().getDownloadUrl());
            assertEquals(item1.getItemIdentifier(), feedItemByEpisodeUrl.getItemIdentifier());
        }

        @Test
        public void testGetItemByGuid() {
            List<Feed> feeds = saveFeedlist(1, 1, true);
            FeedItem item1 = feeds.get(0).getItems().get(0);

            FeedItem feedItemByGuid = DBReader.getFeedItemByGuidOrEpisodeUrl(item1.getItemIdentifier(),
                    item1.getMedia().getDownloadUrl());
            assertEquals(item1.getItemIdentifier(), feedItemByGuid.getItemIdentifier());
        }

    }

    @RunWith(ParameterizedRobolectricTestRunner.class)
    public static class PlaybackHistoryTest extends TestBase {

        private int paramOffset;
        private int paramLimit;

        @ParameterizedRobolectricTestRunner.Parameters
        public static Collection<Object[]> data() {
            List<Integer> limits = Arrays.asList(1, 20, 100);
            List<Integer> offsets = Arrays.asList(0, 10, 20);
            Object[][] rv = new Object[limits.size() * offsets.size()][2];
            int i = 0;
            for (int offset : offsets) {
                for (int limit : limits) {
                    rv[i][0] = offset;
                    rv[i][1] = limit;
                    i++;
                }
            }

            return Arrays.asList(rv);
        }

        public PlaybackHistoryTest(int offset, int limit) {
            this.paramOffset = offset;
            this.paramLimit = limit;

        }

        @Test
        public void testGetPlaybackHistory() {
            final int numItems = (paramLimit + 1) * 2;
            final int playedItems = paramLimit + 1;
            final int numReturnedItems = Math.min(Math.max(playedItems - paramOffset, 0), paramLimit);
            final int numFeeds = 1;

            Feed feed = DbTestUtils.saveFeedlist(numFeeds, numItems, true).get(0);
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

            List<FeedItem> saved = DBReader.getEpisodes(paramOffset, paramLimit,
                    new FeedItemFilter(FeedItemFilter.IS_IN_HISTORY), SortOrder.COMPLETION_DATE_NEW_OLD);
            assertNotNull(saved);
            assertEquals(String.format("Wrong size with offset %d and limit %d: ",
                            paramOffset, paramLimit),
                    numReturnedItems, saved.size());
            for (int i = 0; i < numReturnedItems; i++) {
                FeedItem item = saved.get(i);
                assertNotNull(item.getMedia().getPlaybackCompletionDate());
                assertEquals(String.format("Wrong sort order with offset %d and limit %d: ",
                                paramOffset, paramLimit),
                        item.getId(), ids[paramOffset + i]);
            }
        }
    }
}
