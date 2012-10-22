package de.danoeh.antennapod.util;

import android.util.Log;
import de.danoeh.antennapod.AppConfig;

/** Provides methods for checking and editing a URL.*/
public final class URLChecker {

    /**Class shall not be instantiated.*/
    private URLChecker() {
    }

    /**Logging tag.*/
    private static final String TAG = "URLChecker";

    /** Checks if URL is valid and modifies it if necessary.
     *  @param url The url which is going to be prepared
     *  @return The prepared url
     * */
    public static String prepareURL(String url) {
        StringBuilder builder = new StringBuilder();
        url = url.trim();
        if (!url.startsWith("http")) {
            builder.append("http://");
            if (AppConfig.DEBUG) Log.d(TAG, "Missing http; appending");
        } else if (url.startsWith("https")) {
        	if (AppConfig.DEBUG) Log.d(TAG, "Replacing https with http");
        	url = url.replaceFirst("https", "http");
        }
        builder.append(url);

        return builder.toString();
    }
}
