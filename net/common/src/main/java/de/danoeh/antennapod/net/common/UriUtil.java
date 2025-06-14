package de.danoeh.antennapod.net.common;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Builds a query string from a map of parameters.
     * @param params The parameters to encode.
     * @return The query string.
     */
    public static String queryString(Map<String, String> params) {
        if (params.isEmpty()) {
            return "";
        }
        StringBuilder queryStringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (queryStringBuilder.length() > 0) {
                queryStringBuilder.append("&");
            }
            queryStringBuilder.append(urlEncode(entry.getKey()))
                       .append("=")
                       .append(urlEncode(entry.getValue()));
        }
        return queryStringBuilder.toString();
    }

    /**
     * Encodes a URL using UTF-8. Implementation is chosen based on the current platform version.
     */
    interface PlatformUrlEncoder {
        String encode(String input);
    }


    /**
     * Signature with defined charset is only available in API 33 and above.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    static class StandardUrlEncoder implements PlatformUrlEncoder {
        @Override
        public String encode(String input) {
            return URLEncoder.encode(input, StandardCharsets.UTF_8);
        }
    }

    /**
     * Legacy implementation for API 32 and below - uses the default charset, which is documented as unreliable since
     * it can differ with each platform.
     */
    static class LegacyUrlEncoder implements PlatformUrlEncoder {
        @Override
        public String encode(String input) {
            //noinspection deprecation
            return URLEncoder.encode(input);
        }
    }

    static final PlatformUrlEncoder urlEncoder;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            urlEncoder = new StandardUrlEncoder();
        } else {
            urlEncoder = new LegacyUrlEncoder();
        }
    }

    /**
     * Encodes a URL with as much fidelity as the platform allows.
     * @param input The string to encode.
     * @return The encoded string.
     */
    public static String urlEncode(String input) {
        if (input == null) {
            return "";
        }
        return urlEncoder.encode(input);
    }
}
