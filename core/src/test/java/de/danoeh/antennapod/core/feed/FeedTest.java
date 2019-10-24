package de.danoeh.antennapod.core.feed;

import org.junit.Before;
import org.junit.Test;

import de.danoeh.antennapod.core.util.SortOrder;

import static de.danoeh.antennapod.core.feed.FeedMother.anyFeed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FeedTest {

    private Feed original;
    private Feed changedFeed;

    @Before
    public void setUp() {
        original = anyFeed();
        changedFeed = anyFeed();
    }

    @Test
    public void testCompareWithOther_feedImageDownloadUrlChanged() throws Exception {
        setNewFeedImageDownloadUrl();
        feedHasChanged();
    }

    @Test
    public void testCompareWithOther_sameFeedImage() throws Exception {
        changedFeed.setImageUrl(FeedMother.IMAGE_URL);
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

    @Test
    public void testSetSortOrder_OnlyIntraFeedSortAllowed() throws Exception {
        for (SortOrder sortOrder : SortOrder.values()) {
            if (sortOrder.scope == SortOrder.Scope.INTRA_FEED) {
                original.setSortOrder(sortOrder); // should be okay
            } else {
                try {
                    original.setSortOrder(sortOrder);
                    fail("SortOrder " + sortOrder + " should not be allowed on a feed");
                } catch (IllegalArgumentException iae) {
                    // expected exception
                }
            }
        }
    }

    @Test
    public void testSetSortOrder_NullAllowed() throws Exception {
        original.setSortOrder(null); // should be okay
    }

    private void feedHasNotChanged() {
        assertFalse(original.compareWithOther(changedFeed));
    }

    private void feedHadNoImage() {
        original.setImageUrl(null);
    }

    private void setNewFeedImageDownloadUrl() {
        changedFeed.setImageUrl("http://example.com/new_picture");
    }

    private void feedHasChanged() {
        assertTrue(original.compareWithOther(changedFeed));
    }

    private void feedImageRemoved() {
        changedFeed.setImageUrl(null);
    }

    private void feedImageWasUpdated() {
        assertEquals(original.getImageUrl(), changedFeed.getImageUrl());
    }

    private void feedImageWasNotUpdated() {
        assertEquals(anyFeed().getImageUrl(), original.getImageUrl());
    }

}