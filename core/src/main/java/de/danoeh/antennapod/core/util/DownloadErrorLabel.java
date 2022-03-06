package de.danoeh.antennapod.core.util;

import androidx.annotation.StringRes;
import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.model.download.DownloadError;

/**
 * Provides user-visible labels for download errors.
 */
public class DownloadErrorLabel {

    @StringRes
    public static int from(DownloadError error) {
        switch (error) {
            case SUCCESS: return R.string.download_successful;
            case ERROR_PARSER_EXCEPTION: return R.string.download_error_parser_exception;
            case ERROR_UNSUPPORTED_TYPE: return R.string.download_error_unsupported_type;
            case ERROR_CONNECTION_ERROR: return R.string.download_error_connection_error;
            case ERROR_MALFORMED_URL: return R.string.download_error_error_unknown;
            case ERROR_IO_ERROR: return R.string.download_error_io_error;
            case ERROR_FILE_EXISTS: return R.string.download_error_error_unknown;
            case ERROR_DOWNLOAD_CANCELLED: return R.string.download_canceled_msg;
            case ERROR_DEVICE_NOT_FOUND: return R.string.download_error_device_not_found;
            case ERROR_HTTP_DATA_ERROR: return R.string.download_error_http_data_error;
            case ERROR_NOT_ENOUGH_SPACE: return R.string.download_error_insufficient_space;
            case ERROR_UNKNOWN_HOST: return R.string.download_error_unknown_host;
            case ERROR_REQUEST_ERROR: return R.string.download_error_request_error;
            case ERROR_DB_ACCESS_ERROR: return R.string.download_error_db_access;
            case ERROR_UNAUTHORIZED: return R.string.download_error_unauthorized;
            case ERROR_FILE_TYPE: return R.string.download_error_file_type_type;
            case ERROR_FORBIDDEN: return R.string.download_error_forbidden;
            case ERROR_IO_WRONG_SIZE: return R.string.download_error_wrong_size;
            case ERROR_IO_BLOCKED: return R.string.download_error_blocked;
            case ERROR_UNSUPPORTED_TYPE_HTML: return R.string.download_error_unsupported_type_html;
            case ERROR_NOT_FOUND: return R.string.download_error_not_found;
            case ERROR_CERTIFICATE: return R.string.download_error_certificate;
            case ERROR_PARSER_EXCEPTION_DUPLICATE: return R.string.download_error_parser_exception;
            default:
                if (BuildConfig.DEBUG) {
                    throw new IllegalArgumentException("No mapping from download error to label");
                }
                return R.string.download_error_error_unknown;
        }
    }
}
