package de.danoeh.antennapod.storage.database.mapper;

import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

public class FeedItemSortQuery {
    public static String generateFrom(SortOrder sortOrder) {
        String sortQuery = "";
        switch (sortOrder) {
            case DATE_OLD_NEW:
                sortQuery = PodDBAdapter.KEY_PUBDATE + " " + "ASC";
                break;
            case DATE_NEW_OLD:
                sortQuery = PodDBAdapter.KEY_PUBDATE + " " + "DESC";
                break;
            default:
                sortQuery = "";
                break;
        }
        return sortQuery;
    }
}
