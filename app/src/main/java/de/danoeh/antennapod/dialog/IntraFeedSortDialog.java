package de.danoeh.antennapod.dialog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.util.SortOrder;

public abstract class IntraFeedSortDialog {

    @Nullable
    protected SortOrder currentSortOrder;
    @NonNull
    protected Context context;

    private String[] localSortItems;
    private SortOrder[] localSortValues;

    private String[] commonSortItems;
    private SortOrder[] commonSortValues;

    public IntraFeedSortDialog(@NonNull Context context, @Nullable SortOrder sortOrder) {
        this.context = context;
        this.currentSortOrder = sortOrder;
        setSortOptions();
    }

    private void setSortOptions() {
        localSortItems = context.getResources().getStringArray(R.array.local_feed_episodes_sort_options);
        final String[] localSortStringValues = context.getResources().getStringArray(R.array.local_feed_episodes_sort_values);
        localSortValues = SortOrder.getSortOrderValuesFromStringValues(localSortStringValues);

        commonSortItems = context.getResources().getStringArray(R.array.feed_episodes_sort_options);
        final String[] commonSortStringValues = context.getResources().getStringArray(R.array.feed_episodes_sort_values);
        commonSortValues = SortOrder.getSortOrderValuesFromStringValues(commonSortStringValues);

    }

    public void openDialog(boolean showSortOptionsLocalFeed) {
        final SortOrder[] values = showSortOptionsLocalFeed ? localSortValues : commonSortValues;
        final String [] items = showSortOptionsLocalFeed ? localSortItems : commonSortItems;
        ;

        int idxCurrentSort = getCurrentSortOrderIndex(values);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(context)
                        .setTitle(R.string.sort)
                        .setSingleChoiceItems(items, idxCurrentSort, (dialog, idxNewSort) -> {
                            updateSort(values[idxNewSort]);
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    /**
     * Retrieves index of currentSortOrder index in values array.
     * @param values - array of available sorting options
     * @return if currentSortOrder is found in array - returns index of that element,
     *         otherwise returns -1;
     */
    private int getCurrentSortOrderIndex(SortOrder[] values) {
        int idxCurrentSort;
        for (int i = 0; i < values.length; i++) {
            if (currentSortOrder == values[i]) {
                idxCurrentSort = i;
                return idxCurrentSort;
            }
        }

        return -1;
    }

    protected abstract void updateSort(@NonNull SortOrder sortOrder);
}
