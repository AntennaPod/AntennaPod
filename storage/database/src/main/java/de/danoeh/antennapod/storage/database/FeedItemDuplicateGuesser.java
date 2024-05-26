package de.danoeh.antennapod.storage.database;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

import java.text.DateFormat;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

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
                && mimeTypeLooksSimilar(media1, media2);
    }

    public static boolean sameAndNotEmpty(String string1, String string2) {
        if (StringUtils.isEmpty(string1) || StringUtils.isEmpty(string2)) {
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
        return StringUtils.equals(dateOriginal, dateNew); // Same date; time is ignored.
    }

    private static boolean durationsLookSimilar(FeedMedia media1, FeedMedia media2) {
        return Math.abs(media1.getDuration() - media2.getDuration()) < 10 * 60L * 1000L;
    }

    private static boolean mimeTypeLooksSimilar(FeedMedia media1, FeedMedia media2) {
        String mimeType1 = media1.getMimeType();
        String mimeType2 = media2.getMimeType();
        if (mimeType1 == null || mimeType2 == null) {
            return true;
        }
        if (mimeType1.contains("/") && mimeType2.contains("/")) {
            mimeType1 = mimeType1.substring(0, mimeType1.indexOf("/"));
            mimeType2 = mimeType2.substring(0, mimeType2.indexOf("/"));
        }
        return StringUtils.equals(mimeType1, mimeType2);
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
