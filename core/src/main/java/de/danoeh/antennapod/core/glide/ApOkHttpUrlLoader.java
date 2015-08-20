package de.danoeh.antennapod.core.glide;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.integration.okhttp.OkHttpStreamFetcher;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.service.download.HttpDownloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.NetworkUtils;

/**
 * @see com.bumptech.glide.integration.okhttp.OkHttpUrlLoader
 */
public class ApOkHttpUrlLoader implements ModelLoader<GlideUrl, InputStream> {

    private static final String TAG = ApOkHttpUrlLoader.class.getSimpleName();

    /**
     * The default factory for {@link ApOkHttpUrlLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {

        private static volatile OkHttpClient internalClient;
        private OkHttpClient client;

        private static OkHttpClient getInternalClient() {
            if (internalClient == null) {
                synchronized (Factory.class) {
                    if (internalClient == null) {
                        internalClient = AntennapodHttpClient.newHttpClient();
                        internalClient.interceptors().add(new NetworkAllowanceInterceptor());
                        internalClient.interceptors().add(new BasicAuthenticationInterceptor());
                    }
                }
            }
            return internalClient;
        }

        /**
         * Constructor for a new Factory that runs requests using a static singleton client.
         */
        public Factory() {
            this(getInternalClient());
        }

        /**
         * Constructor for a new Factory that runs requests using given client.
         */
        public Factory(OkHttpClient client) {
            this.client = client;
        }

        @Override
        public ModelLoader<GlideUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new ApOkHttpUrlLoader(client);
        }

        @Override
        public void teardown() {
            // Do nothing, this instance doesn't own the client.
        }
    }

    private final OkHttpClient client;

    public ApOkHttpUrlLoader(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(GlideUrl model, int width, int height) {
        return new OkHttpStreamFetcher(client, model);
    }

    private static class NetworkAllowanceInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            if (NetworkUtils.isDownloadAllowed()) {
                return chain.proceed(chain.request());
            } else {
                return null;
            }
        }

    }

    private static class BasicAuthenticationInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            com.squareup.okhttp.Request request = chain.request();
            String url = request.urlString();
            Context context = ClientConfig.applicationCallbacks.getApplicationInstance();
            String authentication = DBReader.getImageAuthentication(context, url);

            if(TextUtils.isEmpty(authentication)) {
                Log.d(TAG, "no credentials for '" + url + "'");
                return chain.proceed(request);
            }

            // add authentication
            String[] auth = authentication.split(":");
            String credentials = HttpDownloader.encodeCredentials(auth[0], auth[1], "ISO-8859-1");
            com.squareup.okhttp.Request newRequest = request
                    .newBuilder()
                    .addHeader("Authorization", credentials)
                    .build();
            Log.d(TAG, "Basic authentication with ISO-8859-1 encoding");
            Response response = chain.proceed(newRequest);
            if (!response.isSuccessful() && response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                credentials = HttpDownloader.encodeCredentials(auth[0], auth[1], "UTF-8");
                newRequest = request
                        .newBuilder()
                        .addHeader("Authorization", credentials)
                        .build();
                Log.d(TAG, "Basic authentication with UTF-8 encoding");
                return chain.proceed(newRequest);
            } else {
                return response;
            }
        }
    }

}