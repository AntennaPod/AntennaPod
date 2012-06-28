package de.podfetcher.util;

import de.podfetcher.R;
import android.app.DownloadManager;
import android.content.Context;

/** Utility class for Download Errors. */
public class DownloadError {
	public static final int ERROR_PARSER_EXCEPTION = 1;
	public static final int ERROR_UNSUPPORTED_TYPE = 2;
	public static final int ERROR_CONNECTION_ERROR = 3;
	
	
	/** Get a human-readable string for a specific error code. */
	public static String getErrorString(Context context, int code) {
		int resId;
		switch(code) {
		case DownloadManager.ERROR_DEVICE_NOT_FOUND:
			resId = R.string.download_error_insufficient_space;
			break;
		case DownloadManager.ERROR_FILE_ERROR:
			resId = R.string.download_error_file_error;
			break;
		case DownloadManager.ERROR_HTTP_DATA_ERROR:
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
		default:
			resId = R.string.download_error_error_unknown;
		}
		return context.getString(resId);
	}

}
