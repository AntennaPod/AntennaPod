package de.danoeh.antennapod.core.sync.model;

public class SyncServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    public SyncServiceException(String message) {
        super(message);
    }

    public SyncServiceException(Throwable cause) {
        super(cause);
    }
}
