package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItemFilterGroup;
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
        rows = layout.findViewById(R.id.filter_rows);
        FeedItemFilter filter = (FeedItemFilter) getArguments().getSerializable(ARGUMENT_FILTER);

        //add filter rows
        for (FeedItemFilterGroup item : FeedItemFilterGroup.values()) {
            FilterDialogRowBinding binding = FilterDialogRowBinding.inflate(inflater);
            binding.getRoot().addOnButtonCheckedListener(
                    (group, checkedId, isChecked) -> onFilterChanged(getNewFilterValues()));
            binding.filterButton1.setText(item.values[0].displayName);
            binding.filterButton1.setTag(item.values[0].filterId);
            binding.filterButton2.setText(item.values[1].displayName);
            binding.filterButton2.setTag(item.values[1].filterId);
            binding.filterButton1.setMaxLines(3);
            binding.filterButton1.setSingleLine(false);
            binding.filterButton2.setMaxLines(3);
            binding.filterButton2.setSingleLine(false);
            rows.addView(binding.getRoot());
        }

        //add confirm and reset button
        MaterialButton confirmButton = new MaterialButton(getContext());
        confirmButton.setText(getString(R.string.confirm_label));
        confirmButton.setOnClickListener(view1 -> {
            dismiss();
        });
        MaterialButton resetButton = new MaterialButton(getContext());
        resetButton.setText(getString(R.string.reset));
        resetButton.setOnClickListener(view1 -> {
            onFilterChanged(Collections.emptySet());
            for (int i = 0; i < rows.getChildCount(); i++) {
                if (rows.getChildAt(i) instanceof MaterialButtonToggleGroup) {
                    ((MaterialButtonToggleGroup) rows.getChildAt(i)).clearChecked();
                }
            }
            dismiss();
        });
        float eightDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                getResources().getDisplayMetrics());
        LinearLayout.LayoutParams matchParamsMarginEnd = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        matchParamsMarginEnd.setMarginEnd(Math.round(eightDp));
        LinearLayout.LayoutParams matchParamsMarginStart = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        matchParamsMarginStart.setMarginStart(Math.round(eightDp));
        resetButton.setLayoutParams(matchParamsMarginEnd);
        confirmButton.setLayoutParams(matchParamsMarginStart);
        LinearLayout horizontalLinearLayout = new LinearLayout(getContext());
        horizontalLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        horizontalLinearLayout.addView(resetButton);
        horizontalLinearLayout.addView(confirmButton);
        rows.addView(horizontalLinearLayout);

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

    abstract void onFilterChanged(Set<String> newFilterValues);
}
