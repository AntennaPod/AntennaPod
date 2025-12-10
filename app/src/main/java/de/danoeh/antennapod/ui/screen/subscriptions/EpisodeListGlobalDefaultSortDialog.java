package de.danoeh.antennapod.ui.screen.subscriptions;

import android.os.Bundle;
import androidx.annotation.Nullable;

import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.screen.feed.ItemSortDialog;

public class EpisodeListGlobalDefaultSortDialog extends ItemSortDialog {
    public static EpisodeListGlobalDefaultSortDialog newInstance() {
        Bundle bundle = new Bundle();
        EpisodeListGlobalDefaultSortDialog dialog = new EpisodeListGlobalDefaultSortDialog();
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sortOrder = SortOrder.fromCodeString(String.valueOf(UserPreferences.getPrefGlobalSortedOrder().code));
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
        UserPreferences.setPrefGlobalSortedOrder(sortOrder);
    }
}
