package de.danoeh.antennapod.core.feed;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.danoeh.antennapod.core.storage.DBReader;

import static de.danoeh.antennapod.core.feed.FeedItemMother.anyFeedItemWithImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FeedItemTest {

    private static final String TEXT_LONG = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
    private static final String TEXT_SHORT = "Lorem ipsum";

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
    public void testShownotesNullValues() throws Exception {
        testShownotes(null, TEXT_LONG);
        testShownotes(TEXT_LONG, null);
    }

    /**
     * If `description` is reasonably longer than `content:encoded`, use `description`.
     */
    @Test
    public void testShownotesLength() throws Exception {
        testShownotes(TEXT_SHORT, TEXT_LONG);
        testShownotes(TEXT_LONG, TEXT_SHORT);
    }

    /**
     * Checks if the shownotes equal TEXT_LONG, using the given `description` and `content:encoded`.
     *
     * @param description Description of the feed item
     * @param contentEncoded `content:encoded` of the feed item
     */
    private void testShownotes(String description, String contentEncoded) throws Exception {
        try (MockedStatic<DBReader> ignore = Mockito.mockStatic(DBReader.class)) {
            FeedItem item = new FeedItem();
            item.setDescription(description);
            item.setContentEncoded(contentEncoded);
            assertEquals(TEXT_LONG, item.loadShownotes().call());
        }
    }
}