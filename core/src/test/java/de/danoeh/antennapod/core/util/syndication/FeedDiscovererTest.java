package de.danoeh.antennapod.core.util.syndication;

import androidx.test.platform.app.InstrumentationRegistry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link FeedDiscoverer}
 */
@RunWith(RobolectricTestRunner.class)
public class FeedDiscovererTest {

    private FeedDiscoverer fd;

    private File testDir;

    @Before
    public void setUp() {
        fd = new FeedDiscoverer();
        testDir = new File(InstrumentationRegistry
                .getInstrumentation().getTargetContext().getFilesDir(), "FeedDiscovererTest");
        //noinspection ResultOfMethodCallIgnored
        testDir.mkdir();
        assertTrue(testDir.exists());
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(testDir);
    }

    @SuppressWarnings("SameParameterValue")
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
            IOUtils.write(html, out, StandardCharsets.UTF_8);
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

    @Test
    public void testAlternateRSSWithTitleAbsolute() throws Exception {
        checkFindUrls(true, true, true, true, true);
    }

    @Test
    public void testAlternateRSSWithTitleRelative() throws Exception {
        checkFindUrls(true, true, true, false, true);
    }

    @Test
    public void testAlternateRSSNoTitleAbsolute() throws Exception {
        checkFindUrls(true, true, false, true, true);
    }

    @Test
    public void testAlternateRSSNoTitleRelative() throws Exception {
        checkFindUrls(true, true, false, false, true);
    }

    @Test
    public void testAlternateAtomWithTitleAbsolute() throws Exception {
        checkFindUrls(true, false, true, true, true);
    }

    @Test
    public void testFeedAtomWithTitleAbsolute() throws Exception {
        checkFindUrls(false, false, true, true, true);
    }

    @Test
    public void testAlternateRSSWithTitleAbsoluteFromFile() throws Exception {
        checkFindUrls(true, true, true, true, false);
    }
}
