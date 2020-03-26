package de.danoeh.antennapod.core.syndication.util;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;

import java.util.Arrays;

/** Utility class for handling MIME-Types of enclosures */
public class SyndTypeUtils {

	private static final String VALID_MEDIA_MIMETYPE = TextUtils.join("|", Arrays.asList(
			"audio/.*",
			"video/.*",
			"application/ogg",
			"application/octet-stream"));

	private static final String VALID_IMAGE_MIMETYPE = "image/.*";

	private SyndTypeUtils() {

	}

	public static boolean enclosureTypeValid(String type) {
		if (type == null) {
			return false;
		} else {
			return type.matches(VALID_MEDIA_MIMETYPE);
		}
	}
	public static boolean imageTypeValid(String type) {
		if (type == null) {
			return false;
		} else {
			return type.matches(VALID_IMAGE_MIMETYPE);
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
		if (extension == null) {
			return null;
		}

		return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
	}
}
