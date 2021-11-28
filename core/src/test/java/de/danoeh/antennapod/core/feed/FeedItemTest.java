package de.danoeh.antennapod.core.feed;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static de.danoeh.antennapod.core.feed.FeedItemMother.anyFeedItemWithImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FeedItemTest {

    private static final String TEXT_LONG = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
    private static final String TEXT_SHORT = "Lorem ipsum";
    private static final long ONE_HOUR = 1000L * 3600L;

    private FeedItem original;
    private FeedItem changedFeedItem;

    @Before
    public void setUp() {
        original = anyFeedItemWithImage();
        changedFeedItem = anyFeedItemWithImage();
    }

    @Test
    public void testUpdateFromOther_feedItemImageDownloadUrlChanged() {
        setNewFeedItemImageDownloadUrl();
        original.updateFromOther(changedFeedItem);
        assertFeedItemImageWasUpdated();
    }

    @Test
    public void testUpdateFromOther_feedItemImageRemoved() {
        feedItemImageRemoved();
        original.updateFromOther(changedFeedItem);
        assertFeedItemImageWasNotUpdated();
    }

    @Test
    public void testUpdateFromOther_feedItemImageAdded() {
        original.setImageUrl(null);
        setNewFeedItemImageDownloadUrl();
        original.updateFromOther(changedFeedItem);
        assertFeedItemImageWasUpdated();
    }

    @Test
    public void testUpdateFromOther_dateChanged() throws Exception {
        Date originalDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("1952-03-11 00:00:00");
        Date changedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("1952-03-11 00:42:42");
        original.setPubDate(originalDate);
        changedFeedItem.setPubDate(changedDate);
        original.updateFromOther(changedFeedItem);
        assertEquals(changedDate.getTime(), original.getPubDate().getTime());
    }

    /**
     * Test that a played item loses that state after being marked as new.
     */
    @Test
    public void testMarkPlayedItemAsNew_itemNotPlayed() {
        original.setPlayed(true);
        original.setNew();

        assertFalse(original.isPlayed());
    }

    /**
     * Test that a new item loses that state after being marked as played.
     */
    @Test
    public void testMarkNewItemAsPlayed_itemNotNew() {
        original.setNew();
        original.setPlayed(true);

        assertFalse(original.isNew());
    }

    /**
     * Test that a new item loses that state after being marked as not played.
     */
    @Test
    public void testMarkNewItemAsNotPlayed_itemNotNew() {
        original.setNew();
        original.setPlayed(false);

        assertFalse(original.isNew());
    }

    private void setNewFeedItemImageDownloadUrl() {
        changedFeedItem.setImageUrl("http://example.com/new_picture");
    }

    private void feedItemImageRemoved() {
        changedFeedItem.setImageUrl(null);
    }

    private void assertFeedItemImageWasUpdated() {
        assertEquals(original.getImageUrl(), changedFeedItem.getImageUrl());
    }

    private void assertFeedItemImageWasNotUpdated() {
        assertEquals(anyFeedItemWithImage().getImageUrl(), original.getImageUrl());
    }

    /**
     * If one of `description` or `content:encoded` is null, use the other one.
     */
    @Test
    public void testShownotesNullValues() {
        testShownotes(null, TEXT_LONG);
        testShownotes(TEXT_LONG, null);
    }

    /**
     * If `description` is reasonably longer than `content:encoded`, use `description`.
     */
    @Test
    public void testShownotesLength() {
        testShownotes(TEXT_SHORT, TEXT_LONG);
        testShownotes(TEXT_LONG, TEXT_SHORT);
    }

    /**
     * Checks if the shownotes equal TEXT_LONG, using the given `description` and `content:encoded`.
     *
     * @param description Description of the feed item
     * @param contentEncoded `content:encoded` of the feed item
     */
    private void testShownotes(String description, String contentEncoded) {
        FeedItem item = new FeedItem();
        item.setDescriptionIfLonger(description);
        item.setDescriptionIfLonger(contentEncoded);
        assertEquals(TEXT_LONG, item.getDescription());
    }

    @Test
    public void testAutoDownloadBackoff() {
        FeedItem item = new FeedItem();
        item.setMedia(new FeedMedia(item, "https://example.com/file.mp3", 0, "audio/mpeg"));

        long now = ONE_HOUR; // In reality, this is System.currentTimeMillis()
        assertTrue(item.isAutoDownloadable(now));
        item.increaseFailedAutoDownloadAttempts(now);
        assertFalse(item.isAutoDownloadable(now));

        now += ONE_HOUR;
        assertTrue(item.isAutoDownloadable(now));
        item.increaseFailedAutoDownloadAttempts(now);
        assertFalse(item.isAutoDownloadable(now));

        now += ONE_HOUR;
        assertFalse(item.isAutoDownloadable(now)); // Should backoff, so more than 1 hour needed

        now += ONE_HOUR;
        assertTrue(item.isAutoDownloadable(now)); // Now it's enough
        item.increaseFailedAutoDownloadAttempts(now);
        item.increaseFailedAutoDownloadAttempts(now);
        item.increaseFailedAutoDownloadAttempts(now);

        now += 1000L * ONE_HOUR;
        assertFalse(item.isAutoDownloadable(now)); // Should have given up
        item.increaseFailedAutoDownloadAttempts(now);

        now += 1000L * ONE_HOUR;
        assertFalse(item.isAutoDownloadable(now)); // Still given up
    }
}
