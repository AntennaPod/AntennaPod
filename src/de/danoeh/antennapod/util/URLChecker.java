package de.danoeh.antennapod.util;

import android.util.Log;
import de.danoeh.antennapod.BuildConfig;

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
        if (url.startsWith("feed://")) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Replacing feed:// with http://");
            url = url.replace("feed://", "http://");
        } else if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Adding http:// at the beginning of the URL");
            builder.append("http://");
        }
        builder.append(url);

        return builder.toString();
    }
}
