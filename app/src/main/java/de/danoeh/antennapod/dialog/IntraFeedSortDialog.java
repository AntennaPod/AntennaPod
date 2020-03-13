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

    public IntraFeedSortDialog(@NonNull Context context, @Nullable SortOrder sortOrder) {
        this.context = context;
        this.currentSortOrder = sortOrder;
    }

    public void openDialog() {
        final String[] items = context.getResources().getStringArray(R.array.feed_episodes_sort_options);
        final String[] valueStrs = context.getResources().getStringArray(R.array.feed_episodes_sort_values);
        final SortOrder[] values = new SortOrder[valueStrs.length];
        for (int i = 0; i < valueStrs.length; i++) {
            values[i] = SortOrder.valueOf(valueStrs[i]);
        }

        int idxCurrentSort = 0;
        for  (int i = 0; i < values.length; i++) {
            if (currentSortOrder == values[i]) {
                idxCurrentSort = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.sort);
        builder.setSingleChoiceItems(items, idxCurrentSort, (dialog, idxNewSort) -> {
            updateSort(values[idxNewSort]);
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    protected abstract void updateSort(@NonNull SortOrder sortOrder);
}
