package de.danoeh.antennapod.core.preferences;

/**
 * Represents an action to take when a media button event is received.
 */
public enum MediaAction {
    /**
     * Take no action.
     */
    NONE,
    /**
     * Skip to another episode.
     */
    SKIP,
    /**
     * Change the position in the current episode.
     */
    SEEK,
    /**
     * Change the playback speed.
     */
    SPEED,
    /**
     * Restart the episode.
     */
    RESTART;

    public static MediaAction fromString(String s, MediaAction defaultAction) {
        try {
            int i = Integer.valueOf(s);
            MediaAction[] options = values();
            if(i >= 0 && i < options.length) {
                return options[i];
            }
        } catch(NumberFormatException nfe) {
            // Fall through
        }
        return defaultAction;
    }
}
