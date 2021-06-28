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

    // based on https://developer.android.com/guide/topics/media/media-formats
    static final Set<String> AUDIO_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "3gp", "aac", "amr", "flac", "imy", "m4a", "mid", "mkv", "mp3", "mp4", "mxmf", "oga",
            "ogg", "ogx", "opus", "ota", "rtttl", "rtx", "wav", "xmf"
    ));

    static final Set<String> VIDEO_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "3gp", "mkv", "mp4", "ogg", "ogv", "ogx", "webm"
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

    /**
     * @param extensionWithoutDot the file extension (suffix) without the dot
     * @return the {@link MediaType} that likely corresponds to the extension. However, since the
     *         extension is not always enough to determine whether a file is an audio or video (3gp
     *         can be both, for example), this may not be correct. As a result, where possible,
     *         {@link #fromMimeType(String) fromMimeType} should always be tried first.
     */
    public static MediaType fromFileExtension(String extensionWithoutDot) {
        if (AUDIO_FILE_EXTENSIONS.contains(extensionWithoutDot)) {
            return MediaType.AUDIO;
        } else if (VIDEO_FILE_EXTENSIONS.contains(extensionWithoutDot)) {
            return MediaType.VIDEO;
        }
        return MediaType.UNKNOWN;
    }
}
