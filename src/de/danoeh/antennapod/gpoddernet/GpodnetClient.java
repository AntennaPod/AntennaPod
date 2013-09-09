package de.danoeh.antennapod.gpoddernet;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;

/**
 * HTTP client for the gpodder.net service.
 */
public class GpodnetClient extends DefaultHttpClient {

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

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
        return new ThreadSafeClientConnManager(new BasicHttpParams(), prepareSchemeRegistry());
    }

}
