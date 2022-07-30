package de.danoeh.antennapod.core.storage.mapper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.database.mapper.FeedCursorMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import de.danoeh.antennapod.model.feed.Feed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class FeedCursorMapperTest {
    private PodDBAdapter adapter;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();

        PodDBAdapter.init(context);
        adapter = PodDBAdapter.getInstance();

        writeFeedToDatabase();
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testFromCursor() {
        try (Cursor cursor = adapter.getAllFeedsCursor()) {
            cursor.moveToNext();
            Feed feed = FeedCursorMapper.convert(cursor);
            assertTrue(feed.getId() >= 0);
            assertEquals("feed custom title", feed.getTitle());
            assertEquals("feed custom title", feed.getCustomTitle());
            assertEquals("feed link", feed.getLink());
            assertEquals("feed description", feed.getDescription());
            assertEquals("feed payment link", feed.getPaymentLinks().get(0).url);
            assertEquals("feed author", feed.getAuthor());
            assertEquals("feed language", feed.getLanguage());
            assertEquals("feed image url", feed.getImageUrl());
            assertEquals("feed file url", feed.getFile_url());
            assertEquals("feed download url", feed.getDownload_url());
            assertTrue(feed.isDownloaded());
            assertEquals("feed last update", feed.getLastUpdate());
            assertEquals("feed type", feed.getType());
            assertEquals("feed identifier", feed.getFeedIdentifier());
            assertTrue(feed.isPaged());
            assertEquals("feed next page link", feed.getNextPageLink());
            assertTrue(feed.getItemFilter().showUnplayed);
            assertEquals(1, feed.getSortOrder().code);
            assertTrue(feed.hasLastUpdateFailed());
        }
    }

    /**
     * Insert test data to the database.
     * Uses raw database insert instead of adapter.setCompleteFeed() to avoid testing the Feed class
     * against itself.
     */
    private void writeFeedToDatabase() {
        ContentValues values = new ContentValues();
        values.put(PodDBAdapter.KEY_TITLE, "feed title");
        values.put(PodDBAdapter.KEY_CUSTOM_TITLE, "feed custom title");
        values.put(PodDBAdapter.KEY_LINK, "feed link");
        values.put(PodDBAdapter.KEY_DESCRIPTION, "feed description");
        values.put(PodDBAdapter.KEY_PAYMENT_LINK, "feed payment link");
        values.put(PodDBAdapter.KEY_AUTHOR, "feed author");
        values.put(PodDBAdapter.KEY_LANGUAGE, "feed language");
        values.put(PodDBAdapter.KEY_IMAGE_URL, "feed image url");

        values.put(PodDBAdapter.KEY_FILE_URL, "feed file url");
        values.put(PodDBAdapter.KEY_DOWNLOAD_URL, "feed download url");
        values.put(PodDBAdapter.KEY_DOWNLOADED, true);
        values.put(PodDBAdapter.KEY_LASTUPDATE, "feed last update");
        values.put(PodDBAdapter.KEY_TYPE, "feed type");
        values.put(PodDBAdapter.KEY_FEED_IDENTIFIER, "feed identifier");

        values.put(PodDBAdapter.KEY_IS_PAGED, true);
        values.put(PodDBAdapter.KEY_NEXT_PAGE_LINK, "feed next page link");
        values.put(PodDBAdapter.KEY_HIDE, "unplayed");
        values.put(PodDBAdapter.KEY_SORT_ORDER, "1");
        values.put(PodDBAdapter.KEY_LAST_UPDATE_FAILED, true);

        adapter.insertTestData(PodDBAdapter.TABLE_NAME_FEEDS, values);
    }
}
