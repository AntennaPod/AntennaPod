package de.danoeh.antennapod.core.syndication.handler;

import de.danoeh.antennapod.core.feed.Feed;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Atom feeds in FeedHandler.
 */
@RunWith(RobolectricTestRunner.class)
public class AtomParserTest extends FeedParserTestBase {
    @Test
    public void testAtomBasic() throws Exception {
        Feed f1 = createTestFeed(10, true);
        Feed f2 = runFeedTest(f1, new AtomGenerator(), 0);
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
        }, 0);
        assertEquals(logo, f2.getImageUrl());
    }
}
