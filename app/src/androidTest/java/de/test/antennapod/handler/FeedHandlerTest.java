package de.test.antennapod.handler;

import android.content.Context;
import android.test.InstrumentationTestCase;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.syndication.handler.FeedHandler;
import de.danoeh.antennapod.core.syndication.handler.UnsupportedFeedtypeException;
import de.test.antennapod.util.syndication.feedgenerator.AtomGenerator;
import de.test.antennapod.util.syndication.feedgenerator.FeedGenerator;
import de.test.antennapod.util.syndication.feedgenerator.RSS2Generator;

/**
 * Tests for FeedHandler
 */
public class FeedHandlerTest extends InstrumentationTestCase {
    private static final String FEEDS_DIR = "testfeeds";

    private File file = null;
    private OutputStream outputStream = null;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getInstrumentation().getContext();
        File destDir = context.getExternalFilesDir(FEEDS_DIR);
        assertNotNull(destDir);

        file = new File(destDir, "feed.xml");
        file.delete();

        assertNotNull(file);
        assertFalse(file.exists());

        outputStream = new FileOutputStream(file);
    }


    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        file.delete();
        file = null;

        outputStream.close();
        outputStream = null;
    }

    private Feed runFeedTest(Feed feed, FeedGenerator g, String encoding, long flags) throws IOException, UnsupportedFeedtypeException, SAXException, ParserConfigurationException {
        g.writeFeed(feed, outputStream, encoding, flags);
        FeedHandler handler = new FeedHandler();
        Feed parsedFeed = new Feed(feed.getDownload_url(), feed.getLastUpdate());
        parsedFeed.setFile_url(file.getAbsolutePath());
        parsedFeed.setDownloaded(true);
        handler.parseFeed(parsedFeed);
        return parsedFeed;
    }

    private void feedValid(Feed feed, Feed parsedFeed, String feedType) {
        assertEquals(feed.getTitle(), parsedFeed.getTitle());
        if (feedType.equals(Feed.TYPE_ATOM1)) {
            assertEquals(feed.getFeedIdentifier(), parsedFeed.getFeedIdentifier());
        } else {
            assertEquals(feed.getLanguage(), parsedFeed.getLanguage());
        }

        assertEquals(feed.getLink(), parsedFeed.getLink());
        assertEquals(feed.getDescription(), parsedFeed.getDescription());
        assertEquals(feed.getPaymentLink(), parsedFeed.getPaymentLink());

        if (feed.getImage() != null) {
            FeedImage image = feed.getImage();
            FeedImage parsedImage = parsedFeed.getImage();
            assertNotNull(parsedImage);

            assertEquals(image.getTitle(), parsedImage.getTitle());
            assertEquals(image.getDownload_url(), parsedImage.getDownload_url());
        }

        if (feed.getItems() != null) {
            assertNotNull(parsedFeed.getItems());
            assertEquals(feed.getItems().size(), parsedFeed.getItems().size());

            for (int i = 0; i < feed.getItems().size(); i++) {
                FeedItem item = feed.getItems().get(i);
                FeedItem parsedItem = parsedFeed.getItems().get(i);

                if (item.getItemIdentifier() != null)
                    assertEquals(item.getItemIdentifier(), parsedItem.getItemIdentifier());
                assertEquals(item.getTitle(), parsedItem.getTitle());
                assertEquals(item.getDescription(), parsedItem.getDescription());
                assertEquals(item.getContentEncoded(), parsedItem.getContentEncoded());
                assertEquals(item.getLink(), parsedItem.getLink());
                assertEquals(item.getPubDate().getTime(), parsedItem.getPubDate().getTime());
                assertEquals(item.getPaymentLink(), parsedItem.getPaymentLink());

                if (item.hasMedia()) {
                    assertTrue(parsedItem.hasMedia());
                    FeedMedia media = item.getMedia();
                    FeedMedia parsedMedia = parsedItem.getMedia();

                    assertEquals(media.getDownload_url(), parsedMedia.getDownload_url());
                    assertEquals(media.getSize(), parsedMedia.getSize());
                    assertEquals(media.getMime_type(), parsedMedia.getMime_type());
                }

                if (item.hasItemImage()) {
                    assertTrue(parsedItem.hasItemImage());
                    FeedImage image = item.getImage();
                    FeedImage parsedImage = parsedItem.getImage();

                    assertEquals(image.getTitle(), parsedImage.getTitle());
                    assertEquals(image.getDownload_url(), parsedImage.getDownload_url());
                }

                if (item.getChapters() != null) {
                    assertNotNull(parsedItem.getChapters());
                    assertEquals(item.getChapters().size(), parsedItem.getChapters().size());
                    List<Chapter> chapters = item.getChapters();
                    List<Chapter> parsedChapters = parsedItem.getChapters();
                    for (int j = 0; j < chapters.size(); j++) {
                        Chapter chapter = chapters.get(j);
                        Chapter parsedChapter = parsedChapters.get(j);

                        assertEquals(chapter.getTitle(), parsedChapter.getTitle());
                        assertEquals(chapter.getLink(), parsedChapter.getLink());
                    }
                }
            }
        }
    }

    public void testRSS2Basic() throws IOException, UnsupportedFeedtypeException, SAXException, ParserConfigurationException {
        Feed f1 = createTestFeed(10, false, true, true);
        Feed f2 = runFeedTest(f1, new RSS2Generator(), "UTF-8", RSS2Generator.FEATURE_WRITE_GUID);
        feedValid(f1, f2, Feed.TYPE_RSS2);
    }

    public void testAtomBasic() throws IOException, UnsupportedFeedtypeException, SAXException, ParserConfigurationException {
        Feed f1 = createTestFeed(10, false, true, true);
        Feed f2 = runFeedTest(f1, new AtomGenerator(), "UTF-8", 0);
        feedValid(f1, f2, Feed.TYPE_ATOM1);
    }

    private Feed createTestFeed(int numItems, boolean withImage, boolean withFeedMedia, boolean withChapters) {
        FeedImage image = null;
        if (withImage) {
            image = new FeedImage(0, "image", null, "http://example.com/picture", false);
        }
        Feed feed = new Feed(0, null, "title", "http://example.com", "This is the description",
                "http://example.com/payment", "Daniel", "en", null, "http://example.com/feed", image, file.getAbsolutePath(),
                "http://example.com/feed", true);
        feed.setItems(new ArrayList<>());

        for (int i = 0; i < numItems; i++) {
            FeedItem item = new FeedItem(0, "item-" + i, "http://example.com/item-" + i,
                    "http://example.com/items/" + i, new Date(i*60000), FeedItem.UNPLAYED, feed);
            feed.getItems().add(item);
            if (withFeedMedia) {
                item.setMedia(new FeedMedia(0, item, 4711, 0, 1024*1024, "audio/mp3", null, "http://example.com/media-" + i,
                        false, null, 0, 0));
            }
        }

        return feed;
    }

}
