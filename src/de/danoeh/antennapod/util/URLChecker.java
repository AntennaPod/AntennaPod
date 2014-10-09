package de.danoeh.antennapod.util;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.BuildConfig;

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
        url = StringUtils.trim(url);
        if (url.startsWith("feed://")) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Replacing feed:// with http://");
            return url.replaceFirst("feed://", "http://");
        } else if (url.startsWith("pcast://")) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Removing pcast://");
            return prepareURL(StringUtils.removeStart(url, "pcast://"));
        } else if (url.startsWith("itpc")) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Replacing itpc:// with http://");
            return url.replaceFirst("itpc://", "http://");
        } else if (url.startsWith(AP_SUBSCRIBE)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Removing antennapod-subscribe://");
            return prepareURL(StringUtils.removeStart(url, AP_SUBSCRIBE));
        } else if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Adding http:// at the beginning of the URL");
            return "http://" + url;
        } else {
            return url;
        }
    }
}
