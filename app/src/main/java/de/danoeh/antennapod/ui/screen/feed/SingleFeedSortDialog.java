package de.danoeh.antennapod.ui.screen.feed;

import android.os.Bundle;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.DBWriter;

public class SingleFeedSortDialog extends ItemSortDialog {
    private static final String ARG_FEED_ID = "feedId";
    private static final String ARG_FEED_IS_LOCAL = "isLocal";
    private static final String ARG_SORT_ORDER = "sortOrder";

    public static SingleFeedSortDialog newInstance(Feed feed) {
        Bundle bundle = new Bundle();
        bundle.putLong(ARG_FEED_ID, feed.getId());
        bundle.putBoolean(ARG_FEED_IS_LOCAL, feed.isLocalFeed());
        if (feed.getSortOrder() == null) {
            bundle.putString(ARG_SORT_ORDER, String.valueOf(SortOrder.DATE_NEW_OLD.code));
        } else {
            bundle.putString(ARG_SORT_ORDER, String.valueOf(feed.getSortOrder().code));
        }
        SingleFeedSortDialog dialog = new SingleFeedSortDialog();
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sortOrder = SortOrder.fromCodeString(getArguments().getString(ARG_SORT_ORDER));
    }

    @Override
    protected void onAddItem(int title, SortOrder ascending, SortOrder descending, boolean ascendingIsDefault) {
        if (ascending == SortOrder.DATE_OLD_NEW || ascending == SortOrder.DURATION_SHORT_LONG
                || ascending == SortOrder.EPISODE_TITLE_A_Z
                || (getArguments().getBoolean(ARG_FEED_IS_LOCAL) && ascending == SortOrder.EPISODE_FILENAME_A_Z)) {
            super.onAddItem(title, ascending, descending, ascendingIsDefault);
        }
    }

    @Override
    protected void onSelectionChanged() {
        super.onSelectionChanged();
        DBWriter.setFeedItemSortOrder(getArguments().getLong(ARG_FEED_ID), sortOrder);
    }
}
