package de.danoeh.antennapod.core.gpoddernet;

public class GpodnetServiceException extends Exception {

    GpodnetServiceException() {
    }

    GpodnetServiceException(String message) {
        super(message);
    }

    public GpodnetServiceException(Throwable cause) {
        super(cause);
    }

    GpodnetServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
