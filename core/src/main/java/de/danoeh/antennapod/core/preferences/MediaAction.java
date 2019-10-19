package de.danoeh.antennapod.core.preferences;

/**
 * Action to take when a media button event is received.
 */
public enum MediaAction {
    NONE,
    PLAY, PAUSE, TOGGLE_PLAYBACK,
    SKIP_FORWARD, SKIP_BACKWARD,
    SEEK_FORWARD, SEEK_BACKWARD,
    SPEED_INCREASE, SPEED_DECREASE,
    RESTART;

    public static MediaAction fromString(String s) {
        if (s != null) {
            String trimmed = s.trim();
            for (MediaAction option : values()) {
                if (option.name().equals(trimmed)) {
                    return option;
                }
            }
        }
        return NONE;
    }
}
