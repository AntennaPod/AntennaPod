package de.danoeh.antennapod.net.sync;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HostnameParserTest {
    @Test
    public void testHostOnly() {
        assertHostname(new HostnameParser("example.com"), "https", 443, "example.com", "");
        assertHostname(new HostnameParser("www.example.com"), "https", 443, "www.example.com", "");
    }

    @Test
    public void testHostAndPort() {
        assertHostname(new HostnameParser("example.com:443"), "https", 443, "example.com", "");
        assertHostname(new HostnameParser("example.com:80"), "http", 80, "example.com", "");
        assertHostname(new HostnameParser("example.com:123"), "https", 123, "example.com", "");
    }

    @Test
    public void testScheme() {
        assertHostname(new HostnameParser("https://example.com"), "https", 443, "example.com", "");
        assertHostname(new HostnameParser("https://example.com:80"), "https", 80, "example.com", "");
        assertHostname(new HostnameParser("http://example.com"), "http", 80, "example.com", "");
        assertHostname(new HostnameParser("http://example.com:443"), "http", 443, "example.com", "");
    }

    @Test
    public void testSubfolder() {
        assertHostname(new HostnameParser("https://example.com/"), "https", 443, "example.com", "");
        assertHostname(new HostnameParser("https://example.com/a"), "https", 443, "example.com", "/a");
        assertHostname(new HostnameParser("https://example.com/a/"), "https", 443, "example.com", "/a");
        assertHostname(new HostnameParser("https://example.com:42/a"), "https", 42, "example.com", "/a");
    }

    private void assertHostname(HostnameParser parser, String scheme, int port, String host, String subfolder) {
        assertEquals(scheme, parser.scheme);
        assertEquals(port, parser.port);
        assertEquals(host, parser.host);
        assertEquals(subfolder, parser.subfolder);
    }
}
