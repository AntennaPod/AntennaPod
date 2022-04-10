package de.danoeh.antennapod.parser.feed.util;

import android.webkit.MimeTypeMap;
import androidx.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for handling MIME-Types of enclosures.
 * */
public class MimeTypeUtils {
    public static final String OCTET_STREAM = "application/octet-stream";

    // based on https://developer.android.com/guide/topics/media/media-formats
    static final Set<String> AUDIO_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "3gp", "aac", "amr", "flac", "imy", "m4a", "m4b", "mid", "mkv", "mp3", "mp4", "mxmf", "oga",
            "ogg", "ogx", "opus", "ota", "rtttl", "rtx", "wav", "xmf"
    ));

    static final Set<String> VIDEO_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "3gp", "mkv", "mp4", "ogg", "ogv", "ogx", "webm"
    ));

    private MimeTypeUtils() {

    }

    @Nullable
    public static String getMimeType(@Nullable String type, @Nullable String filename) {
        if (isMediaFile(type) && !OCTET_STREAM.equals(type)) {
            return type;
        }
        String filenameType = MimeTypeUtils.getMimeTypeFromUrl(filename);
        if (isMediaFile(filenameType)) {
            return filenameType;
        }
        return type;
    }

    public static boolean isMediaFile(String type) {
        if (type == null) {
            return false;
        } else {
            return type.startsWith("audio/")
                    || type.startsWith("video/")
                    || type.equals("application/ogg")
                    || type.equals("application/octet-stream");
        }
    }

    public static boolean isImageFile(String type) {
        if (type == null) {
            return false;
        } else {
            return type.startsWith("image/");
        }
    }

    /**
     * Should be used if mime-type of enclosure tag is not supported. This
     * method will return the mime-type of the file extension.
     */
    private static String getMimeTypeFromUrl(String url) {
        if (url == null) {
            return null;
        }
        String extension = FilenameUtils.getExtension(url);
        String mapResult = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mapResult != null) {
            return mapResult;
        }

        if (AUDIO_FILE_EXTENSIONS.contains(extension)) {
            return "audio/*";
        } else if (VIDEO_FILE_EXTENSIONS.contains(extension)) {
            return "video/*";
        }
        return null;
    }
}
