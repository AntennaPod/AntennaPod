package de.danoeh.antennapod.ui.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import de.danoeh.antennapod.model.feed.Feed;

class GenerativePlaceholderImageModelLoader implements ModelLoader<String, InputStream> {

    public static class Factory implements ModelLoaderFactory<String, InputStream> {
        @NonNull
        @Override
        public ModelLoader<String, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new GenerativePlaceholderImageModelLoader();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull String model, int width, int height, @NonNull Options options) {
        if (model.startsWith(Feed.PREFIX_GENERATIVE_COVER)) {
            String data = model.substring(Feed.PREFIX_GENERATIVE_COVER.length());
            
            // Find the ### separator to extract feedDownloadUrl
            int separatorIndex = data.indexOf("###");
            if (separatorIndex == -1) {
                return null; // Invalid format
            }
            
            String feedDownloadUrl = data.substring(0, separatorIndex);
            String remaining = data.substring(separatorIndex + 3); // Skip "###"
            
            boolean dummy = remaining.charAt(0) == '1';
            boolean initialized = remaining.charAt(1) == '1';
            boolean noFallbackText = remaining.charAt(2) == '1';
            String text = remaining.substring(4); // Skip "XXX/"

            if (dummy) {
                return new LoadData<>(new ObjectKey(model), new EmptyImageFetcher(width, height));
            }
            return new LoadData<>(new ObjectKey(model),
                    new TextImageFetcher(
                            model,
                            text,
                            feedDownloadUrl,
                            width,
                            height,
                            !noFallbackText));
        }
        return null;
    }

    @Override
    public boolean handles(@NonNull String model) {
        return model.startsWith(Feed.PREFIX_GENERATIVE_COVER);
    }

    private static class EmptyImageFetcher implements DataFetcher<InputStream> {
        private final int width;
        private final int height;

        public EmptyImageFetcher(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            try {
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(0xFFF5F5F5);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                InputStream inputStream = new ByteArrayInputStream(stream.toByteArray());

                callback.onDataReady(inputStream);
            } catch (Exception e) {
                callback.onLoadFailed(e);
            }
        }

        @Override
        public void cleanup() {
            // nothing to clean up
        }

        @Override
        public void cancel() {
            // cannot cancel
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }

    static class TextImageFetcher implements DataFetcher<InputStream> {

        private final String text;
        private final String feedDownloadUrl;
        private final int width;
        private final int height;
        private final boolean showText;
        private final String model;

        TextImageFetcher(String model, String text, String feedDownloadUrl, int width, int height, boolean showText) {
            this.model = model;
            this.text = text;
            this.feedDownloadUrl = feedDownloadUrl;
            this.width = width;
            this.height = height;
            this.showText = showText;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            try {
                InputStream stream = generatePlaceholder();
                callback.onDataReady(stream);
            } catch (Exception e) {
                callback.onLoadFailed(e);
            }
        }

        private InputStream generatePlaceholder() {
            return TextImageGenerator.generatePlaceholderImage(
                    model,
                    text,
                    feedDownloadUrl,
                    width,
                    height,
                    false,
                    showText);
        }

        @Override
        public void cleanup() {
            // Nothing to clean up
        }

        @Override
        public void cancel() {
            // Nothing to cancel
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }
}
