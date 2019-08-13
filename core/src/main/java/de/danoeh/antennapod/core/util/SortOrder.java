package de.danoeh.antennapod.core.util;

/**
 * Provides sort orders to sort a list of episodes.
 */
public enum SortOrder {
    EPISODE_TITLE_A_Z,
    EPISODE_TITLE_Z_A,
    DATE_OLD_NEW,
    DATE_NEW_OLD,
    DURATION_SHORT_LONG,
    DURATION_LONG_SHORT,
    FEED_TITLE_A_Z,
    FEED_TITLE_Z_A,
    RANDOM,
    SMART_SHUFFLE_OLD_NEW,
    SMART_SHUFFLE_NEW_OLD;

    /**
     * Converts the string representation to its enum value. If the string value is unknown,
     * the given default value is returned.
     */
    public static SortOrder parseWithDefault(String value, SortOrder defaultValue) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
