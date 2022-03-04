package de.danoeh.antennapod.model.download;

/** Utility class for Download Errors. */
public enum DownloadError {
    SUCCESS(0),
    ERROR_PARSER_EXCEPTION(1),
    ERROR_UNSUPPORTED_TYPE(2),
    ERROR_CONNECTION_ERROR(3),
    ERROR_MALFORMED_URL(4),
    ERROR_IO_ERROR(5),
    ERROR_FILE_EXISTS(6),
    ERROR_DOWNLOAD_CANCELLED(7),
    ERROR_DEVICE_NOT_FOUND(8),
    ERROR_HTTP_DATA_ERROR(9),
    ERROR_NOT_ENOUGH_SPACE(10),
    ERROR_UNKNOWN_HOST(11),
    ERROR_REQUEST_ERROR(12),
    ERROR_DB_ACCESS_ERROR(13),
    ERROR_UNAUTHORIZED(14),
    ERROR_FILE_TYPE(15),
    ERROR_FORBIDDEN(16),
    ERROR_IO_WRONG_SIZE(17),
    ERROR_IO_BLOCKED(18),
    ERROR_UNSUPPORTED_TYPE_HTML(19),
    ERROR_NOT_FOUND(20),
    ERROR_CERTIFICATE(21),
    ERROR_PARSER_EXCEPTION_DUPLICATE(22);

    private final int code;

    DownloadError(int code) {
        this.code = code;
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
}
