package de.danoeh.antennapod.ui.screen.feed;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.FilterDialogBinding;
import de.danoeh.antennapod.databinding.FilterDialogRowBinding;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public abstract class ItemFilterDialog extends BottomSheetDialogFragment {
    protected static final String ARGUMENT_FILTER = "filter";

    private LinearLayout rows;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.filter_dialog, null, false);
        FilterDialogBinding binding = FilterDialogBinding.bind(layout);
        rows = binding.filterRows;
        FeedItemFilter filter = (FeedItemFilter) getArguments().getSerializable(ARGUMENT_FILTER);

        //add filter rows
        for (FeedItemFilterGroup item : FeedItemFilterGroup.values()) {
            FilterDialogRowBinding rowBinding = FilterDialogRowBinding.inflate(inflater);
            rowBinding.getRoot().addOnButtonCheckedListener(
                    (group, checkedId, isChecked) -> onFilterChanged(getNewFilterValues()));
            rowBinding.filterButton1.setText(item.values[0].displayName);
            rowBinding.filterButton1.setTag(item.values[0].filterId);
            rowBinding.filterButton2.setText(item.values[1].displayName);
            rowBinding.filterButton2.setTag(item.values[1].filterId);
            rowBinding.filterButton1.setMaxLines(3);
            rowBinding.filterButton1.setSingleLine(false);
            rowBinding.filterButton2.setMaxLines(3);
            rowBinding.filterButton2.setSingleLine(false);
            rows.addView(rowBinding.getRoot(), rows.getChildCount() - 1);
        }

        binding.confirmFiltermenu.setOnClickListener(view1 -> dismiss());
        binding.resetFiltermenu.setOnClickListener(view1 -> {
            onFilterChanged(Collections.emptySet());
            for (int i = 0; i < rows.getChildCount(); i++) {
                if (rows.getChildAt(i) instanceof MaterialButtonToggleGroup) {
                    ((MaterialButtonToggleGroup) rows.getChildAt(i)).clearChecked();
                }
            }
        });

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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            setupFullHeight(bottomSheetDialog);
        });
        return dialog;
    }

    private void setupFullHeight(BottomSheetDialog bottomSheetDialog) {
        FrameLayout bottomSheet = (FrameLayout) bottomSheetDialog.findViewById(R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            bottomSheet.setLayoutParams(layoutParams);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
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

    public abstract void onFilterChanged(Set<String> newFilterValues);
}
