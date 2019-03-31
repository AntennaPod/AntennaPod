package de.danoeh.antennapod.core.feed;

import org.junit.Before;
import org.junit.Test;

import static de.danoeh.antennapod.core.feed.FeedItemMother.anyFeedItemWithImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FeedItemTest {

    private FeedItem original;
    private FeedItem changedFeedItem;

    @Before
    public void setUp() {
        original = anyFeedItemWithImage();
        changedFeedItem = anyFeedItemWithImage();
    }

    @Test
    public void testUpdateFromOther_feedItemImageDownloadUrlChanged() throws Exception {
        setNewFeedItemImageDownloadUrl();
        original.updateFromOther(changedFeedItem);
        assertFeedItemImageWasUpdated();
    }

    @Test
    public void testUpdateFromOther_feedItemImageRemoved() throws Exception {
        feedItemImageRemoved();
        original.updateFromOther(changedFeedItem);
        assertFeedItemImageWasNotUpdated();
    }

    @Test
    public void testUpdateFromOther_feedItemImageAdded() throws Exception {
        original.setImageUrl(null);
        setNewFeedItemImageDownloadUrl();
        original.updateFromOther(changedFeedItem);
        assertFeedItemImageWasUpdated();
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

}