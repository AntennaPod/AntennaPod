package de.danoeh.antennapod.core.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static de.danoeh.antennapod.core.util.SortOrder.Scope.INTER_FEED;
import static de.danoeh.antennapod.core.util.SortOrder.Scope.INTRA_FEED;

/**
 * Provides sort orders to sort a list of episodes.
 */
public enum SortOrder {
    DATE_OLD_NEW(1, INTRA_FEED),
    DATE_NEW_OLD(2, INTRA_FEED),
    EPISODE_TITLE_A_Z(3, INTRA_FEED),
    EPISODE_TITLE_Z_A(4, INTRA_FEED),
    DURATION_SHORT_LONG(5, INTRA_FEED),
    DURATION_LONG_SHORT(6, INTRA_FEED),
    FEED_TITLE_A_Z(101, INTER_FEED),
    FEED_TITLE_Z_A(102, INTER_FEED),
    RANDOM(103, INTER_FEED),
    SMART_SHUFFLE_OLD_NEW(104, INTER_FEED),
    SMART_SHUFFLE_NEW_OLD(105, INTER_FEED);

    public enum Scope {
        INTRA_FEED, INTER_FEED;
    }

    // The constant SHOULD NEVER be changed, as it is used in db DDLs
    public static final int CODE_UNSPECIFIED = 0;

    public final int code;

    @NonNull
    public final Scope scope;

    SortOrder(int code, @NonNull Scope scope) {
        this.code = code;
        this.scope = scope;
    }

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

    @Nullable
    public static SortOrder fromCode(int code) {
        if (code == CODE_UNSPECIFIED) {
            return null;
        }
        for (SortOrder sortOrder : values()) {
            if (sortOrder.code == code) {
                return sortOrder;
            }
        }
        throw new IllegalArgumentException("Unsupported code: " + code);
    }

    public static int toCode(@Nullable SortOrder sortOrder) {
        return sortOrder != null ? sortOrder.code : CODE_UNSPECIFIED;
    }
}
