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
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.util.EmbeddedChapterImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import org.apache.commons.io.IOUtils;

public final class ChapterImageModelLoader implements ModelLoader<EmbeddedChapterImage, ByteBuffer> {

    public static class Factory implements ModelLoaderFactory<EmbeddedChapterImage, ByteBuffer> {
        @Override
        public ModelLoader<EmbeddedChapterImage, ByteBuffer> build(MultiModelLoaderFactory unused) {
            return new ChapterImageModelLoader();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    @Nullable
    @Override
    public LoadData<ByteBuffer> buildLoadData(EmbeddedChapterImage model, int width, int height, Options options) {
        return new LoadData<>(new ObjectKey(model), new EmbeddedImageFetcher(model));
    }

    @Override
    public boolean handles(EmbeddedChapterImage model) {
        return true;
    }

    class EmbeddedImageFetcher implements DataFetcher<ByteBuffer> {
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
                } else {
                    URL url = new URL(image.getMedia().getStreamUrl());
                    stream = new BufferedInputStream(url.openStream());
                }
                byte[] imageContent = new byte[image.getLength()];
                stream.skip(image.getPosition());
                stream.read(imageContent, 0, image.getLength());
                callback.onDataReady(ByteBuffer.wrap(imageContent));
            } catch (IOException e) {
                callback.onLoadFailed(new IOException("Loading embedded cover did not work"));
                e.printStackTrace();
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
