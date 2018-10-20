package de.test.antennapod.storage;

import android.content.Context;
import android.test.FlakyTest;
import android.test.InstrumentationTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

import static java.util.Collections.singletonList;

/**
 * Test class for DBTasks
 */
public class DBTasksTest extends InstrumentationTestCase {

    private static final String TAG = "DBTasksTest";

    private Context context;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        assertTrue(PodDBAdapter.deleteDatabase());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getTargetContext();

        // create new database
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();

        UserPreferences.init(context);
    }

    @FlakyTest(tolerance = 3)
    public void testUpdateFeedNewFeed() {
        final int NUM_ITEMS = 10;

        Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < NUM_ITEMS; i++) {
            feed.getItems().add(new FeedItem(0, "item " + i, "id " + i, "link " + i, new Date(), FeedItem.UNPLAYED, feed));
        }
        Feed newFeed = DBTasks.updateFeed(context, feed)[0];

        assertTrue(newFeed == feed);
        assertTrue(feed.getId() != 0);
        for (FeedItem item : feed.getItems()) {
            assertFalse(item.isPlayed());
            assertTrue(item.getId() != 0);
        }
    }

    /** Two feeds with the same title, but different download URLs should be treated as different feeds. */
    public void testUpdateFeedSameTitle() {

        Feed feed1 = new Feed("url1", null, "title");
        Feed feed2 = new Feed("url2", null, "title");

        feed1.setItems(new ArrayList<>());
        feed2.setItems(new ArrayList<>());

        Feed savedFeed1 = DBTasks.updateFeed(context, feed1)[0];
        Feed savedFeed2 = DBTasks.updateFeed(context, feed2)[0];

        assertTrue(savedFeed1.getId() != savedFeed2.getId());
    }

    public void testUpdateFeedUpdatedFeed() {
        final int NUM_ITEMS_OLD = 10;
        final int NUM_ITEMS_NEW = 10;

        final Feed feed = new Feed("url", null, "title");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < NUM_ITEMS_OLD; i++) {
            feed.getItems().add(new FeedItem(0, "item " + i, "id " + i, "link " + i, new Date(i), FeedItem.PLAYED, feed));
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

        for (int i = NUM_ITEMS_OLD; i < NUM_ITEMS_NEW + NUM_ITEMS_OLD; i++) {
            feed.getItems().add(0, new FeedItem(0, "item " + i, "id " + i, "link " + i, new Date(i), FeedItem.UNPLAYED, feed));
        }

        final Feed newFeed = DBTasks.updateFeed(context, feed)[0];
        assertTrue(feed != newFeed);

        updatedFeedTest(newFeed, feedID, itemIDs, NUM_ITEMS_OLD, NUM_ITEMS_NEW);

        final Feed feedFromDB = DBReader.getFeed(newFeed.getId());
        assertNotNull(feedFromDB);
        assertTrue(feedFromDB.getId() == newFeed.getId());
        updatedFeedTest(feedFromDB, feedID, itemIDs, NUM_ITEMS_OLD, NUM_ITEMS_NEW);
    }

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
        feed.setItems(singletonList(item));

        final Feed newFeed = DBTasks.updateFeed(context, feed)[0];
        assertTrue(feed != newFeed);

        final Feed feedFromDB = DBReader.getFeed(newFeed.getId());
        final FeedItem feedItemFromDB = feedFromDB.getItems().get(0);
        assertTrue("state: " + feedItemFromDB.getState(), feedItemFromDB.isNew());
    }

    private void updatedFeedTest(final Feed newFeed, long feedID, List<Long> itemIDs, final int NUM_ITEMS_OLD, final int NUM_ITEMS_NEW) {
        assertTrue(newFeed.getId() == feedID);
        assertTrue(newFeed.getItems().size() == NUM_ITEMS_NEW + NUM_ITEMS_OLD);
        Collections.reverse(newFeed.getItems());
        Date lastDate = new Date(0);
        for (int i = 0; i < NUM_ITEMS_OLD; i++) {
            FeedItem item = newFeed.getItems().get(i);
            assertTrue(item.getFeed() == newFeed);
            assertTrue(item.getId() == itemIDs.get(i));
            assertTrue(item.isPlayed());
            assertTrue(item.getPubDate().getTime() >= lastDate.getTime());
            lastDate = item.getPubDate();
        }
        for (int i = NUM_ITEMS_OLD; i < NUM_ITEMS_NEW + NUM_ITEMS_OLD; i++) {
            FeedItem item = newFeed.getItems().get(i);
            assertTrue(item.getFeed() == newFeed);
            assertTrue(item.getId() != 0);
            assertFalse(item.isPlayed());
            assertTrue(item.getPubDate().getTime() >= lastDate.getTime());
            lastDate = item.getPubDate();
        }
    }
}
