package de.danoeh.antennapod.ui.screen.subscriptions;

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
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.FilterDialogBinding;
import de.danoeh.antennapod.databinding.FilterDialogRowBinding;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.SubscriptionsFilter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SubscriptionsFilterDialog extends BottomSheetDialogFragment {
    private LinearLayout rows;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        SubscriptionsFilter subscriptionsFilter = UserPreferences.getSubscriptionsFilter();
        FilterDialogBinding dialogBinding = FilterDialogBinding.inflate(inflater);
        rows = dialogBinding.filterRows;

        for (SubscriptionsFilterGroup item : SubscriptionsFilterGroup.values()) {
            FilterDialogRowBinding binding = FilterDialogRowBinding.inflate(inflater);
            binding.getRoot().addOnButtonCheckedListener(
                    (group, checkedId, isChecked) -> updateFilter(getFilterValues()));
            binding.buttonGroup.setWeightSum(item.values.length);
            binding.filterButton1.setText(item.values[0].displayName);
            binding.filterButton1.setTag(item.values[0].filterId);
            if (item.values.length == 2) {
                binding.filterButton2.setText(item.values[1].displayName);
                binding.filterButton2.setTag(item.values[1].filterId);
            } else {
                binding.filterButton2.setVisibility(View.GONE);
            }
            binding.filterButton1.setMaxLines(3);
            binding.filterButton1.setSingleLine(false);
            binding.filterButton2.setMaxLines(3);
            binding.filterButton2.setSingleLine(false);
            rows.addView(binding.getRoot(), rows.getChildCount() - 1);
        }

        final Set<String> filterValues = new HashSet<>(Arrays.asList(subscriptionsFilter.getValues()));
        for (String filterId : filterValues) {
            if (!TextUtils.isEmpty(filterId)) {
                Button button = dialogBinding.getRoot().findViewWithTag(filterId);
                if (button != null) {
                    ((MaterialButtonToggleGroup) button.getParent()).check(button.getId());
                }
            }
        }

        dialogBinding.confirmFiltermenu.setOnClickListener(view -> {
            updateFilter(getFilterValues());
            dismiss();
        });
        dialogBinding.resetFiltermenu.setOnClickListener(view -> {
            updateFilter(Collections.emptySet());
            for (int i = 0; i < rows.getChildCount(); i++) {
                if (rows.getChildAt(i) instanceof MaterialButtonToggleGroup) {
                    ((MaterialButtonToggleGroup) rows.getChildAt(i)).clearChecked();
                }
            }
        });
        return dialogBinding.getRoot();
    }

    private Set<String> getFilterValues() {
        Set<String> filterValues = new HashSet<>();
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
            filterValues.add(tag);
        }
        return filterValues;
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
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            bottomSheet.setLayoutParams(layoutParams);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private static void updateFilter(Set<String> filterValues) {
        SubscriptionsFilter subscriptionsFilter = new SubscriptionsFilter(filterValues.toArray(new String[0]));
        UserPreferences.setSubscriptionsFilter(subscriptionsFilter);
        EventBus.getDefault().post(new UnreadItemsUpdateEvent());
    }
}
