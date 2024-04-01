package de.danoeh.antennapod.ui.home.settingsdialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.ChooseHomeScreenOrderDialogBinding;

import java.util.List;

public class HomeSectionsSettingsDialog {
    public static void open(Context context, DialogInterface.OnClickListener onSettingsChanged) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        ChooseHomeScreenOrderDialogBinding viewBinding = ChooseHomeScreenOrderDialogBinding.inflate(layoutInflater);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.configure_home);
        builder.setView(viewBinding.getRoot());
        RecyclerView recyclerView = viewBinding.recyclerView;
        HomeScreenSettingDialogAdapter adapter = new HomeScreenSettingDialogAdapter(context);

        configureRecyclerView(recyclerView, adapter, context);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            final List<String> sectionOrder = adapter.getOrderedSectionTags();
            final List<String> hiddenSections = adapter.getHiddenSectionTags();
            HomePreferences.saveChanges(context, hiddenSections, sectionOrder);
            onSettingsChanged.onClick(dialog, which);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    private static void configureRecyclerView(
            RecyclerView recyclerView,
            HomeScreenSettingDialogAdapter adapter,
            Context context) {
        ItemTouchCallback itemMoveCallback = new ItemTouchCallback(adapter::onItemMove);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemMoveCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.setDragListener(itemTouchHelper::startDrag);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
    }
}
