package de.danoeh.antennapod.core.feed;

import androidx.annotation.Nullable;

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

    // The constant SHOULD NEVER be changed, as it is used in db DDLs
    public static final int CODE_UNSPECIFIED = 0;

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

    @Nullable
    public static IntraFeedSortOrder fromCode(int code) {
        if (code == CODE_UNSPECIFIED) { // sort order not specified
            return null;
        }
        for (IntraFeedSortOrder sortOrder : values()) {
            if (sortOrder.code == code) {
                return sortOrder;
            }
        }
        throw new IllegalArgumentException("Unsupported code: " + code);
    }

    public static int toCode(@Nullable IntraFeedSortOrder sortOrder) {
        return sortOrder != null ? sortOrder.code : CODE_UNSPECIFIED;
    }

}
