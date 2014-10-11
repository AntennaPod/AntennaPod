package de.danoeh.antennapod.core.syndication.util;

import android.webkit.MimeTypeMap;
import org.apache.commons.io.FilenameUtils;

/** Utility class for handling MIME-Types of enclosures */
public class SyndTypeUtils {

	private final static String VALID_MIMETYPE = "audio/.*" + "|" + "video/.*"
			+ "|" + "application/ogg";

	private SyndTypeUtils() {

	}

	public static boolean enclosureTypeValid(String type) {
		if (type == null) {
			return false;
		} else {
			return type.matches(VALID_MIMETYPE);
		}
	}

	/**
	 * Should be used if mime-type of enclosure tag is not supported. This
	 * method will check if the mime-type of the file extension is supported. If
	 * the type is not supported, this method will return null.
	 */
	public static String getValidMimeTypeFromUrl(String url) {
		if (url != null) {
			String extension = FilenameUtils.getExtension(url);
			if (extension != null) {
				String type = MimeTypeMap.getSingleton()
						.getMimeTypeFromExtension(extension);
				if (type != null && enclosureTypeValid(type)) {
					return type;
				}
			}
		}
		return null;
	}
}
