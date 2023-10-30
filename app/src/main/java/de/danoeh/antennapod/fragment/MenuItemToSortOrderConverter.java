package de.danoeh.antennapod.fragment;

import android.view.MenuItem;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.SortOrder;

public class MenuItemToSortOrderConverter {

    public static SortOrder convert(MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.sort_episode_title_asc) {
            return SortOrder.EPISODE_TITLE_A_Z;
        } else if (itemId == R.id.sort_episode_title_desc) {
            return SortOrder.EPISODE_TITLE_Z_A;
        } else if (itemId == R.id.sort_date_asc) {
            return SortOrder.DATE_OLD_NEW;
        } else if (itemId == R.id.sort_date_desc) {
            return SortOrder.DATE_NEW_OLD;
        } else if (itemId == R.id.sort_duration_asc) {
            return SortOrder.DURATION_SHORT_LONG;
        } else if (itemId == R.id.sort_duration_desc) {
            return SortOrder.DURATION_LONG_SHORT;
        } else if (itemId == R.id.sort_feed_title_asc) {
            return SortOrder.FEED_TITLE_A_Z;
        } else if (itemId == R.id.sort_feed_title_desc) {
            return SortOrder.FEED_TITLE_Z_A;
        } else if (itemId == R.id.sort_random) {
            return SortOrder.RANDOM;
        } else if (itemId == R.id.sort_smart_shuffle_asc) {
            return SortOrder.SMART_SHUFFLE_OLD_NEW;
        } else if (itemId == R.id.sort_smart_shuffle_desc) {
            return SortOrder.SMART_SHUFFLE_NEW_OLD;
        } else if (itemId == R.id.sort_size_small_large) {
            return SortOrder.SIZE_SMALL_LARGE;
        } else if (itemId == R.id.sort_size_large_small) {
            return SortOrder.SIZE_LARGE_SMALL;
        }

        return null;
    }

}
