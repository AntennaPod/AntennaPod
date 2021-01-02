package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItemFilter;
import de.danoeh.antennapod.core.feed.FeedItemFilterGroup;
import de.danoeh.antennapod.view.RecursiveRadioGroup;

public abstract class FilterDialog {

    protected FeedItemFilter filter;
    protected Context context;

    public FilterDialog(Context context, FeedItemFilter feedItemFilter) {
        this.context = context;
        this.filter = feedItemFilter;
    }

    public void openDialog() {

        final Set<String> filterValues = new HashSet<>(Arrays.asList(filter.getValues()));
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.filter);

        LayoutInflater inflater = LayoutInflater.from(this.context);
        View layout = inflater.inflate(R.layout.filter_dialog, null, false);
        LinearLayout rows = layout.findViewById(R.id.filter_rows);
        builder.setView(layout);

        for (FeedItemFilterGroup item : FeedItemFilterGroup.values()) {
            RecursiveRadioGroup row = (RecursiveRadioGroup) inflater.inflate(R.layout.filter_dialog_row, null, false);
            RadioButton filter1 = row.findViewById(R.id.filter_dialog_radioButton1);
            RadioButton filter2 = row.findViewById(R.id.filter_dialog_radioButton2);
            filter1.setText(item.values[0].displayName);
            filter1.setTag(item.values[0].filterId);
            filter2.setText(item.values[1].displayName);
            filter2.setTag(item.values[1].filterId);
            rows.addView(row);
        }

        for (String filterId : filterValues) {
            if (!TextUtils.isEmpty(filterId)) {
                ((RadioButton) layout.findViewWithTag(filterId)).setChecked(true);
            }
        }

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            filterValues.clear();
            for (int i = 0; i < rows.getChildCount(); i++) {
                if (!(rows.getChildAt(i) instanceof RecursiveRadioGroup)) {
                    continue;
                }
                RecursiveRadioGroup group = (RecursiveRadioGroup) rows.getChildAt(i);
                if (group.getCheckedButton() != null) {
                    String tag = (String) group.getCheckedButton().getTag();
                    if (tag != null) { // Clear buttons use no tag
                        filterValues.add((String) group.getCheckedButton().getTag());
                    }
                }
            }
            updateFilter(filterValues);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    protected abstract void updateFilter(Set<String> filterValues);
}
