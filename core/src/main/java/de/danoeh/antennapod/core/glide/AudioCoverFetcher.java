package de.danoeh.antennapod.core.glide;

import android.media.MediaMetadataRetriever;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

// see https://github.com/bumptech/glide/issues/699
class AudioCoverFetcher implements DataFetcher<InputStream> {

    private static final String TAG = "AudioCoverFetcher";

    private final String path;

    public AudioCoverFetcher(String path) {
        this.path = path;
    }

    @Override public String getId() {
        return path;
    }

    @Override public InputStream loadData(Priority priority) throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] picture = retriever.getEmbeddedPicture();
            if (picture != null) {
                return new ByteArrayInputStream(picture);
            }
        } finally {
            retriever.release();
        }
        throw new IOException("Loading embedded cover did not work");
    }

    @Override public void cleanup() {
        // nothing to clean up
    }
    @Override public void cancel() {
        // cannot cancel
    }
}
