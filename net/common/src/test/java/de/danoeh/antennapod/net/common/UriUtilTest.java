package de.danoeh.antennapod.net.common;

import de.danoeh.antennapod.net.common.UriUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test class for URIUtil
 */
public class UriUtilTest {

    @Test
    public void testGetURIFromRequestUrlShouldNotEncode() {
        final String testUrl = "http://example.com/this%20is%20encoded";
        assertEquals(testUrl, UriUtil.getURIFromRequestUrl(testUrl).toString());
    }

    @Test
    public void testGetURIFromRequestUrlShouldEncode() {
        final String testUrl = "http://example.com/this is not encoded";
        final String expected = "http://example.com/this%20is%20not%20encoded";
        assertEquals(expected, UriUtil.getURIFromRequestUrl(testUrl).toString());
    }
}
