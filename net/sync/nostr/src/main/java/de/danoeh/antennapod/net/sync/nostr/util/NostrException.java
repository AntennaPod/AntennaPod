package de.danoeh.antennapod.net.sync.nostr.util;

public class NostrException extends Exception {

    public NostrException(String message) {
        super(message);
    }

    public NostrException(Throwable cause) {
        super(cause);
    }
}
