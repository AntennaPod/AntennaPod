package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.SubscriptionsFilterGroup;
import de.danoeh.antennapod.databinding.FilterDialogBinding;
import de.danoeh.antennapod.databinding.FilterDialogRowBinding;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.SubscriptionsFilter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class SubscriptionsFilterDialog {
    public static void showDialog(Context context) {
        SubscriptionsFilter subscriptionsFilter = UserPreferences.getSubscriptionsFilter();
        final Set<String> filterValues = new HashSet<>(Arrays.asList(subscriptionsFilter.getValues()));
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(context.getString(R.string.pref_filter_feed_title));

        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.filter_dialog, null, false);
        FilterDialogBinding dialogBinding = FilterDialogBinding.bind(layout);
        LinearLayout rows = dialogBinding.filterRows;
        builder.setView(layout);

        for (SubscriptionsFilterGroup item : SubscriptionsFilterGroup.values()) {
            FilterDialogRowBinding binding = FilterDialogRowBinding.inflate(inflater);
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

        for (String filterId : filterValues) {
            if (!TextUtils.isEmpty(filterId)) {
                Button button = layout.findViewWithTag(filterId);
                if (button != null) {
                    ((MaterialButtonToggleGroup) button.getParent()).check(button.getId());
                }
            }
        }

        AlertDialog dialog = builder.create();

        dialogBinding.confirmFiltermenu.setOnClickListener(view -> {
            filterValues.clear();
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
            updateFilter(filterValues);
            dialog.dismiss();
        });
        dialogBinding.resetFiltermenu.setOnClickListener(view -> {
            updateFilter(Collections.emptySet());
            for (int i = 0; i < rows.getChildCount(); i++) {
                if (rows.getChildAt(i) instanceof MaterialButtonToggleGroup) {
                    ((MaterialButtonToggleGroup) rows.getChildAt(i)).clearChecked();
                }
            }
        });
        dialog.show();
    }

    private static void updateFilter(Set<String> filterValues) {
        SubscriptionsFilter subscriptionsFilter = new SubscriptionsFilter(filterValues.toArray(new String[0]));
        UserPreferences.setSubscriptionsFilter(subscriptionsFilter);
        EventBus.getDefault().post(new UnreadItemsUpdateEvent());
    }
}
