package de.danoeh.antennapod.model.playback;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum MediaType {
    AUDIO, VIDEO, UNKNOWN;

    private static final Set<String> AUDIO_APPLICATION_MIME_STRINGS = new HashSet<>(Arrays.asList(
            "application/ogg",
            "application/opus",
            "application/x-flac"
    ));

    public static MediaType fromMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return MediaType.UNKNOWN;
        } else if (mimeType.startsWith("audio")) {
            return MediaType.AUDIO;
        } else if (mimeType.startsWith("video")) {
            return MediaType.VIDEO;
        } else if (AUDIO_APPLICATION_MIME_STRINGS.contains(mimeType)) {
            return MediaType.AUDIO;
        }
        return MediaType.UNKNOWN;
    }
}
