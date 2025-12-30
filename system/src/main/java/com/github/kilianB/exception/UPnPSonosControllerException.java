package com.github.kilianB.exception;

/**
 * @author vmichalak
 */
public class UPnPSonosControllerException extends SonosControllerException {

	private static final long serialVersionUID = 1L;
	private final int errorCode;
	private final String errorDescription;
	private final String response;

	public UPnPSonosControllerException(String message, int errorCode, String errorDescription, String response) {
		super(message);
		this.errorCode = errorCode;
		this.errorDescription = errorDescription;
		this.response = response;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getErrorDescription() {
		return errorDescription;
	}

	public String getResponse() {
		return response;
	}
}
