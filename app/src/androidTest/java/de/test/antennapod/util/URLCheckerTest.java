package de.test.antennapod.util;

import android.test.AndroidTestCase;

import de.danoeh.antennapod.core.util.URLChecker;

/**
 * Test class for URLChecker
 */
public class URLCheckerTest extends AndroidTestCase {

    public void testCorrectURLHttp() {
        final String in = "http://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals(in, out);
    }

    public void testCorrectURLHttps() {
        final String in = "https://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals(in, out);
    }

    public void testMissingProtocol() {
        final String in = "example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    public void testFeedProtocol() {
        final String in = "feed://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    public void testPcastProtocolNoScheme() {
        final String in = "pcast://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    public void testItpcProtocol() {
        final String in = "itpc://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    public void testWhiteSpaceUrlShouldNotAppend() {
        final String in = "\n http://example.com \t";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    public void testWhiteSpaceShouldAppend() {
        final String in = "\n example.com \t";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    public void testAntennaPodSubscribeProtocolNoScheme() throws Exception {
        final String in = "antennapod-subscribe://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("http://example.com", out);
    }

    public void testPcastProtocolWithScheme() {
        final String in = "pcast://https://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("https://example.com", out);
    }

    public void testAntennaPodSubscribeProtocolWithScheme() throws Exception {
        final String in = "antennapod-subscribe://https://example.com";
        final String out = URLChecker.prepareURL(in);
        assertEquals("https://example.com", out);
    }

    public void testProtocolRelativeUrlIsAbsolute() throws Exception {
        final String in = "https://example.com";
        final String inBase = "http://examplebase.com";
        final String out = URLChecker.prepareURL(in, inBase);
        assertEquals(in, out);
    }

    public void testProtocolRelativeUrlIsRelativeHttps() throws Exception {
        final String in = "//example.com";
        final String inBase = "https://examplebase.com";
        final String out = URLChecker.prepareURL(in, inBase);
        assertEquals("https://example.com", out);

    }

    public void testProtocolRelativeUrlIsHttpsWithAPSubscribeProtocol() throws Exception {
        final String in = "//example.com";
        final String inBase = "antennapod-subscribe://https://examplebase.com";
        final String out = URLChecker.prepareURL(in, inBase);
        assertEquals("https://example.com", out);
    }

    public void testProtocolRelativeUrlBaseUrlNull() throws Exception {
        final String in = "example.com";
        final String out = URLChecker.prepareURL(in, null);
        assertEquals("http://example.com", out);
    }
}
