package de.danoeh.antennapod.dialog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.SortOrder;

public abstract class IntraFeedSortDialog {

    @Nullable
    protected SortOrder currentSortOrder;
    @NonNull
    protected Context context;

    private final String[] sortItems;
    private final SortOrder[] sortValues;

    public IntraFeedSortDialog(@NonNull Context context, @Nullable SortOrder sortOrder, @NonNull boolean isLocalFeed) {
        this.context = context;
        this.currentSortOrder = sortOrder;

        if (isLocalFeed) {
            sortItems = context.getResources().getStringArray(R.array.local_feed_episodes_sort_options);
            final String[] localSortStringValues =
                    context.getResources().getStringArray(R.array.local_feed_episodes_sort_values);
            sortValues = SortOrder.valuesOf(localSortStringValues);
        } else {
            sortItems = context.getResources().getStringArray(R.array.feed_episodes_sort_options);
            final String[] commonSortStringValues =
                    context.getResources().getStringArray(R.array.feed_episodes_sort_values);
            sortValues = SortOrder.valuesOf(commonSortStringValues);
        }
    }

    public void openDialog() {
        int idxCurrentSort = getCurrentSortOrderIndex();

        AlertDialog.Builder builder =
                new AlertDialog.Builder(context)
                        .setTitle(R.string.sort)
                        .setSingleChoiceItems(sortItems, idxCurrentSort, (dialog, idxNewSort) -> {
                            updateSort(sortValues[idxNewSort]);
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    /**
     * Retrieves index of currentSortOrder index in values array.
     * @return if currentSortOrder is found in array - returns index of that element,
     *         otherwise returns 0, the default sort option;
     */
    private int getCurrentSortOrderIndex() {
        for (int i = 0; i < sortValues.length; i++) {
            if (currentSortOrder == sortValues[i]) {
                return i;
            }
        }
        return 0;
    }

    protected abstract void updateSort(@NonNull SortOrder sortOrder);
}
