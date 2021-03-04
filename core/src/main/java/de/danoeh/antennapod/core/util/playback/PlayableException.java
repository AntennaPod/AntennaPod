package de.danoeh.antennapod.core.util.playback;

/**
 * Exception thrown by {@link Playable} implementations.
 */
public class PlayableException extends Exception {

    private static final long serialVersionUID = 1L;

    public PlayableException(String detailMessage) {
        super(detailMessage);
    }
}
