package de.danoeh.antennapod.core.util;

import android.content.Context;

import de.danoeh.antennapod.core.R;

/** Utility class for Download Errors. */
public enum DownloadError {
	SUCCESS(0, R.string.download_successful),
	ERROR_PARSER_EXCEPTION(1, R.string.download_error_parser_exception),
	ERROR_UNSUPPORTED_TYPE(2, R.string.download_error_unsupported_type),
	ERROR_CONNECTION_ERROR(3, R.string.download_error_connection_error),
	ERROR_MALFORMED_URL(4, R.string.download_error_error_unknown),
	ERROR_IO_ERROR(5, R.string.download_error_io_error),
	ERROR_FILE_EXISTS(6, R.string.download_error_error_unknown),
	ERROR_DOWNLOAD_CANCELLED(7, R.string.download_error_error_unknown),
	ERROR_DEVICE_NOT_FOUND(8, R.string.download_error_device_not_found),
	ERROR_HTTP_DATA_ERROR(9, R.string.download_error_http_data_error),
	ERROR_NOT_ENOUGH_SPACE(10, R.string.download_error_insufficient_space),
	ERROR_UNKNOWN_HOST(11, R.string.download_error_unknown_host),
	ERROR_REQUEST_ERROR(12, R.string.download_error_request_error),
	ERROR_DB_ACCESS_ERROR(13, R.string.download_error_db_access),
	ERROR_UNAUTHORIZED(14, R.string.download_error_unauthorized),
	ERROR_FILE_TYPE(15, R.string.download_error_file_type_type),
	ERROR_FORBIDDEN(16, R.string.download_error_forbidden),
	ERROR_IO_WRONG_SIZE(17, R.string.download_error_forbidden);

	private final int code;
	private final int resId;

	DownloadError(int code, int resId) {
		this.code = code;
		this.resId = resId;
	}

	/** Return DownloadError from its associated code. */
	public static DownloadError fromCode(int code) {
		for (DownloadError reason : values()) {
			if (reason.getCode() == code) {
				return reason;
			}
		}
		throw new IllegalArgumentException("unknown code: " + code);
	}

	/** Get machine-readable code. */
	public int getCode() {
		return code;
	}

	/** Get a human-readable string. */
	public String getErrorString(Context context) {
		return context.getString(resId);
	}

}
