package de.danoeh.antennapod.net.common;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility methods for dealing with URL encoding.
 */
public abstract class UriUtil {
    public static URI getURIFromRequestUrl(String source) {
        // try without encoding the URI
        try {
            return new URI(source);
        } catch (URISyntaxException ignore) {
            System.out.println("Source is not encoded, encoding now");
        }
        try {
            URL url = new URL(source);
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
