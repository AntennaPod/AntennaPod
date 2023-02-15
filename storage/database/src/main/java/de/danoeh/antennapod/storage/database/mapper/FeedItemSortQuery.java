package de.danoeh.antennapod.storage.database.mapper;

import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

public class FeedItemSortQuery {
    public static final String ASCENDING = "ASC";
    public static final String DESCENDING = "DESC";

    public static String generateFrom(SortOrder sortOrder) {
        String sortQuery = "";
        switch (sortOrder) {
            case EPISODE_TITLE_A_Z:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_TITLE + " " + ASCENDING;
                break;
            case EPISODE_TITLE_Z_A:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_TITLE + " " + DESCENDING;
                break;
            case DATE_OLD_NEW:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_PUBDATE + " " + ASCENDING;
                break;
            case DATE_NEW_OLD:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_PUBDATE + " " + DESCENDING;
                break;
            case DURATION_SHORT_LONG:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_DURATION + " " + ASCENDING;
                break;
            case DURATION_LONG_SHORT:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_DURATION + " " + DESCENDING;
                break;
            default:
                sortQuery = "";
                break;
        }
        return sortQuery;
    }
}