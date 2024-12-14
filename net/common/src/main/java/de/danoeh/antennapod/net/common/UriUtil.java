package de.danoeh.antennapod.net.common;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    interface PlatformUrlEncoder {
        String encode(String input);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    static class StandardUrlEncoder implements PlatformUrlEncoder {
        @Override
        public String encode(String input) {
            return URLEncoder.encode(input, StandardCharsets.UTF_8);
        }
    }

    static class LegacyUrlEncoder implements PlatformUrlEncoder {
        @Override
        public String encode(String input) {
            return URLEncoder.encode(input);
        }
    }

    private static final PlatformUrlEncoder urlEncoder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? new StandardUrlEncoder()
            : new LegacyUrlEncoder();

    public static String urlEncode(String input) {
        return urlEncoder.encode(input);
    }
}
