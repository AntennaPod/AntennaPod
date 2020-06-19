package de.test.antennapod.handler;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import de.danoeh.antennapod.core.feed.Feed;
import de.test.antennapod.util.syndication.feedgenerator.AtomGenerator;
import de.test.antennapod.util.syndication.feedgenerator.RSS2Generator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Atom feeds in FeedHandler.
 */
@SmallTest
public class AtomParserTest extends FeedParserTestBase {
    @Test
    public void testAtomBasic() throws Exception {
        Feed f1 = createTestFeed(10, true);
        Feed f2 = runFeedTest(f1, new AtomGenerator(), "UTF-8", 0);
        feedValid(f1, f2, Feed.TYPE_ATOM1);
    }

    @Test
    public void testLogoWithWhitespace() throws Exception {
        String logo = "https://example.com/image.png";
        Feed f1 = createTestFeed(0, false);
        f1.setImageUrl(null);
        Feed f2 = runFeedTest(f1, new AtomGenerator() {
            @Override
            protected void writeAdditionalAttributes(XmlSerializer xml) throws IOException {
                xml.startTag(null, "logo");
                xml.text(" " + logo + "\n");
                xml.endTag(null, "logo");
            }
        }, "UTF-8", 0);
        assertEquals(logo, f2.getImageUrl());
    }
}
