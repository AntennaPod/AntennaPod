package de.danoeh.antennapod.core.feed;

import android.test.AndroidTestCase;

import static de.danoeh.antennapod.core.feed.FeedImageMother.anyFeedImage;
import static de.danoeh.antennapod.core.feed.FeedMother.anyFeed;

public class FeedTest extends AndroidTestCase {

    private Feed original;
    private FeedImage originalImage;
    private Feed changedFeed;

    @Override
    protected void setUp() {
        original = anyFeed();
        originalImage = original.getImage();
        changedFeed = anyFeed();
    }

    public void testCompareWithOther_feedImageDownloadUrlChanged() throws Exception {
        setNewFeedImageDownloadUrl();

        feedHasChanged();
    }

    public void testCompareWithOther_sameFeedImage() throws Exception {
        changedFeed.setImage(anyFeedImage());

        feedHasNotChanged();
    }

    public void testCompareWithOther_feedImageRemoved() throws Exception {
        feedImageRemoved();

        feedHasNotChanged();
    }

    public void testUpdateFromOther_feedImageDownloadUrlChanged() throws Exception {
        setNewFeedImageDownloadUrl();

        original.updateFromOther(changedFeed);

        feedImageWasUpdated();
    }

    public void testUpdateFromOther_feedImageRemoved() throws Exception {
        feedImageRemoved();

        original.updateFromOther(changedFeed);

        feedImageWasNotUpdated();
    }

    public void testUpdateFromOther_feedImageAdded() throws Exception {
        feedHadNoImage();
        setNewFeedImageDownloadUrl();

        original.updateFromOther(changedFeed);

        feedImageWasUpdated();
    }

    private void feedHasNotChanged() {
        assertFalse(original.compareWithOther(changedFeed));
    }

    private void feedHadNoImage() {
        original.setImage(null);
    }

    private void setNewFeedImageDownloadUrl() {
        changedFeed.getImage().setDownload_url("http://example.com/new_picture");
    }

    private void feedHasChanged() {
        assertTrue(original.compareWithOther(changedFeed));
    }

    private void feedImageRemoved() {
        changedFeed.setImage(null);
    }

    private void feedImageWasUpdated() {
        assertEquals(original.getImage().getDownload_url(), changedFeed.getImage().getDownload_url());
    }

    private void feedImageWasNotUpdated() {
        assertTrue(originalImage == original.getImage());
    }

}