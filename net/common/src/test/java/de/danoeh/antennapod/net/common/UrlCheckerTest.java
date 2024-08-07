package de.danoeh.antennapod.net.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link UrlChecker}
 */
@RunWith(RobolectricTestRunner.class)
public class UrlCheckerTest {

    @Test
    public void testCorrectURLHttp() {
        final String in = "http://example.com";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals(in, out);
    }

    @Test
    public void testCorrectURLHttps() {
        final String in = "https://example.com";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals(in, out);
    }

    @Test
    public void testMissingProtocol() {
        final String in = "example.com";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testFeedProtocol() {
        final String in = "feed://example.com";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testPcastProtocolNoScheme() {
        final String in = "pcast://example.com";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testItpcProtocol() {
        final String in = "itpc://example.com";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testItpcProtocolWithScheme() {
        final String in = "itpc://https://example.com";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testWhiteSpaceUrlShouldNotAppend() {
        final String in = "\n http://example.com \t";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testWhiteSpaceShouldAppend() {
        final String in = "\n example.com \t";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testAntennaPodSubscribeProtocolNoScheme() {
        final String in = "antennapod-subscribe://example.com";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testPcastProtocolWithScheme() {
        final String in = "pcast://https://example.com";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testAntennaPodSubscribeProtocolWithScheme() {
        final String in = "antennapod-subscribe://https://example.com";
        final String out = UrlChecker.prepareUrl(in);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testAntennaPodSubscribeDeeplink() throws UnsupportedEncodingException {
        final String feed = "http://example.org/podcast.rss";
        assertEquals(feed, UrlChecker.prepareUrl("https://antennapod.org/deeplink/subscribe?url=" + feed));
        assertEquals(feed, UrlChecker.prepareUrl("http://antennapod.org/deeplink/subscribe?url=" + feed));
        assertEquals(feed, UrlChecker.prepareUrl("http://antennapod.org/deeplink/subscribe/?url=" + feed));
        assertEquals(feed, UrlChecker.prepareUrl("https://www.antennapod.org/deeplink/subscribe?url=" + feed));
        assertEquals(feed, UrlChecker.prepareUrl("http://www.antennapod.org/deeplink/subscribe?url=" + feed));
        assertEquals(feed, UrlChecker.prepareUrl("http://www.antennapod.org/deeplink/subscribe/?url=" + feed));
        assertEquals(feed, UrlChecker.prepareUrl("http://www.antennapod.org/deeplink/subscribe?url="
                + URLEncoder.encode(feed, "UTF-8")));
        assertEquals(feed, UrlChecker.prepareUrl("http://www.antennapod.org/deeplink/subscribe?url="
                + "example.org/podcast.rss"));
        assertEquals(feed, UrlChecker.prepareUrl("https://antennapod.org/deeplink/subscribe?url=" + feed + "&title=a"));
        assertEquals(feed, UrlChecker.prepareUrl("https://antennapod.org/deeplink/subscribe?url="
                + URLEncoder.encode(feed) + "&title=a"));
    }

    @Test
    public void testProtocolRelativeUrlIsAbsolute() {
        final String in = "https://example.com";
        final String inBase = "http://examplebase.com";
        final String out = UrlChecker.prepareUrl(in, inBase);
        assertEquals(in, out);
    }

    @Test
    public void testProtocolRelativeUrlIsRelativeHttps() {
        final String in = "//example.com";
        final String inBase = "https://examplebase.com";
        final String out = UrlChecker.prepareUrl(in, inBase);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testProtocolRelativeUrlIsHttpsWithApSubscribeProtocol() {
        final String in = "//example.com";
        final String inBase = "antennapod-subscribe://https://examplebase.com";
        final String out = UrlChecker.prepareUrl(in, inBase);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testProtocolRelativeUrlBaseUrlNull() {
        final String in = "example.com";
        final String out = UrlChecker.prepareUrl(in, null);
        assertEquals("http://example.com", out);
    }

    @Test
    public void testUrlEqualsSame() {
        assertTrue(UrlChecker.urlEquals("https://www.example.com/test", "https://www.example.com/test"));
        assertTrue(UrlChecker.urlEquals("https://www.example.com/test", "https://www.example.com/test/"));
        assertTrue(UrlChecker.urlEquals("https://www.example.com/test", "https://www.example.com//test"));
        assertTrue(UrlChecker.urlEquals("https://www.example.com", "https://www.example.com/"));
        assertTrue(UrlChecker.urlEquals("https://www.example.com", "http://www.example.com"));
        assertTrue(UrlChecker.urlEquals("http://www.example.com/", "https://www.example.com/"));
        assertTrue(UrlChecker.urlEquals("https://www.example.com/?id=42", "https://www.example.com/?id=42"));
        assertTrue(UrlChecker.urlEquals("https://example.com/podcast%20test", "https://example.com/podcast test"));
        assertTrue(UrlChecker.urlEquals("https://example.com/?a=podcast%20test", "https://example.com/?a=podcast test"));
        assertTrue(UrlChecker.urlEquals("https://example.com/?", "https://example.com/"));
        assertTrue(UrlChecker.urlEquals("https://example.com/?", "https://example.com"));
        assertTrue(UrlChecker.urlEquals("https://Example.com", "https://example.com"));
        assertTrue(UrlChecker.urlEquals("https://example.com/test", "https://example.com/Test"));
        assertTrue(UrlChecker.urlEquals("antennapod_local:abc", "antennapod_local:abc"));
    }

    @Test
    public void testUrlEqualsDifferent() {
        assertFalse(UrlChecker.urlEquals("https://www.example.com/test", "https://www.example2.com/test"));
        assertFalse(UrlChecker.urlEquals("https://www.example.com/test", "https://www.example.de/test"));
        assertFalse(UrlChecker.urlEquals("https://example.com/", "https://otherpodcast.example.com/"));
        assertFalse(UrlChecker.urlEquals("https://www.example.com/?id=42&a=b", "https://www.example.com/?id=43&a=b"));
        assertFalse(UrlChecker.urlEquals("https://example.com/podcast%25test", "https://example.com/podcast test"));
        assertFalse(UrlChecker.urlEquals("antennapod_local:abc", "https://example.com/"));
    }
}
