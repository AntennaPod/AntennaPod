package de.danoeh.antennapod.parser.feed.util;

import android.webkit.MimeTypeMap;
import androidx.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;

/**
 * Utility class for handling MIME-Types of enclosures.
 * */
public class SyndTypeUtils {
    public static final String OCTET_STREAM = "application/octet-stream";

    private SyndTypeUtils() {

    }

    @Nullable
    public static String getMimeType(@Nullable String type, @Nullable String filename) {
        if (isMediaFile(type) && !OCTET_STREAM.equals(type)) {
            return type;
        }
        String filenameType = SyndTypeUtils.getMimeTypeFromUrl(filename);
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
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
}
