package de.danoeh.antennapod.core.service.download;

import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBWriter;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.StatusLine;

/**
 * Provides access to a HttpClient singleton.
 */
public class AntennapodHttpClient {
    private static final String TAG = "AntennapodHttpClient";

    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 30000;

    private static final int MAX_CONNECTIONS = 8;


    private static volatile OkHttpClient httpClient = null;

    /**
     * Returns the HttpClient singleton.
     */
    public static synchronized OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = newBuilder().build();
        }
        return httpClient;
    }

    public static synchronized void reinit() {
        httpClient = newBuilder().build();
    }

    /**
     * Creates a new HTTP client.  Most users should just use
     * getHttpClient() to get the standard AntennaPod client,
     * but sometimes it's necessary for others to have their own
     * copy so that the clients don't share state.
     * @return http client
     */
    @NonNull
    public static OkHttpClient.Builder newBuilder() {
        Log.d(TAG, "Creating new instance of HTTP client");

        System.setProperty("http.maxConnections", String.valueOf(MAX_CONNECTIONS));

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // detect 301 Moved permanently and 308 Permanent Redirect
        builder.networkInterceptors().add(chain -> {
            Request request = chain.request();
            Response response = chain.proceed(request);
            if (response.code() == HttpURLConnection.HTTP_MOVED_PERM ||
                    response.code() == StatusLine.HTTP_PERM_REDIRECT) {
                String location = response.header("Location");
                if (location.startsWith("/")) { // URL is not absolute, but relative
                    HttpUrl url = request.url();
                    location = url.scheme() + "://" + url.host() + location;
                } else if (!location.toLowerCase().startsWith("http://") &&
                        !location.toLowerCase().startsWith("https://")) {
                    // Reference is relative to current path
                    HttpUrl url = request.url();
                    String path = url.encodedPath();
                    String newPath = path.substring(0, path.lastIndexOf("/") + 1) + location;
                    location = url.scheme() + "://" + url.host() + newPath;
                }
                try {
                    DBWriter.updateFeedDownloadURL(request.url().toString(), location).get();
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            return response;
        });

        // set cookie handler
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        builder.cookieJar(new JavaNetCookieJar(cm));

        // set timeouts
        builder.connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);

        // configure redirects
        builder.followRedirects(true);
        builder.followSslRedirects(true);

        ProxyConfig config = UserPreferences.getProxyConfig();
        if (config.type != Proxy.Type.DIRECT) {
            int port = config.port > 0 ? config.port : ProxyConfig.DEFAULT_PORT;
            SocketAddress address = InetSocketAddress.createUnresolved(config.host, port);
            Proxy proxy = new Proxy(config.type, address);
            builder.proxy(proxy);
            if (!TextUtils.isEmpty(config.username)) {
                String credentials = Credentials.basic(config.username, config.password);
                builder.interceptors().add(chain -> {
                    Request request = chain.request().newBuilder()
                            .header("Proxy-Authorization", credentials).build();
                    return chain.proceed(request);
                });
            }
        }
        if(16 <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < 21) {
            builder.sslSocketFactory(new CustomSslSocketFactory(), trustManager());
        }
        return builder;
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

    private static X509TrustManager trustManager() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:"
                        + Arrays.toString(trustManagers));
            }
            return (X509TrustManager) trustManagers[0];
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
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
