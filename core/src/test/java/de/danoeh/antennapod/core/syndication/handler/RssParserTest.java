package de.danoeh.antennapod.core.syndication.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.Date;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for RSS feeds in FeedHandler.
 */
@RunWith(RobolectricTestRunner.class)
public class RssParserTest {

    @Test
    public void testRss2Basic() throws Exception {
        File feedFile = FeedParserTestHelper.getFeedFile("feed-rss-testRss2Basic.xml");
        Feed feed = FeedParserTestHelper.runFeedParser(feedFile);
        assertEquals(Feed.TYPE_RSS2, feed.getType());
        assertEquals("title", feed.getTitle());
        assertEquals("en", feed.getLanguage());
        assertEquals("http://example.com", feed.getLink());
        assertEquals("This is the description", feed.getDescription());
        assertEquals("http://example.com/payment", feed.getPaymentLink());
        assertEquals("http://example.com/picture", feed.getImageUrl());
        assertEquals(10, feed.getItems().size());
        for (int i = 0; i < feed.getItems().size(); i++) {
            FeedItem item = feed.getItems().get(i);
            assertEquals("http://example.com/item-" + i, item.getItemIdentifier());
            assertEquals("item-" + i, item.getTitle());
            assertNull(item.getDescription());
            assertNull(item.getContentEncoded());
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
    public void testImageWithWhitespace() throws Exception {
        File feedFile = FeedParserTestHelper.getFeedFile("feed-rss-testImageWithWhitespace.xml");
        Feed feed = FeedParserTestHelper.runFeedParser(feedFile);
        assertEquals("title", feed.getTitle());
        assertEquals("http://example.com", feed.getLink());
        assertEquals("This is the description", feed.getDescription());
        assertEquals("http://example.com/payment", feed.getPaymentLink());
        assertEquals("https://example.com/image.png", feed.getImageUrl());
        assertEquals(0, feed.getItems().size());
    }

    @Test
    public void testMediaContentMime() throws Exception {
        File feedFile = FeedParserTestHelper.getFeedFile("feed-rss-testMediaContentMime.xml");
        Feed feed = FeedParserTestHelper.runFeedParser(feedFile);
        assertEquals("title", feed.getTitle());
        assertEquals("http://example.com", feed.getLink());
        assertEquals("This is the description", feed.getDescription());
        assertEquals("http://example.com/payment", feed.getPaymentLink());
        assertNull(feed.getImageUrl());
        assertEquals(1, feed.getItems().size());
        FeedItem feedItem = feed.getItems().get(0);
        //noinspection ConstantConditions
        assertEquals(MediaType.VIDEO, feedItem.getMedia().getMediaType());
        assertEquals("https://www.example.com/file.mp4", feedItem.getMedia().getDownload_url());
    }
}
