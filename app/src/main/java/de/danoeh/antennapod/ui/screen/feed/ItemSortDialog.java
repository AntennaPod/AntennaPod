package de.danoeh.antennapod.ui.screen.feed;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.SortDialogBinding;
import de.danoeh.antennapod.databinding.SortDialogItemActiveBinding;
import de.danoeh.antennapod.databinding.SortDialogItemBinding;
import de.danoeh.antennapod.model.feed.SortOrder;

public class ItemSortDialog extends BottomSheetDialogFragment {
    protected SortOrder sortOrder;
    protected SortDialogBinding viewBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewBinding = SortDialogBinding.inflate(inflater);
        populateList();
        viewBinding.keepSortedCheckbox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> ItemSortDialog.this.onSelectionChanged());
        return viewBinding.getRoot();
    }

    private void populateList() {
        viewBinding.gridLayout.removeAllViews();
        onAddItem(R.string.episode_title, SortOrder.EPISODE_TITLE_A_Z, SortOrder.EPISODE_TITLE_Z_A, true);
        onAddItem(R.string.feed_title, SortOrder.FEED_TITLE_A_Z, SortOrder.FEED_TITLE_Z_A, true);
        onAddItem(R.string.duration, SortOrder.DURATION_SHORT_LONG, SortOrder.DURATION_LONG_SHORT, true);
        onAddItem(R.string.date, SortOrder.DATE_OLD_NEW, SortOrder.DATE_NEW_OLD, false);
        onAddItem(R.string.size, SortOrder.SIZE_SMALL_LARGE, SortOrder.SIZE_LARGE_SMALL, false);
        onAddItem(R.string.filename, SortOrder.EPISODE_FILENAME_A_Z, SortOrder.EPISODE_FILENAME_Z_A, true);
        onAddItem(R.string.random, SortOrder.RANDOM, SortOrder.RANDOM, true);
        onAddItem(R.string.smart_shuffle, SortOrder.SMART_SHUFFLE_OLD_NEW, SortOrder.SMART_SHUFFLE_NEW_OLD, false);
    }

    protected void onAddItem(int title, SortOrder ascending, SortOrder descending, boolean ascendingIsDefault) {
        if (sortOrder == ascending || sortOrder == descending) {
            SortDialogItemActiveBinding item = SortDialogItemActiveBinding.inflate(
                    getLayoutInflater(), viewBinding.gridLayout, false);
            SortOrder other;
            if (ascending == descending) {
                item.button.setText(title);
                other = ascending;
            } else if (sortOrder == ascending) {
                item.button.setText(getString(title) + "\u00A0▲");
                other = descending;
            } else {
                item.button.setText(getString(title) + "\u00A0▼");
                other = ascending;
            }
            item.button.setOnClickListener(v -> {
                sortOrder = other;
                populateList();
                onSelectionChanged();
            });
            viewBinding.gridLayout.addView(item.getRoot());
        } else {
            SortDialogItemBinding item = SortDialogItemBinding.inflate(
                    getLayoutInflater(), viewBinding.gridLayout, false);
            item.button.setText(title);
            item.button.setOnClickListener(v -> {
                sortOrder = ascendingIsDefault ? ascending : descending;
                populateList();
                onSelectionChanged();
            });
            viewBinding.gridLayout.addView(item.getRoot());
        }
    }

    protected void onSelectionChanged() {
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
}
