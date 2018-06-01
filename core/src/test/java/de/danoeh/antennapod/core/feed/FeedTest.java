package de.danoeh.antennapod.core.feed;

import org.junit.Before;
import org.junit.Test;

import static de.danoeh.antennapod.core.feed.FeedImageMother.anyFeedImage;
import static de.danoeh.antennapod.core.feed.FeedMother.anyFeed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FeedTest {

    private Feed original;
    private FeedImage originalImage;
    private Feed changedFeed;

    @Before
    public void setUp() {
        original = anyFeed();
        originalImage = original.getImage();
        changedFeed = anyFeed();
    }

    @Test
    public void testCompareWithOther_feedImageDownloadUrlChanged() throws Exception {
        setNewFeedImageDownloadUrl();

        feedHasChanged();
    }

    @Test
    public void testCompareWithOther_sameFeedImage() throws Exception {
        changedFeed.setImage(anyFeedImage());

        feedHasNotChanged();
    }

    @Test
    public void testCompareWithOther_feedImageRemoved() throws Exception {
        feedImageRemoved();

        feedHasNotChanged();
    }

    @Test
    public void testUpdateFromOther_feedImageDownloadUrlChanged() throws Exception {
        setNewFeedImageDownloadUrl();

        original.updateFromOther(changedFeed);

        feedImageWasUpdated();
    }

    @Test
    public void testUpdateFromOther_feedImageRemoved() throws Exception {
        feedImageRemoved();

        original.updateFromOther(changedFeed);

        feedImageWasNotUpdated();
    }

    @Test
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