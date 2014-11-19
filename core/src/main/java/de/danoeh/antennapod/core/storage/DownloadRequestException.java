package de.danoeh.antennapod.core.storage;

/**
 * Thrown by the DownloadRequester if a download request contains invalid data
 * or something went wrong while processing the request.
 */
public class DownloadRequestException extends Exception {

	public DownloadRequestException() {
		super();
	}

	public DownloadRequestException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public DownloadRequestException(String detailMessage) {
		super(detailMessage);
	}

	public DownloadRequestException(Throwable throwable) {
		super(throwable);
	}

}
