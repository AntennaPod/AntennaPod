package de.danoeh.antennapod.core.service.download;

import android.support.annotation.NonNull;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.BuildConfig;

/**
 * Provides access to a HttpClient singleton.
 */
public class AntennapodHttpClient {
    private static final String TAG = "AntennapodHttpClient";

    public static final int CONNECTION_TIMEOUT = 30000;
    public static final int READ_TIMEOUT = 30000;

    public static final int MAX_CONNECTIONS = 8;


    private static volatile OkHttpClient httpClient = null;

    /**
     * Returns the HttpClient singleton.
     */
    public static synchronized OkHttpClient getHttpClient() {
        if (httpClient == null) {

            httpClient = newHttpClient();
        }
        return httpClient;
    }

    /**
     * Creates a new HTTP client.  Most users should just use
     * getHttpClient() to get the standard AntennaPod client,
     * but sometimes it's necessary for others to have their own
     * copy so that the clients don't share state.
     * @return http client
     */
    @NonNull
    public static OkHttpClient newHttpClient() {
        Log.d(TAG, "Creating new instance of HTTP client");

        System.setProperty("http.maxConnections", String.valueOf(MAX_CONNECTIONS));

        OkHttpClient client = new OkHttpClient();

        // set cookie handler
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        client.setCookieHandler(cm);

        // set timeouts
        client.setConnectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        client.setReadTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);

        // configure redirects
        client.setFollowRedirects(true);
        client.setFollowSslRedirects(true);
        return client;
    }

    /**
     * Closes expired connections. This method should be called by the using class once has finished its work with
     * the HTTP client.
     */
    public static synchronized void cleanup() {
        if (httpClient != null) {
            // does nothing at the moment
        }
    }
}
