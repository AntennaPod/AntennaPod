package de.danoeh.antennapod.core.storage;

import android.text.TextUtils;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

import java.text.DateFormat;
import java.util.Locale;

/**
 * Publishers sometimes mess up their feed by adding episodes twice or by changing the ID of existing episodes.
 * This class tries to guess if publishers actually meant another episode,
 * even if their feed explicitly says that the episodes are different.
 */
public class FeedItemDuplicateGuesser {
    public static boolean seemDuplicates(FeedItem item1, FeedItem item2) {
        if (sameAndNotEmpty(item1.getItemIdentifier(), item2.getItemIdentifier())) {
            return true;
        }
        FeedMedia media1 = item1.getMedia();
        FeedMedia media2 = item2.getMedia();
        if (media1 == null || media2 == null) {
            return false;
        }
        if (sameAndNotEmpty(media1.getStreamUrl(), media2.getStreamUrl())) {
            return true;
        }
        return titlesLookSimilar(item1, item2)
                && datesLookSimilar(item1, item2)
                && durationsLookSimilar(media1, media2)
                && TextUtils.equals(media1.getMime_type(), media2.getMime_type());
    }

    private static boolean sameAndNotEmpty(String string1, String string2) {
        if (TextUtils.isEmpty(string1) || TextUtils.isEmpty(string2)) {
            return false;
        }
        return string1.equals(string2);
    }

    private static boolean datesLookSimilar(FeedItem item1, FeedItem item2) {
        if (item1.getPubDate() == null || item2.getPubDate() == null) {
            return false;
        }
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US); // MM/DD/YY
        String dateOriginal = dateFormat.format(item2.getPubDate());
        String dateNew = dateFormat.format(item1.getPubDate());
        return TextUtils.equals(dateOriginal, dateNew); // Same date; time is ignored.
    }

    private static boolean durationsLookSimilar(FeedMedia media1, FeedMedia media2) {
        return Math.abs(media1.getDuration() - media2.getDuration()) < 10 * 60L * 1000L;
    }

    private static boolean titlesLookSimilar(FeedItem item1, FeedItem item2) {
        return sameAndNotEmpty(canonicalizeTitle(item1.getTitle()), canonicalizeTitle(item2.getTitle()));
    }

    private static String canonicalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        return title
                .trim()
                .replace('“', '"')
                .replace('”', '"')
                .replace('„', '"')
                .replace('—', '-');
    }
}
