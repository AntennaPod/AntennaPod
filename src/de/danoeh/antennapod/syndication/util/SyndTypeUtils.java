package de.danoeh.antennapod.syndication.util;

import org.apache.commons.io.FilenameUtils;

import android.webkit.MimeTypeMap;

/** Utility class for handling MIME-Types of enclosures */
public class SyndTypeUtils {

	private final static String VALID_MIMETYPE = "audio/.*" + "|" + "video/.*"
			+ "|" + "application/ogg";

	private SyndTypeUtils() {

	}

	public static boolean typeValid(String type) {
		return type.matches(VALID_MIMETYPE);
	}

	/**
	 * Should be used if mime-type of enclosure tag is not supported. This
	 * method will check if the mime-type of the file extension is supported. If
	 * the type is not supported, this method will return null.
	 */
	public static String getValidMimeTypeFromUrl(String url) {
		String extension = FilenameUtils.getExtension(url);
		if (extension != null) {
			String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
					extension);
			if (typeValid(type)) {
				return type;
			}
		}
		return null;
	}
}
