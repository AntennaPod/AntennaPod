package de.danoeh.antennapod.core.util;

import android.content.Context;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBWriter;

import java.util.Comparator;

/**
 * Provides method for sorting the queue according to rules.
 */
public class QueueSorter {
    public enum Rule {
        ALPHA_ASC,
        ALPHA_DESC,
        DATE_ASC,
        DATE_DESC,
        DURATION_ASC,
        DURATION_DESC
    }

    public static void sort(final Context context, final Rule rule, final boolean broadcastUpdate) {
        Comparator<FeedItem> comparator = null;

        switch (rule) {
            case ALPHA_ASC:
                comparator = new Comparator<FeedItem>() {
                    public int compare(FeedItem f1, FeedItem f2) {
                        return f1.getTitle().compareTo(f2.getTitle());
                    }
                };
                break;
            case ALPHA_DESC:
                comparator = new Comparator<FeedItem>() {
                    public int compare(FeedItem f1, FeedItem f2) {
                        return f2.getTitle().compareTo(f1.getTitle());
                    }
                };
                break;
            case DATE_ASC:
                comparator = new Comparator<FeedItem>() {
                    public int compare(FeedItem f1, FeedItem f2) {
                        return f1.getPubDate().compareTo(f2.getPubDate());
                    }
                };
                break;
            case DATE_DESC:
                comparator = new Comparator<FeedItem>() {
                    public int compare(FeedItem f1, FeedItem f2) {
                        return f2.getPubDate().compareTo(f1.getPubDate());
                    }
                };
                break;
            case DURATION_ASC:
                comparator = new Comparator<FeedItem>() {
                    public int compare(FeedItem f1, FeedItem f2) {
                        FeedMedia f1Media = f1.getMedia();
                        FeedMedia f2Media = f2.getMedia();
                        int duration1 = f1Media != null ? f1Media.getDuration() : -1;
                        int duration2 = f2Media != null ? f2Media.getDuration() : -1;

                        if (duration1 == -1 || duration2 == -1)
                            return duration2 - duration1;
                        else
                            return duration1 - duration2;
                    }
                };
                break;
            case DURATION_DESC:
                comparator = new Comparator<FeedItem>() {
                public int compare(FeedItem f1, FeedItem f2) {
                    FeedMedia f1Media = f1.getMedia();
                    FeedMedia f2Media = f2.getMedia();
                    int duration1 = f1Media != null ? f1Media.getDuration() : -1;
                    int duration2 = f2Media != null ? f2Media.getDuration() : -1;

                    return -1 * (duration1 - duration2);
                }
            };
            default:
        }

        if (comparator != null) {
            DBWriter.sortQueue(context, comparator, broadcastUpdate);
        }
    }
}
