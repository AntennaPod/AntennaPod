package de.danoeh.antennapod.net.sync.nostr;

import de.danoeh.antennapod.net.sync.model.SyncServiceException;

public class NostrServiceException extends SyncServiceException {

    private static final long serialVersionUID = 1L;

    public NostrServiceException(String message) {
        super(message);
    }

    public NostrServiceException(Throwable cause) {
        super(cause);
    }
}
