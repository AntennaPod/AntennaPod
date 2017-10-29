package de.danoeh.antennapod.core.feed;

import android.test.AndroidTestCase;

import static de.danoeh.antennapod.core.feed.FeedItemMother.anyFeedItemWithImage;

public class FeedItemTest extends AndroidTestCase {

    private FeedItem original;
    private FeedImage originalImage;
    private FeedItem changedFeedItem;

    @Override
    protected void setUp() {
        original = anyFeedItemWithImage();
        originalImage = original.getImage();
        changedFeedItem = anyFeedItemWithImage();
    }

    public void testUpdateFromOther_feedItemImageDownloadUrlChanged() throws Exception {
        setNewFeedItemImageDownloadUrl();

        original.updateFromOther(changedFeedItem);

        feedItemImageWasUpdated();
    }

    public void testUpdateFromOther_feedItemImageRemoved() throws Exception {
        feedItemImageRemoved();

        original.updateFromOther(changedFeedItem);

        feedItemImageWasNotUpdated();
    }

    public void testUpdateFromOther_feedItemImageAdded() throws Exception {
        feedItemHadNoImage();
        setNewFeedItemImageDownloadUrl();

        original.updateFromOther(changedFeedItem);

        feedItemImageWasUpdated();
    }

    private void feedItemHadNoImage() {
        original.setImage(null);
    }

    private void setNewFeedItemImageDownloadUrl() {
        changedFeedItem.getImage().setDownload_url("http://example.com/new_picture");
    }

    private void feedItemImageRemoved() {
        changedFeedItem.setImage(null);
    }

    private void feedItemImageWasUpdated() {
        assertEquals(original.getImage().getDownload_url(), changedFeedItem.getImage().getDownload_url());
    }

    private void feedItemImageWasNotUpdated() {
        assertTrue(originalImage == original.getImage());
    }

}