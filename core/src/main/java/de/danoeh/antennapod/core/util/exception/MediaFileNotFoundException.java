package de.danoeh.antennapod.core.util.exception;

import de.danoeh.antennapod.core.feed.FeedMedia;

public class MediaFileNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	private final FeedMedia media;

	public MediaFileNotFoundException(String msg, FeedMedia media) {
		super(msg);
		this.media = media;
	}

	public MediaFileNotFoundException(FeedMedia media) {
		super();
		this.media = media;
	}

	public FeedMedia getMedia() {
		return media;
	}
}
