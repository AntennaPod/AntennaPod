package de.danoeh.antennapod.storage.database.mapper;

import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.PodDBAdapter;

public class FeedItemSortQuery {
    public static String generateFrom(SortOrder sortOrder) {
        if (sortOrder == null) {
            sortOrder = SortOrder.DATE_NEW_OLD;
        }
        switch (sortOrder) {
            case EPISODE_TITLE_A_Z:
                return PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_TITLE + " " + "ASC";
            case EPISODE_TITLE_Z_A:
                return PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_TITLE + " " + "DESC";
            case DURATION_SHORT_LONG:
                return PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_DURATION + " " + "ASC";
            case DURATION_LONG_SHORT:
                return PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_DURATION + " " + "DESC";
            case SIZE_SMALL_LARGE:
                return PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_SIZE + " " + "ASC";
            case SIZE_LARGE_SMALL:
                return PodDBAdapter.TABLE_NAME_FEED_MEDIA + "." + PodDBAdapter.KEY_SIZE + " " + "DESC";
            case COMPLETION_DATE_NEW_OLD:
                return PodDBAdapter.TABLE_NAME_FEED_MEDIA + "."
                        + PodDBAdapter.KEY_PLAYBACK_COMPLETION_DATE + " " + "DESC";
            case DATE_OLD_NEW:
                return PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_PUBDATE + " " + "ASC";
            case EPISODE_FILENAME_A_Z:
                return PodDBAdapter.KEY_LINK + " " + "ASC";
            case EPISODE_FILENAME_Z_A:
                return PodDBAdapter.KEY_LINK + " " + "DESC";
            case DATE_NEW_OLD:
            default:
                return PodDBAdapter.TABLE_NAME_FEED_ITEMS + "." + PodDBAdapter.KEY_PUBDATE + " " + "DESC";
        }
    }
}