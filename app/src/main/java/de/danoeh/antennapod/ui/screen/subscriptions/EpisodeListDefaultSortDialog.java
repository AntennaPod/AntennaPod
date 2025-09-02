package de.danoeh.antennapod.ui.screen.subscriptions;

import android.os.Bundle;
import androidx.annotation.Nullable;

import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.screen.feed.ItemSortDialog;

public class EpisodeListDefaultSortDialog extends ItemSortDialog {
    private static final String ARG_SORT_ORDER = "sortOrder";

    public static EpisodeListDefaultSortDialog newInstance() {
        Bundle bundle = new Bundle();
        SortOrder currentSortOrder = UserPreferences.getPrefDefaultSortedOrder();
        if (currentSortOrder == null) {
            bundle.putString(ARG_SORT_ORDER, String.valueOf(SortOrder.DATE_NEW_OLD.code));
        } else {
            bundle.putString(ARG_SORT_ORDER, String.valueOf(currentSortOrder.code));
        }
        EpisodeListDefaultSortDialog dialog = new EpisodeListDefaultSortDialog();
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
                || ascending == SortOrder.EPISODE_TITLE_A_Z) {
            super.onAddItem(title, ascending, descending, ascendingIsDefault);
        }
    }

    @Override
    protected void onSelectionChanged() {
        super.onSelectionChanged();
        UserPreferences.setPrefDefaultSortedOrder(sortOrder);
    }
}
