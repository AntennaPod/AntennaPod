package de.danoeh.antennapod.parser.feed.util;

import android.webkit.MimeTypeMap;
import org.apache.commons.io.FilenameUtils;

/**
 * Utility class for handling MIME-Types of enclosures.
 * */
public class SyndTypeUtils {
    private SyndTypeUtils() {

    }

    public static boolean enclosureTypeValid(String type) {
        if (type == null) {
            return false;
        } else {
            return type.startsWith("audio/")
                    || type.startsWith("video/")
                    || type.equals("application/ogg")
                    || type.equals("application/octet-stream");
        }
    }

    public static boolean imageTypeValid(String type) {
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
    public static String getMimeTypeFromUrl(String url) {
        if (url == null) {
            return null;
        }
        String extension = FilenameUtils.getExtension(url);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
}
