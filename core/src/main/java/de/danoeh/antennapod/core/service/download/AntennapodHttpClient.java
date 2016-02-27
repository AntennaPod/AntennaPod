package de.danoeh.antennapod.core.service.download;

import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.http.StatusLine;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBWriter;

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

    public static synchronized void reinit() {
        httpClient = newHttpClient();
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

        // detect 301 Moved permanently and 308 Permanent Redirect
        client.networkInterceptors().add(chain -> {
            Request request = chain.request();
            Response response = chain.proceed(request);
            if (response.code() == HttpURLConnection.HTTP_MOVED_PERM ||
                    response.code() == StatusLine.HTTP_PERM_REDIRECT) {
                String location = response.header("Location");
                if (location.startsWith("/")) { // URL is not absolute, but relative
                    URL url = request.url();
                    location = url.getProtocol() + "://" + url.getHost() + location;
                } else if (!location.toLowerCase().startsWith("http://") &&
                        !location.toLowerCase().startsWith("https://")) {
                    // Reference is relative to current path
                    URL url = request.url();
                    String path = url.getPath();
                    String newPath = path.substring(0, path.lastIndexOf("/") + 1) + location;
                    location = url.getProtocol() + "://" + url.getHost() + newPath;
                }
                try {
                    DBWriter.updateFeedDownloadURL(request.urlString(), location).get();
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            return response;
        });

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

        ProxyConfig config = UserPreferences.getProxyConfig();
        if (config.type != Proxy.Type.DIRECT) {
            int port = config.port > 0 ? config.port : ProxyConfig.DEFAULT_PORT;
            SocketAddress address = InetSocketAddress.createUnresolved(config.host, port);
            Proxy proxy = new Proxy(config.type, address);
            client.setProxy(proxy);
            if (!TextUtils.isEmpty(config.username)) {
                String credentials = Credentials.basic(config.username, config.password);
                client.interceptors().add(chain -> {
                    Request request = chain.request().newBuilder()
                            .header("Proxy-Authorization", credentials).build();
                    return chain.proceed(request);
                });
            }
        }
        if(16 <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < 21) {
            client.setSslSocketFactory(new CustomSslSocketFactory());
        }
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

    private static class CustomSslSocketFactory extends SSLSocketFactory {

        private SSLSocketFactory factory;

        public CustomSslSocketFactory() {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, null);
                factory= sslContext.getSocketFactory();
            } catch(GeneralSecurityException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return factory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return factory.getSupportedCipherSuites();
        }

        public Socket createSocket() throws IOException {
            SSLSocket result = (SSLSocket) factory.createSocket();
            configureSocket(result);
            return result;
        }

        public Socket createSocket(String var1, int var2) throws IOException {
            SSLSocket result = (SSLSocket) factory.createSocket(var1, var2);
            configureSocket(result);
            return result;
        }

        public Socket createSocket(Socket var1, String var2, int var3, boolean var4) throws IOException {
            SSLSocket result = (SSLSocket) factory.createSocket(var1, var2, var3, var4);
            configureSocket(result);
            return result;
        }

        public Socket createSocket(InetAddress var1, int var2) throws IOException {
            SSLSocket result = (SSLSocket) factory.createSocket(var1, var2);
            configureSocket(result);
            return result;
        }

        public Socket createSocket(String var1, int var2, InetAddress var3, int var4) throws IOException {
            SSLSocket result = (SSLSocket) factory.createSocket(var1, var2, var3, var4);
            configureSocket(result);
            return result;
        }

        public Socket createSocket(InetAddress var1, int var2, InetAddress var3, int var4) throws IOException {
            SSLSocket result = (SSLSocket) factory.createSocket(var1, var2, var3, var4);
            configureSocket(result);
            return result;
        }

        private void configureSocket(SSLSocket s) {
            s.setEnabledProtocols(new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" } );
        }

    }

}
