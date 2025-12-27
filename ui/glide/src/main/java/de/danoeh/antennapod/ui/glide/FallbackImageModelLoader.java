package de.danoeh.antennapod.ui.glide;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

import de.danoeh.antennapod.ui.common.FallbackImageData;

public final class FallbackImageModelLoader implements ModelLoader<FallbackImageData, InputStream> {

    public static class Factory implements ModelLoaderFactory<FallbackImageData, InputStream> {
        @NonNull
        @Override
        public ModelLoader<FallbackImageData, InputStream> build(@NonNull MultiModelLoaderFactory unused) {
            return new FallbackImageModelLoader();
        }

        @Override
        public void teardown() {
        }
    }

    @Override
    public LoadData<InputStream> buildLoadData(
            @NonNull FallbackImageData model,
            int width,
            int height,
            @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model.toString()),
                new FallbackImageFetcher(model, width, height));
    }

    @Override
    public boolean handles(@NonNull FallbackImageData model) {
        return true;
    }

    static class FallbackImageFetcher implements DataFetcher<InputStream> {
        private final FallbackImageData model;
        private final int width;
        private final int height;

        public FallbackImageFetcher(FallbackImageData model, int width, int height) {
            this.model = model;
            this.width = width;
            this.height = height;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            try {
                InputStream inputStream = TextImageGenerator.generatePlaceholderImage(
                        model.getFeedDownloadUrl(),
                        model.getFallbackText(),
                        null,
                        width,
                        height,
                        true,
                        !model.isShowImageWithoutFallbackText());
                callback.onDataReady(inputStream);
            } catch (Exception e) {
                callback.onLoadFailed(e);
            }
        }

        @Override
        public void cleanup() {

        }

        @Override
        public void cancel() {

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
