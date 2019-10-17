package de.danoeh.antennapod.core.feed;

/**
 * Provides sort orders to sort a list of episodes within a feed.
 */
public enum IntraFeedSortOrder {
    DATE_OLD_NEW(1),
    DATE_NEW_OLD(2),
    EPISODE_TITLE_A_Z(3),
    EPISODE_TITLE_Z_A(4),
    DURATION_SHORT_LONG(5),
    DURATION_LONG_SHORT(6);

    public final int code;

    IntraFeedSortOrder(int code) {
        this.code = code;
    }

    /**
     * Converts the string representation to its enum value. If the string value is unknown,
     * the given default value is returned.
     */
    public static IntraFeedSortOrder parseWithDefault(String value, IntraFeedSortOrder defaultValue) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
