package de.danoeh.antennapod.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItemFilterGroup;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.ui.common.RecursiveRadioGroup;

import java.util.HashSet;
import java.util.Set;

public abstract class ItemFilterDialog extends BottomSheetDialogFragment {
    protected static final String ARGUMENT_FILTER = "filter";

    private LinearLayout rows;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.filter_dialog, null, false);
        rows = layout.findViewById(R.id.filter_rows);
        FeedItemFilter filter = (FeedItemFilter) getArguments().getSerializable(ARGUMENT_FILTER);

        for (FeedItemFilterGroup item : FeedItemFilterGroup.values()) {
            RecursiveRadioGroup row = (RecursiveRadioGroup) inflater.inflate(R.layout.filter_dialog_row, null, false);
            row.setOnCheckedChangeListener((group, checkedId) -> onFilterChanged(getNewFilterValues()));
            RadioButton filter1 = row.findViewById(R.id.filter_dialog_radioButton1);
            RadioButton filter2 = row.findViewById(R.id.filter_dialog_radioButton2);
            filter1.setText(item.values[0].displayName);
            filter1.setTag(item.values[0].filterId);
            filter2.setText(item.values[1].displayName);
            filter2.setTag(item.values[1].filterId);
            rows.addView(row);
        }

        for (String filterId : filter.getValues()) {
            if (!TextUtils.isEmpty(filterId)) {
                RadioButton button = layout.findViewWithTag(filterId);
                if (button != null) {
                    button.setChecked(true);
                }
            }
        }
        return layout;
    }

    protected Set<String> getNewFilterValues() {
        final Set<String> newFilterValues = new HashSet<>();
        for (int i = 0; i < rows.getChildCount(); i++) {
            if (!(rows.getChildAt(i) instanceof RecursiveRadioGroup)) {
                continue;
            }
            RecursiveRadioGroup group = (RecursiveRadioGroup) rows.getChildAt(i);
            if (group.getCheckedButton() != null) {
                String tag = (String) group.getCheckedButton().getTag();
                if (tag != null) { // Clear buttons use no tag
                    newFilterValues.add((String) group.getCheckedButton().getTag());
                }
            }
        }
        return newFilterValues;
    }

    abstract void onFilterChanged(Set<String> newFilterValues);
}
