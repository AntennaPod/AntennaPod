package de.danoeh.antennapod.storage.importexport;

import de.danoeh.antennapod.model.feed.Feed;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class OpmlWriterTest {

    private Feed createFeed(String title, String downloadUrl, String link, String type) {
        return new Feed(0, null, title, link, null, null, null, null, type, null, null, null, downloadUrl, 0);
    }

    @Test
    public void testWriteSingleFeed() throws Exception {
        Feed feed = createFeed("Test Feed", "https://example.com/feed.xml", "https://example.com", "rss");
        StringWriter writer = new StringWriter();
        OpmlWriter.writeDocument(Collections.singletonList(feed), writer);

        List<OpmlElement> elements = new OpmlReader().readDocument(new StringReader(writer.toString()));
        assertEquals(1, elements.size());
        assertEquals("Test Feed", elements.get(0).getText());
        assertEquals("https://example.com/feed.xml", elements.get(0).getXmlUrl());
        assertEquals("https://example.com", elements.get(0).getHtmlUrl());
        assertEquals("rss", elements.get(0).getType());
    }

    @Test
    public void testWriteMultipleFeeds() throws Exception {
        Feed feed1 = createFeed("Feed One", "https://example.com/feed1.xml", "https://example.com/1", null);
        Feed feed2 = createFeed("Feed Two", "https://example.com/feed2.xml", "https://example.com/2", null);
        StringWriter writer = new StringWriter();
        OpmlWriter.writeDocument(Arrays.asList(feed1, feed2), writer);

        List<OpmlElement> elements = new OpmlReader().readDocument(new StringReader(writer.toString()));
        assertEquals(2, elements.size());
        assertEquals("Feed One", elements.get(0).getText());
        assertEquals("https://example.com/feed1.xml", elements.get(0).getXmlUrl());
        assertEquals("Feed Two", elements.get(1).getText());
        assertEquals("https://example.com/feed2.xml", elements.get(1).getXmlUrl());
    }

    @Test
    public void testNonSubscribedFeedExcluded() throws Exception {
        Feed subscribed = createFeed("Subscribed", "https://example.com/sub.xml", null, null);
        Feed notSubscribed = createFeed("Not Subscribed", "https://example.com/unsub.xml", null, null);
        notSubscribed.setState(Feed.STATE_NOT_SUBSCRIBED);
        StringWriter writer = new StringWriter();
        OpmlWriter.writeDocument(Arrays.asList(subscribed, notSubscribed), writer);

        List<OpmlElement> elements = new OpmlReader().readDocument(new StringReader(writer.toString()));
        assertEquals(1, elements.size());
        assertEquals("Subscribed", elements.get(0).getText());
    }

    @Test
    public void testFeedWithNullLinkAndType() throws Exception {
        Feed feed = createFeed("Feed Without Link", "https://example.com/feed.xml", null, null);
        StringWriter writer = new StringWriter();
        OpmlWriter.writeDocument(Collections.singletonList(feed), writer);

        List<OpmlElement> elements = new OpmlReader().readDocument(new StringReader(writer.toString()));
        assertEquals(1, elements.size());
        assertNull(elements.get(0).getHtmlUrl());
        assertNull(elements.get(0).getType());
    }

    @Test
    public void testEmptyFeedList() throws Exception {
        StringWriter writer = new StringWriter();
        OpmlWriter.writeDocument(Collections.emptyList(), writer);

        List<OpmlElement> elements = new OpmlReader().readDocument(new StringReader(writer.toString()));
        assertEquals(0, elements.size());
    }

    @Test
    public void testArchivedFeedExcluded() throws Exception {
        Feed subscribed = createFeed("Active", "https://example.com/active.xml", null, null);
        Feed archived = createFeed("Archived", "https://example.com/archived.xml", null, null);
        archived.setState(Feed.STATE_ARCHIVED);
        StringWriter writer = new StringWriter();
        OpmlWriter.writeDocument(Arrays.asList(subscribed, archived), writer);

        List<OpmlElement> elements = new OpmlReader().readDocument(new StringReader(writer.toString()));
        assertEquals(1, elements.size());
        assertEquals("Active", elements.get(0).getText());
    }
}
