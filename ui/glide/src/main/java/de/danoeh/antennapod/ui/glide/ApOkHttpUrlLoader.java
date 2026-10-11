package de.danoeh.antennapod.ui.glide;

import android.content.ContentResolver;
import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.net.common.HttpCredentialEncoder;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.net.common.UserAgentInterceptor;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@see com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader}.
 */
class ApOkHttpUrlLoader<T> implements ModelLoader<T, InputStream> {

    /**
     * The default factory for {@link ApOkHttpUrlLoader}s.
     */
    public static class Factory<T> implements ModelLoaderFactory<T, InputStream> {

        private static volatile OkHttpClient internalClient;
        private final OkHttpClient client;

        static OkHttpClient getInternalClient() {
            if (internalClient == null) {
                synchronized (Factory.class) {
                    if (internalClient == null) {
                        OkHttpClient.Builder builder = AntennapodHttpClient.newBuilder();
                        builder.interceptors().add(new NetworkAllowanceInterceptor());
                        builder.interceptors().add(new UserAgentInterceptor());
                        builder.cache(null); // Handled by Glide
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
            this.client = getInternalClient();
        }

        @NonNull
        @Override
        public ModelLoader<T, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new ApOkHttpUrlLoader<>(client);
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
    public LoadData<InputStream> buildLoadData(@NonNull T model, int width, int height, @NonNull Options options) {
        String url = getUrl(model);
        GlideUrl glideUrl = new GlideUrl(url);
        if (model instanceof AuthenticatedImageUrl) {
            AuthenticatedImageUrl imageUrl = (AuthenticatedImageUrl) model;
            if (imageUrl.hasCredentials() && "https".equalsIgnoreCase(Uri.parse(url).getScheme())) {
                LazyHeaders headers = new LazyHeaders.Builder()
                        .addHeader("Authorization",
                                HttpCredentialEncoder.encode(imageUrl.getUsername(),
                                        imageUrl.getPassword(), "ISO-8859-1"))
                        .build();
                glideUrl = new GlideUrl(url, headers);
            }
        }
        return new LoadData<>(new ObjectKey(url), new ResizingOkHttpStreamFetcher(client, glideUrl));
    }

    @Override
    public boolean handles(@NonNull T model) {
        String url = getUrl(model);
        return !TextUtils.isEmpty(url)
                // If the other loaders fail, do not attempt to load as web resource
                && !url.startsWith(Feed.PREFIX_GENERATIVE_COVER)
                && !url.startsWith(FeedMedia.FILENAME_PREFIX_EMBEDDED_COVER)
                // Leave content URIs to Glide's default loaders
                && !url.startsWith(ContentResolver.SCHEME_CONTENT)
                && !url.startsWith(ContentResolver.SCHEME_ANDROID_RESOURCE);
    }

    private String getUrl(T model) {
        return model instanceof AuthenticatedImageUrl ? ((AuthenticatedImageUrl) model).getUrl() : (String) model;
    }

    static class NetworkAllowanceInterceptor implements Interceptor {

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            if (NetworkUtils.isImageAllowed()) {
                return chain.proceed(chain.request());
            } else {
                return new Response.Builder()
                        .protocol(Protocol.HTTP_2)
                        .code(420)
                        .message("Policy Not Fulfilled")
                        .body(ResponseBody.create(new byte[0], null))
                        .request(chain.request())
                        .build();
            }
        }
    }
}
