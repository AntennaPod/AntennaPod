package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItemFilter;
import de.danoeh.antennapod.core.feed.FeedItemFilterGroup;
import de.danoeh.antennapod.view.RecursiveRadioGroup;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.filter_dialog, null, false);
        builder.setView(layout);

        for (FeedItemFilterGroup item : FeedItemFilterGroup.values()) {
            RecursiveRadioGroup row = (RecursiveRadioGroup) inflater.inflate(R.layout.filter_dialog_row, null);
            RadioButton filter1 = row.findViewById(R.id.filter_dialog_radioButton1);
            RadioButton filter2 = row.findViewById(R.id.filter_dialog_radioButton2);
            filter1.setText(item.values[0].displayName);
            filter1.setTag(item.values[0].filterId);
            filter2.setText(item.values[1].displayName);
            filter2.setTag(item.values[1].filterId);
            layout.addView(row);
        }

        for (String filterId : filterValues) {
            if (!TextUtils.isEmpty(filterId)) {
                ((RadioButton) layout.findViewWithTag(filterId)).setChecked(true);
            }
        }

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            filterValues.clear();
            for (int i = 0; i < layout.getChildCount(); i++) {
                if (!(layout.getChildAt(i) instanceof RecursiveRadioGroup)) {
                    continue;
                }
                RecursiveRadioGroup group = (RecursiveRadioGroup) layout.getChildAt(i);
                if (group.getCheckedButton() != null) {
                    filterValues.add((String) group.getCheckedButton().getTag());
                }
            }
            updateFilter(filterValues);
        });

        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    protected abstract void updateFilter(Set<String> filterValues);
}
