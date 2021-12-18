package de.danoeh.antennapod.core.storage;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link FeedItemDuplicateGuesser}.
 */
public class FeedItemDuplicateGuesserTest {
    private static final long MINUTES = 1000 * 60;
    private static final long DAYS = 24 * 60 * MINUTES;

    @Test
    public void testSameId() {
        assertTrue(FeedItemDuplicateGuesser.seemDuplicates(
                item("id", "Title1", "example.com/episode1", 0, 5 * MINUTES, "audio/*"),
                item("id", "Title2", "example.com/episode2", 0, 20 * MINUTES, "video/*")));
    }

    @Test
    public void testDuplicateDownloadUrl() {
        assertTrue(FeedItemDuplicateGuesser.seemDuplicates(
                item("id1", "Title1", "example.com/episode", 0, 5 * MINUTES, "audio/*"),
                item("id2", "Title2", "example.com/episode", 0, 5 * MINUTES, "audio/*")));
        assertFalse(FeedItemDuplicateGuesser.seemDuplicates(
                item("id1", "Title1", "example.com/episode1", 0, 5 * MINUTES, "audio/*"),
                item("id2", "Title2", "example.com/episode2", 0, 5 * MINUTES, "audio/*")));
    }

    @Test
    public void testOtherAttributes() {
        assertTrue(FeedItemDuplicateGuesser.seemDuplicates(
                item("id1", "Title", "example.com/episode1", 10, 5 * MINUTES, "audio/*"),
                item("id2", "Title", "example.com/episode2", 10, 5 * MINUTES, "audio/*")));
        assertTrue(FeedItemDuplicateGuesser.seemDuplicates(
                item("id1", "Title", "example.com/episode1", 10, 5 * MINUTES, "audio/*"),
                item("id2", "Title", "example.com/episode2", 20, 6 * MINUTES, "audio/*")));
        assertFalse(FeedItemDuplicateGuesser.seemDuplicates(
                item("id1", "Title", "example.com/episode1", 10, 5 * MINUTES, "audio/*"),
                item("id2", "Title", "example.com/episode2", 10, 5 * MINUTES, "video/*")));
        assertFalse(FeedItemDuplicateGuesser.seemDuplicates(
                item("id1", "Title", "example.com/episode1", 5 * DAYS, 5 * MINUTES, "audio/*"),
                item("id2", "Title", "example.com/episode2", 2 * DAYS, 5 * MINUTES, "audio/*")));
    }

    @Test
    public void testNoMediaType() {
        assertTrue(FeedItemDuplicateGuesser.seemDuplicates(
                item("id1", "Title", "example.com/episode1", 2 * DAYS, 5 * MINUTES, ""),
                item("id2", "Title", "example.com/episode2", 2 * DAYS, 5 * MINUTES, "")));
    }

    private FeedItem item(String guid, String title, String downloadUrl,
                                  long date, long duration, String mime) {
        FeedItem item = new FeedItem(0, title, guid, "link", new Date(date), FeedItem.PLAYED, null);
        FeedMedia media = new FeedMedia(item, downloadUrl, duration, mime);
        item.setMedia(media);
        return item;
    }
}