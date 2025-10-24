package de.danoeh.antennapod.ui.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;

import okhttp3.Credentials;

/**
 * A {@link ModelLoader} for handling {@link AuthenticatedImageUrl} models.
 * This loader converts the {@link AuthenticatedImageUrl} into a {@link GlideUrl}
 * with an "Authorization" header, then delegates the actual loading to Glide's
 * default networking stack.
 */
public class AuthenticatedImageLoader implements ModelLoader<AuthenticatedImageUrl, InputStream> {

    private final ModelLoader<GlideUrl, InputStream> httpUrlLoader;

    public AuthenticatedImageLoader(ModelLoader<GlideUrl, InputStream> httpUrlLoader) {
        this.httpUrlLoader = httpUrlLoader;
    }

    @Override
    public LoadData<InputStream> buildLoadData(@NonNull AuthenticatedImageUrl model, int width, int height,
                                               @NonNull Options options) {
        Headers headers = new LazyHeaders.Builder()
                .addHeader("Authorization",
                        Credentials.basic(model.getUsername(), model.getPassword()))
                .build();
        GlideUrl glideUrl = new GlideUrl(model.getImageUrl(), headers);
        return httpUrlLoader.buildLoadData(glideUrl, width, height, options);
    }

    @Override
    public boolean handles(@NonNull AuthenticatedImageUrl model) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<AuthenticatedImageUrl, InputStream> {

        @NonNull
        @Override
        public ModelLoader<AuthenticatedImageUrl, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new AuthenticatedImageLoader(multiFactory.build(GlideUrl.class, InputStream.class));
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
