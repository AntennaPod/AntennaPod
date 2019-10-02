package de.danoeh.antennapod.core.glide;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.service.download.HttpDownloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.NetworkUtils;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @see com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
 */
class ApOkHttpUrlLoader implements ModelLoader<String, InputStream> {

    private static final String TAG = ApOkHttpUrlLoader.class.getSimpleName();

    /**
     * The default factory for {@link ApOkHttpUrlLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<String, InputStream> {

        private static volatile OkHttpClient internalClient;
        private final OkHttpClient client;

        private static OkHttpClient getInternalClient() {
            if (internalClient == null) {
                synchronized (Factory.class) {
                    if (internalClient == null) {
                        OkHttpClient.Builder builder = AntennapodHttpClient.newBuilder();
                        builder.interceptors().add(new NetworkAllowanceInterceptor());
                        builder.interceptors().add(new BasicAuthenticationInterceptor());
                        internalClient = builder.build();
                    }
                }
            }
            return internalClient;
        }

        /**
         * Constructor for a new Factory that runs requests using a static singleton client.
         */
        Factory() {
            this(getInternalClient());
        }

        /**
         * Constructor for a new Factory that runs requests using given client.
         */
        Factory(OkHttpClient client) {
            this.client = client;
        }

        @NonNull
        @Override
        public ModelLoader<String, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new ApOkHttpUrlLoader(client);
        }

        @Override
        public void teardown() {
            // Do nothing, this instance doesn't own the client.
        }
    }

    private final OkHttpClient client;

    private ApOkHttpUrlLoader(OkHttpClient client) {
        this.client = client;
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull String model, int width, int height, @NonNull Options options) {
        if (TextUtils.isEmpty(model)) {
            return null;
        } else if (model.startsWith("/")) {
            return new LoadData<>(new ObjectKey(model), new AudioCoverFetcher(model));
        } else {
            GlideUrl url = new GlideUrl(model);
            return new LoadData<>(new ObjectKey(model), new OkHttpStreamFetcher(client, url));
        }
    }

    @Override
    public boolean handles(@NonNull String s) {
        return true;
    }

    private static class NetworkAllowanceInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            if (NetworkUtils.isImageAllowed()) {
                return chain.proceed(chain.request());
            } else {
                return new Response.Builder()
                        .protocol(Protocol.HTTP_2)
                        .code(420)
                        .message("Policy Not Fulfilled")
                        .body(ResponseBody.create(null, new byte[0]))
                        .request(chain.request())
                        .build();
            }
        }

    }

    private static class BasicAuthenticationInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            String url = request.url().toString();
            String authentication = DBReader.getImageAuthentication(url);

            if(TextUtils.isEmpty(authentication)) {
                Log.d(TAG, "no credentials for '" + url + "'");
                return chain.proceed(request);
            }

            // add authentication
            String[] auth = authentication.split(":");
            if (auth.length != 2) {
                Log.d(TAG, "Invalid credentials for '" + url + "'");
                return chain.proceed(request);
            }

            String credentials = HttpDownloader.encodeCredentials(auth[0], auth[1], "ISO-8859-1");
            Request newRequest = request
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