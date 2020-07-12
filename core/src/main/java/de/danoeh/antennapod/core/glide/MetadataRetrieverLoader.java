package de.danoeh.antennapod.core.glide;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import de.danoeh.antennapod.core.feed.FeedMedia;

import java.io.InputStream;

class MetadataRetrieverLoader implements ModelLoader<String, InputStream> {

    /**
     * The default factory for {@link MetadataRetrieverLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<String, InputStream> {
        private final Context context;

        Factory(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public ModelLoader<String, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new MetadataRetrieverLoader(context);
        }

        @Override
        public void teardown() {
            // Do nothing, this instance doesn't own the client.
        }
    }

    private final Context context;

    private MetadataRetrieverLoader(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull String model,
                                               int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model),
                new AudioCoverFetcher(model.replace(FeedMedia.FILENAME_PREFIX_EMBEDDED_COVER, ""), context));
    }

    @Override
    public boolean handles(@NonNull String model) {
        return model.startsWith(FeedMedia.FILENAME_PREFIX_EMBEDDED_COVER);
    }
}
