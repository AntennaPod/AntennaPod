package de.danoeh.antennapod.core.util;

/** Thrown if a feed has invalid attribute values. */
public class InvalidFeedException extends Exception {

	public InvalidFeedException() {
	}

	public InvalidFeedException(String detailMessage) {
		super(detailMessage);
	}

	public InvalidFeedException(Throwable throwable) {
		super(throwable);
	}

	public InvalidFeedException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
