package de.danoeh.antennapod.util;

import de.danoeh.antennapod.AppConfig;
import android.util.Log;

/** Provides methods for checking and editing a URL.*/
public final class URLChecker {

    /**Class shall not be instantiated.*/
    private URLChecker() {
    }

    /**Logging tag.*/
    private static final String TAG = "URLChecker";
    /**Indicator for URLs made by Feedburner.*/
    private static final String FEEDBURNER_URL = "feeds.feedburner.com";
    /**Prefix that is appended to URLs by Feedburner.*/
    private static final String FEEDBURNER_PREFIX = "?format=xml";

    /** Checks if URL is valid and modifies it if necessary.
     *  @param url The url which is going to be prepared
     *  @return The prepared url
     * */
    public static String prepareURL(final String url) {
        StringBuilder builder = new StringBuilder();

        if (!url.startsWith("http")) {
            builder.append("http://");
            if (AppConfig.DEBUG) Log.d(TAG, "Missing http; appending");
        }
        builder.append(url);

        if (url.contains(FEEDBURNER_URL)) {
            if (AppConfig.DEBUG) Log.d(TAG,
            "URL seems to be Feedburner URL; appending prefix");
            builder.append(FEEDBURNER_PREFIX);
        }
        return builder.toString();
    }
}
