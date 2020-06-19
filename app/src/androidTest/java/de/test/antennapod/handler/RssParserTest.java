package de.test.antennapod.handler;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import de.danoeh.antennapod.core.feed.Feed;
import de.test.antennapod.util.syndication.feedgenerator.RSS2Generator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Tests for RSS feeds in FeedHandler.
 */
@SmallTest
public class RssParserTest extends FeedParserTestBase {
    @Test
    public void testRss2Basic() throws Exception {
        Feed f1 = createTestFeed(10, true);
        Feed f2 = runFeedTest(f1, new RSS2Generator(), "UTF-8", RSS2Generator.FEATURE_WRITE_GUID);
        feedValid(f1, f2, Feed.TYPE_RSS2);
    }

    @Test
    public void testImageWithWhitespace() throws Exception {
        String image = "https://example.com/image.png";
        Feed f1 = createTestFeed(0, false);
        f1.setImageUrl(null);
        Feed f2 = runFeedTest(f1, new RSS2Generator() {
            @Override
            protected void writeAdditionalAttributes(XmlSerializer xml) throws IOException {
                xml.startTag(null, "image");
                xml.startTag(null, "url");
                xml.text(" " + image + "\n");
                xml.endTag(null, "url");
                xml.endTag(null, "image");
            }
        }, "UTF-8", 0);
        assertEquals(image, f2.getImageUrl());
    }
}
