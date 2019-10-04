package de.test.antennapod.util;

import androidx.test.filters.SmallTest;
import de.danoeh.antennapod.core.util.URLChecker;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test class for URLChecker
 */
@SmallTest
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
    public void testAntennaPodSubscribeProtocolNoScheme() throws Exception {
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
    public void testAntennaPodSubscribeProtocolWithScheme() throws Exception {
        final String in = "antennapod-subscribe://https://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testProtocolRelativeUrlIsAbsolute() throws Exception {
        final String in = "https://example.com";
        final String inBase = "http://examplebase.com";
        final String out = URLChecker.prepareURL(in, inBase);
        assertEquals(in, out);
    }

    @Test
    public void testProtocolRelativeUrlIsRelativeHttps() throws Exception {
        final String in = "//example.com";
        final String inBase = "https://examplebase.com";
        final String out = URLChecker.prepareURL(in, inBase);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testProtocolRelativeUrlIsHttpsWithAPSubscribeProtocol() throws Exception {
        final String in = "//example.com";
        final String inBase = "antennapod-subscribe://https://examplebase.com";
        final String out = URLChecker.prepareURL(in, inBase);
        assertEquals("https://example.com", out);
    }

    @Test
    public void testProtocolRelativeUrlBaseUrlNull() throws Exception {
        final String in = "example.com";
        final String out = URLChecker.prepareURL(in, null);
        assertEquals("http://example.com", out);
    }
}
