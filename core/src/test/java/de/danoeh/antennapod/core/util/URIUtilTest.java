package de.danoeh.antennapod.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test class for URIUtil
 */
public class URIUtilTest {

    @Test
    public void testGetURIFromRequestUrlShouldNotEncode() {
        final String testUrl = "http://example.com/this%20is%20encoded";
        assertEquals(testUrl, URIUtil.getURIFromRequestUrl(testUrl).toString());
    }

    @Test
    public void testGetURIFromRequestUrlShouldEncode() {
        final String testUrl = "http://example.com/this is not encoded";
        final String expected = "http://example.com/this%20is%20not%20encoded";
        assertEquals(expected, URIUtil.getURIFromRequestUrl(testUrl).toString());
    }
}
