package de.danoeh.antennapod.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItemFilterGroup;
import de.danoeh.antennapod.databinding.FilterDialogRowBinding;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

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
            FilterDialogRowBinding binding = FilterDialogRowBinding.inflate(inflater);
            binding.getRoot().addOnButtonCheckedListener(
                    (group, checkedId, isChecked) -> onFilterChanged(getNewFilterValues()));
            binding.filterButton1.setText(item.values[0].displayName);
            binding.filterButton1.setTag(item.values[0].filterId);
            binding.filterButton2.setText(item.values[1].displayName);
            binding.filterButton2.setTag(item.values[1].filterId);
            rows.addView(binding.getRoot());
        }

        for (String filterId : filter.getValues()) {
            if (!TextUtils.isEmpty(filterId)) {
                Button button = layout.findViewWithTag(filterId);
                if (button != null) {
                    ((MaterialButtonToggleGroup) button.getParent()).check(button.getId());
                }
            }
        }
        return layout;
    }

    protected Set<String> getNewFilterValues() {
        final Set<String> newFilterValues = new HashSet<>();
        for (int i = 0; i < rows.getChildCount(); i++) {
            if (!(rows.getChildAt(i) instanceof MaterialButtonToggleGroup)) {
                continue;
            }
            MaterialButtonToggleGroup group = (MaterialButtonToggleGroup) rows.getChildAt(i);
            if (group.getCheckedButtonId() == View.NO_ID) {
                continue;
            }
            String tag = (String) group.findViewById(group.getCheckedButtonId()).getTag();
            if (tag == null) { // Clear buttons use no tag
                continue;
            }
            newFilterValues.add(tag);
        }
        return newFilterValues;
    }

    abstract void onFilterChanged(Set<String> newFilterValues);
}
