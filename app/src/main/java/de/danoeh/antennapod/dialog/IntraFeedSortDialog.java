package de.danoeh.antennapod.dialog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.IntraFeedSortOrder;

public abstract class IntraFeedSortDialog {

    @Nullable
    protected IntraFeedSortOrder currentSortOrder;
    @NonNull
    protected Context context;

    public IntraFeedSortDialog(@NonNull Context context, @Nullable IntraFeedSortOrder sortOrder) {
        this.context = context;
        this.currentSortOrder = sortOrder;
    }

    public void openDialog() {
        final String[] items = context.getResources().getStringArray(R.array.feed_episodes_sort_options);
        final String[] valueStrs = context.getResources().getStringArray(R.array.feed_episodes_sort_values);
        final IntraFeedSortOrder[] values = new IntraFeedSortOrder[valueStrs.length];
        for (int i = 0; i < valueStrs.length; i++) {
            values[i] = IntraFeedSortOrder.valueOf(valueStrs[i]);
        }

        int idxCurrentSort = -1;
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

    protected abstract void updateSort(@NonNull IntraFeedSortOrder sortOrder);
}
