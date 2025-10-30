package de.danoeh.antennapod.model;

import android.media.MediaMetadataRetriever;

import java.io.IOException;

/**
 * On SDK<29, this class does not have a close method yet, so the app crashes when using try-with-resources.
 */
public class MediaMetadataRetrieverCompat extends MediaMetadataRetriever {
    public void close() {
        try {
            release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
