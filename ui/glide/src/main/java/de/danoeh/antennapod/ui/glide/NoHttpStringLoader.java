package de.danoeh.antennapod.ui.glide;

import android.net.Uri;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.model.StringLoader;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;

import java.io.InputStream;

/**
 * StringLoader that does not handle http/https urls. Used to avoid fallback to StringLoader when
 * AntennaPod blocks mobile image loading.
 */
public final class NoHttpStringLoader extends StringLoader<InputStream> {

    public static class StreamFactory implements ModelLoaderFactory<String, InputStream> {
        @NonNull
        @Override
        public ModelLoader<String, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new NoHttpStringLoader(multiFactory.build(Uri.class, InputStream.class));
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    public NoHttpStringLoader(ModelLoader<Uri, InputStream> uriLoader) {
        super(uriLoader);
    }

    @Override
    public boolean handles(@NonNull String model) {
        return !model.startsWith("http")
                // If the custom loaders fail, do not attempt to load with Glide internal loaders
                && !model.startsWith(Feed.PREFIX_GENERATIVE_COVER)
                && !model.startsWith(FeedMedia.FILENAME_PREFIX_EMBEDDED_COVER)
                && super.handles(model);
    }
}
