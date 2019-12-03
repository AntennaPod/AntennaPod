package de.danoeh.antennapodSA.core.gpoddernet;

public class GpodnetServiceException extends Exception {
    private static final long serialVersionUID = 1L;

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
