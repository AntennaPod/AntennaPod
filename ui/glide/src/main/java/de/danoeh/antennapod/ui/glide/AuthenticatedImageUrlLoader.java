package de.danoeh.antennapod.ui.glide;

import android.content.ContentResolver;
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
import de.danoeh.antennapod.net.common.HttpCredentialEncoder;

import java.io.InputStream;

class AuthenticatedImageUrlLoader implements ModelLoader<AuthenticatedImageUrl, InputStream> {

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull AuthenticatedImageUrl model,
                                               int width, int height, @NonNull Options options) {
        GlideUrl glideUrl;
        if (model.hasCredentials()) {
            LazyHeaders headers = new LazyHeaders.Builder()
                    .addHeader("Authorization", HttpCredentialEncoder.encode(model.getUsername(), model.getPassword(), "ISO-8859-1"))
                    .build();
            glideUrl = new GlideUrl(model.getUrl(), headers);
        } else {
            glideUrl = new GlideUrl(model.getUrl());
        }
        return new LoadData<>(new ObjectKey(model.getUrl()),
                new ResizingOkHttpStreamFetcher(ApOkHttpUrlLoader.Factory.getInternalClient(), glideUrl));
    }

    @Override
    public boolean handles(@NonNull AuthenticatedImageUrl model) {
        String url = model.getUrl();
        return !TextUtils.isEmpty(url)
                && !url.startsWith(Feed.PREFIX_GENERATIVE_COVER)
                && !url.startsWith(FeedMedia.FILENAME_PREFIX_EMBEDDED_COVER)
                && !url.startsWith(ContentResolver.SCHEME_CONTENT)
                && !url.startsWith(ContentResolver.SCHEME_ANDROID_RESOURCE);
    }

    public static class Factory implements ModelLoaderFactory<AuthenticatedImageUrl, InputStream> {

        @NonNull
        @Override
        public ModelLoader<AuthenticatedImageUrl, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new AuthenticatedImageUrlLoader();
        }

        @Override
        public void teardown() {
        }
    }
}
