package de.danoeh.antennapod.core.feed;

import android.text.TextUtils;

public enum MediaType {
	AUDIO, VIDEO, UNKNOWN;

	public static MediaType fromMimeType(String mime_type) {
		if (TextUtils.isEmpty(mime_type)) {
			return MediaType.UNKNOWN;
		} else if (mime_type.startsWith("audio")) {
			return MediaType.AUDIO;
		} else if (mime_type.startsWith("video")) {
			return MediaType.VIDEO;
		} else if (mime_type.equals("application/ogg")) {
			return MediaType.AUDIO;
		}
		return MediaType.UNKNOWN;
	}
}
