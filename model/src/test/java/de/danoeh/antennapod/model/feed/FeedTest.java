package de.danoeh.antennapod.model.feed;

import org.junit.Before;
import org.junit.Test;

import static de.danoeh.antennapod.model.feed.FeedMother.anyFeed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class FeedTest {

    private Feed original;
    private Feed changedFeed;

    @Before
    public void setUp() {
        original = anyFeed();
        changedFeed = anyFeed();
    }

    @Test
    public void testUpdateFromOther_feedImageDownloadUrlChanged() {
        changedFeed.setImageUrl("http://example.com/new_picture");
        original.updateFromOther(changedFeed);
        assertEquals(original.getImageUrl(), changedFeed.getImageUrl());
    }

    @Test
    public void testUpdateFromOther_feedImageRemoved() {
        changedFeed.setImageUrl(null);
        original.updateFromOther(changedFeed);
        assertEquals(anyFeed().getImageUrl(), original.getImageUrl());
    }

    @Test
    public void testUpdateFromOther_feedImageAdded() {
        original.setImageUrl(null);
        changedFeed.setImageUrl("http://example.com/new_picture");
        original.updateFromOther(changedFeed);
        assertEquals(original.getImageUrl(), changedFeed.getImageUrl());
    }

    @Test
    public void testSetSortOrder_OnlyIntraFeedSortAllowed() {
        for (SortOrder sortOrder : SortOrder.values()) {
            if (sortOrder.scope == SortOrder.Scope.INTRA_FEED) {
                original.setSortOrder(sortOrder); // should be okay
            } else {
                assertThrows(IllegalArgumentException.class, () -> original.setSortOrder(sortOrder));
            }
        }
    }

    @Test
    public void testSetSortOrder_NullAllowed() {
        original.setSortOrder(null); // should be okay
    }
}