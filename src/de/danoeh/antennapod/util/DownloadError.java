package de.danoeh.antennapod.util;

import android.content.Context;
import de.danoeh.antennapod.R;

/** Utility class for Download Errors. */
public class DownloadError {
	public static final int ERROR_PARSER_EXCEPTION = 1;
	public static final int ERROR_UNSUPPORTED_TYPE = 2;
	public static final int ERROR_CONNECTION_ERROR = 3;
	public static final int ERROR_MALFORMED_URL = 4;
	public static final int ERROR_IO_ERROR = 5;
	public static final int ERROR_FILE_EXISTS = 6;
	public static final int ERROR_DOWNLOAD_CANCELLED = 7;
	public static final int ERROR_DEVICE_NOT_FOUND = 8;
	public static final int ERROR_HTTP_DATA_ERROR = 9;
	public static final int ERROR_NOT_ENOUGH_SPACE = 10;
	public static final int ERROR_UNKNOWN_HOST = 11;
	
	/** Get a human-readable string for a specific error code. */
	public static String getErrorString(Context context, int code) {
		int resId;
		switch(code) {
		case ERROR_NOT_ENOUGH_SPACE:
			resId = R.string.download_error_insufficient_space;
			break;
		case ERROR_DEVICE_NOT_FOUND:
			resId = R.string.download_error_device_not_found;
			break;
		case ERROR_IO_ERROR:
			resId = R.string.download_error_io_error;
			break;
		case ERROR_HTTP_DATA_ERROR:
			resId = R.string.download_error_http_data_error;
			break;
		case ERROR_PARSER_EXCEPTION:
			resId = R.string.download_error_parser_exception;
			break;
		case ERROR_UNSUPPORTED_TYPE:
			resId = R.string.download_error_unsupported_type;
			break;
		case ERROR_CONNECTION_ERROR:
			resId = R.string.download_error_connection_error;
			break;
		case ERROR_UNKNOWN_HOST:
			resId = R.string.download_error_unknown_host;
			break;
		default:
			resId = R.string.download_error_error_unknown;
		}
		return context.getString(resId);
	}

}
