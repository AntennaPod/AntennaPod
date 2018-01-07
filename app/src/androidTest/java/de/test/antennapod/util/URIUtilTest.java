package de.test.antennapod.util;

import android.test.AndroidTestCase;

import de.danoeh.antennapod.core.util.URIUtil;

/**
 * Test class for URIUtil
 */
public class URIUtilTest extends AndroidTestCase {

    public void testGetURIFromRequestUrlShouldNotEncode() {
        final String testUrl = "http://example.com/this%20is%20encoded";
        assertEquals(testUrl, URIUtil.getURIFromRequestUrl(testUrl).toString());
    }

    public void testGetURIFromRequestUrlShouldEncode() {
        final String testUrl = "http://example.com/this is not encoded";
        final String expected = "http://example.com/this%20is%20not%20encoded";
        assertEquals(expected, URIUtil.getURIFromRequestUrl(testUrl).toString());
    }
}
