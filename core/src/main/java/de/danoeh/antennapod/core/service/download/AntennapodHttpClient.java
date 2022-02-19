package de.danoeh.antennapod.core.service.download;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.BasicAuthorizationInterceptor;
import de.danoeh.antennapod.core.service.UserAgentInterceptor;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.net.ssl.SslClientSetup;
import okhttp3.Cache;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.StatusLine;
import java.io.File;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Provides access to a HttpClient singleton.
 */
public class AntennapodHttpClient {
    private static final String TAG = "AntennapodHttpClient";
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;
    private static final int MAX_CONNECTIONS = 8;
    private static File cacheDirectory;

    private static volatile OkHttpClient httpClient = null;

    private AntennapodHttpClient() {

    }

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
            if (response.code() == HttpURLConnection.HTTP_MOVED_PERM
                    || response.code() == StatusLine.HTTP_PERM_REDIRECT) {
                String location = response.header("Location");
                if (location == null) {
                    return response;
                }
                if (location.startsWith("/")) { // URL is not absolute, but relative
                    HttpUrl url = request.url();
                    location = url.scheme() + "://" + url.host() + location;
                } else if (!location.toLowerCase().startsWith("http://")
                        && !location.toLowerCase().startsWith("https://")) {
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
        builder.interceptors().add(new BasicAuthorizationInterceptor());
        builder.networkInterceptors().add(new UserAgentInterceptor());

        // set cookie handler
        CookieManager cm = new CookieManager();
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        builder.cookieJar(new JavaNetCookieJar(cm));

        // set timeouts
        builder.connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        builder.cache(new Cache(cacheDirectory, 20L * 1000000)); // 20MB

        // configure redirects
        builder.followRedirects(true);
        builder.followSslRedirects(true);

        ProxyConfig config = UserPreferences.getProxyConfig();
        if (config.type != Proxy.Type.DIRECT && !TextUtils.isEmpty(config.host)) {
            int port = config.port > 0 ? config.port : ProxyConfig.DEFAULT_PORT;
            SocketAddress address = InetSocketAddress.createUnresolved(config.host, port);
            builder.proxy(new Proxy(config.type, address));
            if (!TextUtils.isEmpty(config.username) && config.password != null) {
                builder.proxyAuthenticator((route, response) -> {
                    String credentials = Credentials.basic(config.username, config.password);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credentials)
                            .build();
                });
            }
        }

        SslClientSetup.installCertificates(builder);
        return builder;
    }

    public static void setCacheDirectory(File cacheDirectory) {
        AntennapodHttpClient.cacheDirectory = cacheDirectory;
    }
}
