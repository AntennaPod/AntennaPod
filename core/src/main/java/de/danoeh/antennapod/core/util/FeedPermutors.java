package de.danoeh.antennapod.core.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;

import static de.danoeh.antennapod.core.util.SortOrder.Scope.INTER_FEED;
import static de.danoeh.antennapod.core.util.SortOrder.Scope.INTRA_FEED;

/**
 * Provides method for sorting the a list of {@link de.danoeh.antennapod.core.feed.Feed} according to rules.
 */
public class FeedPermutors {

    /**
     * Returns a Permutor that sorts a list appropriate to the given sort order.
     *
     * @return Permutor that sorts a list appropriate to the given sort order.
     */
    @NonNull
    public static Permutor<Feed> getPermutor(@NonNull SortOrder sortOrder) {

        Comparator<Feed> comparator = null;
        Permutor<Feed> permutor = null;

        switch (sortOrder) {
//            case DATE_OLD_NEW:
//                comparator = (f1, f2) -> pubDate(f1).compareTo(pubDate(f2));
//                break;
//            case DATE_NEW_OLD:
//                comparator = (f1, f2) -> pubDate(f2).compareTo(pubDate(f1));
//                break;
//            case DURATION_SHORT_LONG:
//                comparator = (f1, f2) -> Integer.compare(duration(f1), duration(f2));
//                break;
//            case DURATION_LONG_SHORT:
//                comparator = (f1, f2) -> Integer.compare(duration(f2), duration(f1));
//                break;
            case FEED_TITLE_A_Z:
                comparator = (f1, f2) -> feedTitle(f1).compareTo(feedTitle(f2));
                break;
            case FEED_TITLE_Z_A:
                comparator = (f1, f2) -> feedTitle(f2).compareTo(feedTitle(f1));
                break;
        }

        if (comparator != null) {
            final Comparator<Feed> comparator2 = comparator;
            permutor = (queue) -> Collections.sort(queue, comparator2);
        }
        return permutor;
    }

    // Null-safe accessors

    @NonNull
    private static Date pubDate(@Nullable FeedItem item) {
        return (item != null && item.getPubDate() != null) ?
                item.getPubDate() : new Date(0);
    }

    @NonNull
    private static String itemTitle(@Nullable FeedItem item) {
        return (item != null && item.getTitle() != null) ?
                item.getTitle() : "";
    }

    private static int duration(@Nullable FeedItem item) {
        return (item != null && item.getMedia() != null) ?
                item.getMedia().getDuration() : 0;
    }

    @NonNull
    private static String feedTitle(@Nullable Feed feed) {
        return (feed != null && feed.getTitle() != null) ?
                feed.getTitle() : "";
    }

    /**
     * Provides sort orders to sort a list of episodes.
     */
    public enum SortOrder {
//        DATE_OLD_NEW(1, INTRA_FEED),
//        DATE_NEW_OLD(2, INTRA_FEED),
        FEED_TITLE_A_Z(101, INTER_FEED),
        FEED_TITLE_Z_A(102, INTER_FEED);

        public enum Scope {
            INTRA_FEED, INTER_FEED
        }

        public final int code;

        @NonNull
        public final de.danoeh.antennapod.core.util.SortOrder.Scope scope;

        SortOrder(int code, @NonNull de.danoeh.antennapod.core.util.SortOrder.Scope scope) {
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
        public static SortOrder fromCodeString(@Nullable String codeStr) {
            if (TextUtils.isEmpty(codeStr)) {
                return null;
            }
            int code = Integer.parseInt(codeStr);
            for (SortOrder sortOrder : values()) {
                if (sortOrder.code == code) {
                    return sortOrder;
                }
            }
            throw new IllegalArgumentException("Unsupported code: " + code);
        }

        @Nullable
        public static String toCodeString(@Nullable de.danoeh.antennapod.core.util.SortOrder sortOrder) {
            return sortOrder != null ? Integer.toString(sortOrder.code) : null;
        }
    }

}
