package de.danoeh.antennapod.storage.database.mapper;

import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

public class FeedItemSortQuery {
    public static String generateFrom(SortOrder sortOrder) {
        String sortQuery = "";
        switch (sortOrder) {
            case EPISODE_TITLE_A_Z:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_TITLE + " " + "ASC";
                break;
            case EPISODE_TITLE_Z_A:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_TITLE + " " + "DESC";
                break;
            case DATE_OLD_NEW:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_PUBDATE + " " + "ASC";
                break;
            case DATE_NEW_OLD:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_PUBDATE + " " + "DESC";
                break;
            case DURATION_SHORT_LONG:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_DURATION + " " + "ASC";
                break;
            case DURATION_LONG_SHORT:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_DURATION + " " + "DESC";
                break;
            case SIZE_SMALL_LARGE:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_SIZE + " " + "ASC";
                break;
            case SIZE_LARGE_SMALL:
                sortQuery = PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_SIZE + " " + "DESC";
                break;
            default:
                sortQuery = "";
                break;
        }
        return sortQuery;
    }
}