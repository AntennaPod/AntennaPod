package de.danoeh.antennapod.storage.database.mapper;

import static de.danoeh.antennapod.storage.database.PodDBAdapter.KEY_DURATION;
import static de.danoeh.antennapod.storage.database.PodDBAdapter.KEY_PUBDATE;

import de.danoeh.antennapod.model.feed.SortOrder;

public class FeedItemSortQuery {
    public static final String ASCENDING = "ASC";
    public static final String DESCENDING = "DESC";

    public static String generateFrom(SortOrder sortOrder) {
        String sortQuery = "";
        switch (sortOrder) {
            case DATE_OLD_NEW:
                sortQuery = KEY_PUBDATE + " " + ASCENDING;
                break;
            case DURATION_LONG_SHORT:
                sortQuery =  KEY_DURATION + " " + DESCENDING;
                break;
            case  DURATION_SHORT_LONG:
                sortQuery =  KEY_DURATION + " " + ASCENDING;
                break;
            default:
                sortQuery = KEY_PUBDATE + " " + DESCENDING;
                break;
        }
        return sortQuery;
    }
}

