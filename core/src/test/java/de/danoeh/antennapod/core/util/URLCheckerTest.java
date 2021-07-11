package de.danoeh.antennapod.core.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link URLChecker}
 */
@RunWith(RobolectricTestRunner.class)
public class URLCheckerTest {

    @Test
    public void testCorrectURLHttp() {
        final String in = "http://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals(in, out);
    }

    @Test
    public void testCorrectURLHttps() {
        final String in = "https://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals(in, out);
    }

    @Test
    public void testMissingProtocol() {
        final String in = "example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testFeedProtocol() {
        final String in = "feed://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testPcastProtocolNoScheme() {
        final String in = "pcast://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testItpcProtocol() {
        final String in = "itpc://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testItpcProtocolWithScheme() {
        final String in = "itpc://https://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testWhiteSpaceUrlShouldNotAppend() {
        final String in = "\n http://example.com \t";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testWhiteSpaceShouldAppend() {
        final String in = "\n example.com \t";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testAntennaPodSubscribeProtocolNoScheme() {
        final String in = "antennapod-subscribe://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testPcastProtocolWithScheme() {
        final String in = "pcast://https://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testAntennaPodSubscribeProtocolWithScheme() {
        final String in = "antennapod-subscribe://https://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testProtocolRelativeUrlIsAbsolute() {
        final String in = "https://example.com";
        final String inBase = "http://examplebase.com";
        final String out = URLChecker.prepareURL(in, inBase);
        assertEquals(in, out);
    }

    @Test
    public void testProtocolRelativeUrlIsRelativeHttps() {
        final String in = "//example.com";
        final String inBase = "https://examplebase.com";
        final String out = URLChecker.prepareURL(in, inBase);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testProtocolRelativeUrlIsHttpsWithApSubscribeProtocol() {
        final String in = "//example.com";
        final String inBase = "antennapod-subscribe://https://examplebase.com";
        final String out = URLChecker.prepareURL(in, inBase);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testProtocolRelativeUrlBaseUrlNull() {
        final String in = "example.com";
        final String out = URLChecker.prepareURL(in, null);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testUrlEqualsSame() {
        assertTrue(URLChecker.urlEquals("https://www.example.com/test", "https://www.example.com/test"));
        assertTrue(URLChecker.urlEquals("https://www.example.com/test", "https://www.example.com/test/"));
        assertTrue(URLChecker.urlEquals("https://www.example.com/test", "https://www.example.com//test"));
        assertTrue(URLChecker.urlEquals("https://www.example.com", "https://www.example.com/"));
        assertTrue(URLChecker.urlEquals("https://www.example.com", "http://www.example.com"));
        assertTrue(URLChecker.urlEquals("http://www.example.com/", "https://www.example.com/"));
        assertTrue(URLChecker.urlEquals("https://www.example.com/?id=42", "https://www.example.com/?id=42"));
        assertTrue(URLChecker.urlEquals("https://example.com/podcast%20test", "https://example.com/podcast test"));
        assertTrue(URLChecker.urlEquals("https://example.com/?a=podcast%20test", "https://example.com/?a=podcast test"));
        assertTrue(URLChecker.urlEquals("https://example.com/?", "https://example.com/"));
        assertTrue(URLChecker.urlEquals("https://example.com/?", "https://example.com"));
        assertTrue(URLChecker.urlEquals("https://Example.com", "https://example.com"));
        assertTrue(URLChecker.urlEquals("https://example.com/test", "https://example.com/Test"));
    }

    @Test
    public void testUrlEqualsDifferent() {
        assertFalse(URLChecker.urlEquals("https://www.example.com/test", "https://www.example2.com/test"));
        assertFalse(URLChecker.urlEquals("https://www.example.com/test", "https://www.example.de/test"));
        assertFalse(URLChecker.urlEquals("https://example.com/", "https://otherpodcast.example.com/"));
        assertFalse(URLChecker.urlEquals("https://www.example.com/?id=42&a=b", "https://www.example.com/?id=43&a=b"));
        assertFalse(URLChecker.urlEquals("https://example.com/podcast%25test", "https://example.com/podcast test"));
    }
}
