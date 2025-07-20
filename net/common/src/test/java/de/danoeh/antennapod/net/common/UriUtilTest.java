package de.danoeh.antennapod.net.common;

import de.danoeh.antennapod.net.common.UriUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

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

    @Test
    public void testUrlEncode() {
        String testUrl = "http://example.com/this is not encoded";
        String expected = "http%3A%2F%2Fexample.com%2Fthis+is+not+encoded";
        assertEquals(expected, UriUtil.urlEncode(testUrl));
    }

    @Test
    public void testQueryString() {
        Map<String, String> params = HashMap.newHashMap(2);
        params.put("key1", "value1");
        params.put("key2", "value2");
        String expected = "key1=value1&key2=value2";
        assertEquals(expected, UriUtil.queryString(params));
    }
}
