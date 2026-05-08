package de.danoeh.antennapod.storage.importexport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.StringReader;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class OpmlReaderTest {

    @Test
    public void testReadSimple() throws Exception {
        String opml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<opml version=\"2.0\">"
                + "<head><title>Test</title></head>"
                + "<body>"
                + "<outline text=\"Feed Text\" title=\"Feed 1\" type=\"rss\""
                + " xmlUrl=\"https://example.com/feed1.xml\" htmlUrl=\"https://example.com/1\"/>"
                + "<outline text=\"Feed 2\" title=\"Feed 2\" xmlUrl=\"https://example.com/feed2.xml\"/>"
                + "</body>"
                + "</opml>";
        OpmlReader reader = new OpmlReader();
        ArrayList<OpmlElement> result = reader.readDocument(new StringReader(opml));
        assertEquals(2, result.size());
        assertEquals("Feed 1", result.get(0).getText());
        assertEquals("https://example.com/feed1.xml", result.get(0).getXmlUrl());
        assertEquals("https://example.com/1", result.get(0).getHtmlUrl());
        assertEquals("rss", result.get(0).getType());
        assertEquals("Feed 2", result.get(1).getText());
        assertEquals("https://example.com/feed2.xml", result.get(1).getXmlUrl());
    }

    @Test
    public void testSkipsFeedWithoutXmlUrl() throws Exception {
        String opml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<opml version=\"2.0\">"
                + "<body>"
                + "<outline text=\"No Url\" title=\"No Url\"/>"
                + "<outline text=\"With Url\" title=\"With Url\" xmlUrl=\"https://example.com/feed.xml\"/>"
                + "</body>"
                + "</opml>";
        OpmlReader reader = new OpmlReader();
        ArrayList<OpmlElement> result = reader.readDocument(new StringReader(opml));
        assertEquals(1, result.size());
        assertEquals("With Url", result.get(0).getText());
    }

    @Test
    public void testTitlePreferredOverText() throws Exception {
        String opml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<opml version=\"2.0\">"
                + "<body>"
                + "<outline text=\"Text Attr\" title=\"Title Attr\" xmlUrl=\"https://example.com/feed.xml\"/>"
                + "</body>"
                + "</opml>";
        OpmlReader reader = new OpmlReader();
        ArrayList<OpmlElement> result = reader.readDocument(new StringReader(opml));
        assertEquals("Title Attr", result.get(0).getText());
    }

    @Test
    public void testTextUsedWhenNoTitle() throws Exception {
        String opml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<opml version=\"2.0\">"
                + "<body>"
                + "<outline text=\"Text Attr\" xmlUrl=\"https://example.com/feed.xml\"/>"
                + "</body>"
                + "</opml>";
        OpmlReader reader = new OpmlReader();
        ArrayList<OpmlElement> result = reader.readDocument(new StringReader(opml));
        assertEquals("Text Attr", result.get(0).getText());
    }

    @Test
    public void testXmlUrlUsedAsTextWhenNeitherPresent() throws Exception {
        String opml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<opml version=\"2.0\">"
                + "<body>"
                + "<outline xmlUrl=\"https://example.com/feed.xml\"/>"
                + "</body>"
                + "</opml>";
        OpmlReader reader = new OpmlReader();
        ArrayList<OpmlElement> result = reader.readDocument(new StringReader(opml));
        assertEquals(1, result.size());
        assertEquals("https://example.com/feed.xml", result.get(0).getText());
    }

    @Test
    public void testNestedCategoryFeeds() throws Exception {
        String opml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<opml version=\"2.0\">"
                + "<body>"
                + "<outline text=\"Tech\">"
                + "<outline title=\"Feed 1\" xmlUrl=\"https://example.com/feed1.xml\"/>"
                + "<outline title=\"Feed 2\" xmlUrl=\"https://example.com/feed2.xml\"/>"
                + "</outline>"
                + "</body>"
                + "</opml>";
        OpmlReader reader = new OpmlReader();
        ArrayList<OpmlElement> result = reader.readDocument(new StringReader(opml));
        assertEquals(2, result.size());
        assertEquals("Feed 1", result.get(0).getText());
        assertEquals("Feed 2", result.get(1).getText());
    }

    @Test
    public void testHtmlUrlAndTypeAreOptional() throws Exception {
        String opml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<opml version=\"2.0\">"
                + "<body>"
                + "<outline title=\"Minimal\" xmlUrl=\"https://example.com/feed.xml\"/>"
                + "</body>"
                + "</opml>";
        OpmlReader reader = new OpmlReader();
        ArrayList<OpmlElement> result = reader.readDocument(new StringReader(opml));
        assertEquals(1, result.size());
        assertNull(result.get(0).getHtmlUrl());
        assertNull(result.get(0).getType());
    }
}
