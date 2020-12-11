package de.danoeh.antennapod.core.glide;

import android.content.ContentResolver;
import android.content.Context;
import android.media.MediaMetadataRetriever;

import android.net.Uri;
import androidx.annotation.NonNull;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

// see https://github.com/bumptech/glide/issues/699
class AudioCoverFetcher implements DataFetcher<InputStream> {

    private static final String TAG = "AudioCoverFetcher";

    private final String path;
    private final Context context;

    public AudioCoverFetcher(String path, Context context) {
        this.path = path;
        this.context = context;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (path.startsWith(ContentResolver.SCHEME_CONTENT)) {
                retriever.setDataSource(context, Uri.parse(path));
            } else {
                retriever.setDataSource(path);
            }
            byte[] picture = retriever.getEmbeddedPicture();
            if (picture != null) {
                callback.onDataReady(new ByteArrayInputStream(picture));
                return;
            }
        } finally {
            retriever.release();
        }
        callback.onLoadFailed(new IOException("Loading embedded cover did not work"));
    }

    @Override public void cleanup() {
        // nothing to clean up
    }

    @Override public void cancel() {
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
