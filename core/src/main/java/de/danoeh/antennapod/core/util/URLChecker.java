package de.danoeh.antennapod.core.util;

import android.net.Uri;
import android.util.Log;

import de.danoeh.antennapod.core.BuildConfig;

/**
 * Provides methods for checking and editing a URL.
 */
public final class URLChecker {

    /**
     * Class shall not be instantiated.
     */
    private URLChecker() {
    }

    /**
     * Logging tag.
     */
    private static final String TAG = "URLChecker";

    private static final String AP_SUBSCRIBE = "antennapod-subscribe://";

    /**
     * Checks if URL is valid and modifies it if necessary.
     *
     * @param url The url which is going to be prepared
     * @return The prepared url
     */
    public static String prepareURL(String url) {
        url = url.trim();
        if (url.startsWith("feed://")) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Replacing feed:// with http://");
            return url.replaceFirst("feed://", "http://");
        } else if (url.startsWith("pcast://")) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Removing pcast://");
            return prepareURL(url.substring("pcast://".length()));
        } else if (url.startsWith("pcast:")) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Removing pcast:");
            return prepareURL(url.substring("pcast:".length()));
        } else if (url.startsWith("itpc")) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Replacing itpc:// with http://");
            return url.replaceFirst("itpc://", "http://");
        } else if (url.startsWith(AP_SUBSCRIBE)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Removing antennapod-subscribe://");
            return prepareURL(url.substring(AP_SUBSCRIBE.length()));
        } else if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Adding http:// at the beginning of the URL");
            return "http://" + url;
        } else {
            return url;
        }
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
    public static String prepareURL(String url, String base) {
        if (base == null) {
            return prepareURL(url);
        }
        url = url.trim();
        base = prepareURL(base);
        Uri urlUri = Uri.parse(url);
        Uri baseUri = Uri.parse(base);
        if (urlUri.isRelative() && baseUri.isAbsolute()) {
            return urlUri.buildUpon().scheme(baseUri.getScheme()).build().toString();
        } else {
            return prepareURL(url);
        }
    }
}
