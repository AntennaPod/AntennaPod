package de.danoeh.antennapod.service.download;

import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.BuildConfig;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.util.concurrent.TimeUnit;

/**
 * Provides access to a HttpClient singleton.
 */
public class AntennapodHttpClient {
    private static final String TAG = "AntennapodHttpClient";

    public static final long EXPIRED_CONN_TIMEOUT_SEC = 30;

    public static final int MAX_REDIRECTS = 5;
    public static final int CONNECTION_TIMEOUT = 30000;
    public static final int SOCKET_TIMEOUT = 30000;

    public static final int MAX_CONNECTIONS = 8;


    private static volatile HttpClient httpClient = null;

    /**
     * Returns the HttpClient singleton.
     */
    public static synchronized HttpClient getHttpClient() {
        if (httpClient == null) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Creating new instance of HTTP client");

            HttpParams params = new BasicHttpParams();
            params.setParameter(CoreProtocolPNames.USER_AGENT, AppConfig.USER_AGENT);
            params.setIntParameter("http.protocol.max-redirects", MAX_REDIRECTS);
            params.setBooleanParameter("http.protocol.reject-relative-redirect",
                    false);
            HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
            HttpClientParams.setRedirecting(params, true);

            httpClient = new DefaultHttpClient(createClientConnectionManager(), params);
            // Workaround for broken URLs in redirection
            ((AbstractHttpClient) httpClient)
                    .setRedirectHandler(new APRedirectHandler());
        }
        return httpClient;
    }

    /**
     * Closes expired connections. This method should be called by the using class once has finished its work with
     * the HTTP client.
     */
    public static synchronized void cleanup() {
        if (httpClient != null) {
            httpClient.getConnectionManager().closeExpiredConnections();
            httpClient.getConnectionManager().closeIdleConnections(EXPIRED_CONN_TIMEOUT_SEC, TimeUnit.SECONDS);
        }
    }


    private static ClientConnectionManager createClientConnectionManager() {
        HttpParams params = new BasicHttpParams();
        params.setIntParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, MAX_CONNECTIONS);
        return new ThreadSafeClientConnManager(params, prepareSchemeRegistry());
    }

    private static SchemeRegistry prepareSchemeRegistry() {
        SchemeRegistry sr = new SchemeRegistry();

        Scheme http = new Scheme("http",
                PlainSocketFactory.getSocketFactory(), 80);
        sr.register(http);
        Scheme https = new Scheme("https",
                SSLSocketFactory.getSocketFactory(), 443);
        sr.register(https);

        return sr;
    }

}
