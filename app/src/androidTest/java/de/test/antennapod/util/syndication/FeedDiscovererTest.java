package de.test.antennapod.util.syndication;

import android.test.InstrumentationTestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import de.danoeh.antennapod.core.util.syndication.FeedDiscoverer;

/**
 * Test class for FeedDiscoverer
 */
public class FeedDiscovererTest extends InstrumentationTestCase {

    private FeedDiscoverer fd;

    private File testDir;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        fd = new FeedDiscoverer();
        testDir = getInstrumentation().getTargetContext().getExternalFilesDir("FeedDiscovererTest");
        testDir.mkdir();
        assertTrue(testDir.exists());
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteDirectory(testDir);
        super.tearDown();
    }

    private String createTestHtmlString(String rel, String type, String href, String title) {
        return String.format("<html><head><title>Test</title><link rel=\"%s\" type=\"%s\" href=\"%s\" title=\"%s\"></head><body></body></html>",
                rel, type, href, title);
    }

    private String createTestHtmlString(String rel, String type, String href) {
        return String.format("<html><head><title>Test</title><link rel=\"%s\" type=\"%s\" href=\"%s\"></head><body></body></html>",
                rel, type, href);
    }

    private void checkFindUrls(boolean isAlternate, boolean isRss, boolean withTitle, boolean isAbsolute, boolean fromString) throws Exception {
        final String title = "Test title";
        final String hrefAbs = "http://example.com/feed";
        final String hrefRel = "/feed";
        final String base = "http://example.com";

        final String rel = (isAlternate) ? "alternate" : "feed";
        final String type = (isRss) ? "application/rss+xml" : "application/atom+xml";
        final String href = (isAbsolute) ? hrefAbs : hrefRel;

        Map<String, String> res;
        String html = (withTitle) ? createTestHtmlString(rel, type, href, title)
                : createTestHtmlString(rel, type, href);
        if (fromString) {
            res = fd.findLinks(html, base);
        } else {
            File testFile = new File(testDir, "feed");
            FileOutputStream out = new FileOutputStream(testFile);
            IOUtils.write(html, out);
            out.close();
            res = fd.findLinks(testFile, base);
        }

        assertNotNull(res);
        assertEquals(1, res.size());
        for (String key : res.keySet()) {
            assertEquals(hrefAbs, key);
        }
        assertTrue(res.containsKey(hrefAbs));
        if (withTitle) {
            assertEquals(title, res.get(hrefAbs));
        } else {
            assertEquals(href, res.get(hrefAbs));
        }
    }

    public void testAlternateRSSWithTitleAbsolute() throws Exception {
        checkFindUrls(true, true, true, true, true);
    }

    public void testAlternateRSSWithTitleRelative() throws Exception {
        checkFindUrls(true, true, true, false, true);
    }

    public void testAlternateRSSNoTitleAbsolute() throws Exception {
        checkFindUrls(true, true, false, true, true);
    }

    public void testAlternateRSSNoTitleRelative() throws Exception {
        checkFindUrls(true, true, false, false, true);
    }

    public void testAlternateAtomWithTitleAbsolute() throws Exception {
        checkFindUrls(true, false, true, true, true);
    }

    public void testFeedAtomWithTitleAbsolute() throws Exception {
        checkFindUrls(false, false, true, true, true);
    }

    public void testAlternateRSSWithTitleAbsoluteFromFile() throws Exception {
        checkFindUrls(true, true, true, true, false);
    }
}
