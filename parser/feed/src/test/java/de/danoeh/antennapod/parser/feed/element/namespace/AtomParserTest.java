package de.danoeh.antennapod.parser.feed.element.namespace;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.Date;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for Atom feeds in FeedHandler.
 */
@RunWith(RobolectricTestRunner.class)
public class AtomParserTest {

    @Test
    public void testAtomBasic() throws Exception {
        File feedFile = FeedParserTestHelper.getFeedFile("feed-atom-testAtomBasic.xml");
        Feed feed = FeedParserTestHelper.runFeedParser(feedFile);
        assertEquals(Feed.TYPE_ATOM1, feed.getType());
        assertEquals("title", feed.getTitle());
        assertEquals("http://example.com/feed", feed.getFeedIdentifier());
        assertEquals("http://example.com", feed.getLink());
        assertEquals("This is the description", feed.getDescription());
        assertEquals("http://example.com/payment", feed.getPaymentLinks().get(0).url);
        assertEquals("http://example.com/picture", feed.getImageUrl());
        assertEquals(10, feed.getItems().size());
        for (int i = 0; i < feed.getItems().size(); i++) {
            FeedItem item = feed.getItems().get(i);
            assertEquals("http://example.com/item-" + i, item.getItemIdentifier());
            assertEquals("item-" + i, item.getTitle());
            assertNull(item.getDescription());
            assertEquals("http://example.com/items/" + i, item.getLink());
            assertEquals(new Date(i * 60000), item.getPubDate());
            assertNull(item.getPaymentLink());
            assertEquals("http://example.com/picture", item.getImageLocation());
            // media
            assertTrue(item.hasMedia());
            FeedMedia media = item.getMedia();
            //noinspection ConstantConditions
            assertEquals("http://example.com/media-" + i, media.getDownload_url());
            assertEquals(1024 * 1024, media.getSize());
            assertEquals("audio/mp3", media.getMime_type());
            // chapters
            assertNull(item.getChapters());
        }
    }

    @Test
    public void testEmptyRelLinks() throws Exception {
        File feedFile = FeedParserTestHelper.getFeedFile("feed-atom-testEmptyRelLinks.xml");
        Feed feed = FeedParserTestHelper.runFeedParser(feedFile);
        assertEquals(Feed.TYPE_ATOM1, feed.getType());
        assertEquals("title", feed.getTitle());
        assertEquals("http://example.com/feed", feed.getFeedIdentifier());
        assertEquals("http://example.com", feed.getLink());
        assertEquals("This is the description", feed.getDescription());
        assertNull(feed.getPaymentLinks());
        assertEquals("http://example.com/picture", feed.getImageUrl());
        assertEquals(1, feed.getItems().size());

        // feed entry
        FeedItem item = feed.getItems().get(0);
        assertEquals("http://example.com/item-0", item.getItemIdentifier());
        assertEquals("item-0", item.getTitle());
        assertNull(item.getDescription());
        assertEquals("http://example.com/items/0", item.getLink());
        assertEquals(new Date(0), item.getPubDate());
        assertNull(item.getPaymentLink());
        assertEquals("http://example.com/picture", item.getImageLocation());
        // media
        assertFalse(item.hasMedia());
        // chapters
        assertNull(item.getChapters());
    }

    @Test
    public void testLogoWithWhitespace() throws Exception {
        File feedFile = FeedParserTestHelper.getFeedFile("feed-atom-testLogoWithWhitespace.xml");
        Feed feed = FeedParserTestHelper.runFeedParser(feedFile);
        assertEquals("title", feed.getTitle());
        assertEquals("http://example.com/feed", feed.getFeedIdentifier());
        assertEquals("http://example.com", feed.getLink());
        assertEquals("This is the description", feed.getDescription());
        assertEquals("http://example.com/payment", feed.getPaymentLinks().get(0).url);
        assertEquals("https://example.com/image.png", feed.getImageUrl());
        assertEquals(0, feed.getItems().size());
    }
}
