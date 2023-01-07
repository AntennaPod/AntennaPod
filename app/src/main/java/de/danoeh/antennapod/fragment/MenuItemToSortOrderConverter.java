package de.danoeh.antennapod.fragment;

import android.view.MenuItem;

import java.util.HashMap;
import java.util.Map;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.SortOrder;

public class MenuItemToSortOrderConverter {

    private static final Map<Integer, SortOrder> ID_TO_SORT_ORDER = new HashMap<>();

    static {
        ID_TO_SORT_ORDER.put(R.id.sort_date_asc, SortOrder.DATE_OLD_NEW);
        ID_TO_SORT_ORDER.put(R.id.sort_date_desc, SortOrder.DATE_NEW_OLD);
        ID_TO_SORT_ORDER.put(R.id.sort_duration_asc, SortOrder.DURATION_SHORT_LONG);
        ID_TO_SORT_ORDER.put(R.id.sort_duration_desc, SortOrder.DURATION_LONG_SHORT);
        ID_TO_SORT_ORDER.put(R.id.sort_episode_title_asc, SortOrder.EPISODE_TITLE_A_Z);
        ID_TO_SORT_ORDER.put(R.id.sort_episode_title_desc, SortOrder.EPISODE_TITLE_Z_A);
        ID_TO_SORT_ORDER.put(R.id.sort_feed_title_asc, SortOrder.FEED_TITLE_A_Z);
        ID_TO_SORT_ORDER.put(R.id.sort_feed_title_desc, SortOrder.FEED_TITLE_Z_A);
        ID_TO_SORT_ORDER.put(R.id.sort_smart_shuffle_asc, SortOrder.SMART_SHUFFLE_OLD_NEW);
        ID_TO_SORT_ORDER.put(R.id.sort_smart_shuffle_desc, SortOrder.SMART_SHUFFLE_NEW_OLD);
        ID_TO_SORT_ORDER.put(R.id.sort_random, SortOrder.RANDOM);
    }

    public static SortOrder convert(MenuItem item) {
        return ID_TO_SORT_ORDER.get(item.getItemId());
    }

}
