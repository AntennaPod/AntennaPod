package de.danoeh.antennapod.core.feed;

import org.junit.Before;
import org.junit.Test;

import static de.danoeh.antennapod.core.feed.FeedMediaMother.anyFeedMedia;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FeedMediaTest {

    private FeedMedia media;

    @Before
    public void setUp() {
        media = anyFeedMedia();
    }

    /**
     * Downloading a media from a not new and not played item should not change the item state.
     */
    @Test
    public void testDownloadMediaOfNotNewAndNotPlayedItem_unchangedItemState() {
        FeedItem item = mock(FeedItem.class);
        when(item.isNew()).thenReturn(false);
        when(item.isPlayed()).thenReturn(false);

        media.setItem(item);
        media.setDownloaded(true);

        verify(item, never()).setNew();
        verify(item, never()).setPlayed(true);
        verify(item, never()).setPlayed(false);
    }

    /**
     * Downloading a media from a played item (thus not new) should not change the item state.
     */
    @Test
    public void testDownloadMediaOfPlayedItem_unchangedItemState() {
        FeedItem item = mock(FeedItem.class);
        when(item.isNew()).thenReturn(false);
        when(item.isPlayed()).thenReturn(true);

        media.setItem(item);
        media.setDownloaded(true);

        verify(item, never()).setNew();
        verify(item, never()).setPlayed(true);
        verify(item, never()).setPlayed(false);
    }

    /**
     * Downloading a media from a new item (thus not played) should change the item to not played.
     */
    @Test
    public void testDownloadMediaOfNewItem_changedToNotPlayedItem() {
        FeedItem item = mock(FeedItem.class);
        when(item.isNew()).thenReturn(true);
        when(item.isPlayed()).thenReturn(false);

        media.setItem(item);
        media.setDownloaded(true);

        verify(item).setPlayed(false);
        verify(item, never()).setNew();
        verify(item, never()).setPlayed(true);
    }

}
