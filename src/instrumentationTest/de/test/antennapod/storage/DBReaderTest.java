package instrumentationTest.de.test.antennapod.storage;

import android.content.Context;
import android.test.InstrumentationTestCase;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.PodDBAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
}
