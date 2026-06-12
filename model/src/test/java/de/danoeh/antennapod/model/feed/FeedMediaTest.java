package de.danoeh.antennapod.model.feed;

import org.junit.Before;
import org.junit.Test;

import static de.danoeh.antennapod.model.feed.FeedMediaMother.anyFeedMedia;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
        media.setDownloaded(true, System.currentTimeMillis());

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
        media.setDownloaded(true, System.currentTimeMillis());

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
        media.setDownloaded(true, System.currentTimeMillis());

        verify(item).setPlayed(false);
        verify(item, never()).setNew();
        verify(item, never()).setPlayed(true);
    }

    @Test
    public void testIsMarkedForRedownload_whenDownloadDateIsMinusOne_returnsTrue() {
        FeedMedia m = new FeedMedia(1L, null, 0, 0, 0L, "audio/mp3", null,
                "http://example.com/ep", -1L, null, 0, 0L);
        assertTrue(m.isMarkedForRedownload());
    }

    @Test
    public void testIsMarkedForRedownload_whenNotDownloaded_returnsFalse() {
        assertFalse(media.isMarkedForRedownload());
    }

    @Test
    public void testIsMarkedForRedownload_whenDownloaded_returnsFalse() {
        FeedMedia m = new FeedMedia(1L, null, 0, 0, 0L, "audio/mp3", null,
                "http://example.com/ep", System.currentTimeMillis(), null, 0, 0L);
        assertFalse(m.isMarkedForRedownload());
    }

    @Test
    public void testIsDownloaded_whenDownloadDateIsMinusOne_returnsFalse() {
        FeedMedia m = new FeedMedia(1L, null, 0, 0, 0L, "audio/mp3", null,
                "http://example.com/ep", -1L, null, 0, 0L);
        assertFalse(m.isDownloaded());
    }

    @Test
    public void testSetWantsRedownload_isMarkedForRedownload() {
        media.setWantsRedownload();
        assertTrue(media.isMarkedForRedownload());
    }

    @Test
    public void testSetWantsRedownload_clearsLocalFileUrl() {
        FeedMedia m = new FeedMedia(1L, null, 0, 0, 0L, "audio/mp3", "/path/to/file.mp3",
                "http://example.com/ep", System.currentTimeMillis(), null, 0, 0L);
        m.setWantsRedownload();
        assertNull(m.getLocalFileUrl());
    }

    @Test
    public void testSetWantsRedownload_isNotDownloaded() {
        media.setWantsRedownload();
        assertFalse(media.isDownloaded());
    }

}
