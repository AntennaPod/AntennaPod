package de.danoeh.antennapod.ui.glide;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import de.danoeh.antennapod.model.feed.EmbeddedChapterImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;

public final class ChapterImageModelLoader implements ModelLoader<EmbeddedChapterImage, ByteBuffer> {
    public static class Factory implements ModelLoaderFactory<EmbeddedChapterImage, ByteBuffer> {
        private final Context context;

        public Factory(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public ModelLoader<EmbeddedChapterImage, ByteBuffer> build(@NonNull MultiModelLoaderFactory unused) {
            return new ChapterImageModelLoader(context);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    private final Context context;

    public ChapterImageModelLoader(Context context) {
        this.context = context;
    }

    @Override
    public LoadData<ByteBuffer> buildLoadData(@NonNull EmbeddedChapterImage model, int width,
                                              int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model), new EmbeddedImageFetcher(model, context));
    }

    @Override
    public boolean handles(@NonNull EmbeddedChapterImage model) {
        return true;
    }

    static class EmbeddedImageFetcher implements DataFetcher<ByteBuffer> {
        private final EmbeddedChapterImage image;
        private final Context context;

        public EmbeddedImageFetcher(EmbeddedChapterImage image, Context context) {
            this.image = image;
            this.context = context;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super ByteBuffer> callback) {

            BufferedInputStream stream = null;
            try {
                boolean isLocalFeed = image.getMedia().getStreamUrl().startsWith(ContentResolver.SCHEME_CONTENT);
                if (isLocalFeed || image.getMedia().localFileAvailable()) {
                    if (isLocalFeed) {
                        Uri uri = Uri.parse(image.getMedia().getStreamUrl());
                        stream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
                    } else {
                        File localFile = new File(image.getMedia().getLocalFileUrl());
                        stream = new BufferedInputStream(new FileInputStream(localFile));
                    }
                    IOUtils.skip(stream, image.getPosition());
                    byte[] imageContent = new byte[image.getLength()];
                    IOUtils.read(stream, imageContent, 0, image.getLength());
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
