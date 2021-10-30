package de.danoeh.antennapod.core.glide;

import android.net.Uri;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.model.StringLoader;

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
        return !model.startsWith("http") && super.handles(model);
    }
}
