package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItemFilter;

public abstract class FilterDialog {

    protected FeedItemFilter filter;
    protected Context context;

    public FilterDialog(Context context, FeedItemFilter feedItemFilter) {
        this.context = context;
        this.filter = feedItemFilter;
    }

    public void openDialog() {
        final String[] items = context.getResources().getStringArray(R.array.episode_filter_options);
        final String[] values = context.getResources().getStringArray(R.array.episode_filter_values);
        final boolean[] checkedItems = new boolean[items.length];

        final Set<String> filterValues = new HashSet<>(Arrays.asList(filter.getValues()));

        // make sure we have no empty strings in the filter list
        for (String filterValue : filterValues) {
            if (TextUtils.isEmpty(filterValue)) {
                filterValues.remove(filterValue);
            }
        }

        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (filterValues.contains(value)) {
                checkedItems[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.filter);
        builder.setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
            if (isChecked) {
                filterValues.add(values[which]);
            } else {
                filterValues.remove(values[which]);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            updateFilter(filterValues);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    protected abstract void updateFilter(Set<String> filterValues);
}
