package de.danoeh.antennapod.net.common;

import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides methods for checking and editing a URL.
 */
public final class UrlChecker {

    /**
     * Class shall not be instantiated.
     */
    private UrlChecker() {
    }

    /**
     * Logging tag.
     */
    private static final String TAG = "UrlChecker";

    private static final String AP_SUBSCRIBE = "antennapod-subscribe://";
    private static final String AP_SUBSCRIBE_DEEPLINK = "antennapod.org/deeplink/subscribe";

    /**
     * Checks if URL is valid and modifies it if necessary.
     *
     * @param url The url which is going to be prepared
     * @return The prepared url
     */
    public static String prepareUrl(@NonNull String url) {
        url = url.trim();
        String lowerCaseUrl = url.toLowerCase(Locale.ROOT); // protocol names are case insensitive
        if (lowerCaseUrl.startsWith("feed://")) {
            Log.d(TAG, "Replacing feed:// with http://");
            return prepareUrl(url.substring("feed://".length()));
        } else if (lowerCaseUrl.startsWith("pcast://")) {
            Log.d(TAG, "Removing pcast://");
            return prepareUrl(url.substring("pcast://".length()));
        } else if (lowerCaseUrl.startsWith("pcast:")) {
            Log.d(TAG, "Removing pcast:");
            return prepareUrl(url.substring("pcast:".length()));
        } else if (lowerCaseUrl.startsWith("itpc")) {
            Log.d(TAG, "Replacing itpc:// with http://");
            return prepareUrl(url.substring("itpc://".length()));
        } else if (lowerCaseUrl.startsWith(AP_SUBSCRIBE)) {
            Log.d(TAG, "Removing antennapod-subscribe://");
            return prepareUrl(url.substring(AP_SUBSCRIBE.length()));
        } else if (lowerCaseUrl.contains(AP_SUBSCRIBE_DEEPLINK)) {
            Log.d(TAG, "Removing " + AP_SUBSCRIBE_DEEPLINK);
            String query = Uri.parse(url).getQueryParameter("url");
            try {
                return prepareUrl(URLDecoder.decode(query, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return prepareUrl(query);
            }
        } else if (lowerCaseUrl.contains("subscribeonandroid.com")) {
            return prepareUrl(url.replaceFirst("((www.)?(subscribeonandroid.com/))", ""));
        } else if (!(lowerCaseUrl.startsWith("http://") || lowerCaseUrl.startsWith("https://"))) {
            Log.d(TAG, "Adding http:// at the beginning of the URL");
            return "http://" + url;
        } else {
            return url;
        }
    }

    public static boolean isDeeplinkWithoutUrl(String url) {
        return url.toLowerCase(Locale.ROOT).contains(AP_SUBSCRIBE_DEEPLINK)
                && Uri.parse(url).getQueryParameter("url") == null;
    }

    /**
     * Checks if URL is valid and modifies it if necessary.
     * This method also handles protocol relative URLs.
     *
     * @param url  The url which is going to be prepared
     * @param base The url against which the (possibly relative) url is applied. If this is null,
     *             the result of prepareURL(url) is returned instead.
     * @return The prepared url
     */
    public static String prepareUrl(String url, String base) {
        if (base == null) {
            return prepareUrl(url);
        }
        url = url.trim();
        base = prepareUrl(base);
        Uri urlUri = Uri.parse(url);
        Uri baseUri = Uri.parse(base);
        if (urlUri.isRelative() && baseUri.isAbsolute()) {
            return urlUri.buildUpon().scheme(baseUri.getScheme()).build().toString();
        } else {
            return prepareUrl(url);
        }
    }

    public static boolean containsUrl(List<String> list, String url) {
        for (String item : list) {
            if (urlEquals(item, url)) {
                return true;
            }
        }
        return false;
    }

    public static boolean urlEquals(String string1, String string2) {
        Uri url1 = Uri.parse(string1);
        Uri url2 = Uri.parse(string2);
        if (url1 == null || url2 == null || url1.getHost() == null || url2.getHost() == null) {
            return string1.equals(string2); // Unable to parse url properly
        }
        if (!url1.getHost().toLowerCase(Locale.ROOT).equals(url2.getHost().toLowerCase(Locale.ROOT))) {
            return false;
        }
        List<String> pathSegments1 = normalizePathSegments(url1.getPathSegments());
        List<String> pathSegments2 = normalizePathSegments(url2.getPathSegments());
        if (!pathSegments1.equals(pathSegments2)) {
            return false;
        }
        if (TextUtils.isEmpty(url1.getQuery())) {
            return TextUtils.isEmpty(url2.getQuery());
        }
        return url1.getQuery().equals(url2.getQuery());
    }

    /**
     * Removes empty segments and converts all to lower case.
     * @param input List of path segments
     * @return Normalized list of path segments
     */
    private static List<String> normalizePathSegments(List<String> input) {
        List<String> result = new ArrayList<>();
        for (String string : input) {
            if (!TextUtils.isEmpty(string)) {
                result.add(string.toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }
}
