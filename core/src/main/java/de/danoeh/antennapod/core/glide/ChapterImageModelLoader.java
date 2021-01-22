package de.danoeh.antennapod.core.glide;

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
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.util.EmbeddedChapterImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;

public final class ChapterImageModelLoader implements ModelLoader<EmbeddedChapterImage, ByteBuffer> {

    public static class Factory implements ModelLoaderFactory<EmbeddedChapterImage, ByteBuffer> {
        @NonNull
        @Override
        public ModelLoader<EmbeddedChapterImage, ByteBuffer> build(@NonNull MultiModelLoaderFactory unused) {
            return new ChapterImageModelLoader();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    @Nullable
    @Override
    public LoadData<ByteBuffer> buildLoadData(@NonNull EmbeddedChapterImage model,
                                              int width,
                                              int height,
                                              @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model), new EmbeddedImageFetcher(model));
    }

    @Override
    public boolean handles(@NonNull EmbeddedChapterImage model) {
        return true;
    }

    static class EmbeddedImageFetcher implements DataFetcher<ByteBuffer> {
        private final EmbeddedChapterImage image;

        public EmbeddedImageFetcher(EmbeddedChapterImage image) {
            this.image = image;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super ByteBuffer> callback) {

            BufferedInputStream stream = null;
            try {
                if (image.getMedia().localFileAvailable()) {
                    File localFile = new File(image.getMedia().getLocalMediaUrl());
                    stream = new BufferedInputStream(new FileInputStream(localFile));
                    stream.skip(image.getPosition());
                    byte[] imageContent = new byte[image.getLength()];
                    stream.read(imageContent, 0, image.getLength());
                    callback.onDataReady(ByteBuffer.wrap(imageContent));
                } else {
                    Request.Builder httpReq = new Request.Builder();
                    // Skipping would download the whole file
                    httpReq.header("Range", "bytes=" + image.getPosition()
                            + "-" + (image.getPosition() + image.getLength()));
                    httpReq.url(image.getMedia().getStreamUrl());
                    Response response = AntennapodHttpClient.getHttpClient().newCall(httpReq.build()).execute();
                    if (!response.isSuccessful() || response.body() == null) {
                        throw new IOException("Invalid response: " + response.code() + " " + response.message());
                    }
                    callback.onDataReady(ByteBuffer.wrap(response.body().bytes()));
                }
            } catch (IOException e) {
                callback.onLoadFailed(e);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }

        @Override public void cleanup() {
            // nothing to clean up
        }
        @Override public void cancel() {
            // cannot cancel
        }

        @NonNull
        @Override
        public Class<ByteBuffer> getDataClass() {
            return ByteBuffer.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }
}
