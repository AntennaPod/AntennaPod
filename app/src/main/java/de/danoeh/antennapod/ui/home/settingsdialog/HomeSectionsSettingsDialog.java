package de.danoeh.antennapod.ui.home.settingsdialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.home.HomeFragment;

import java.util.List;

public class HomeSectionsSettingsDialog {
    public static void open(Context context, DialogInterface.OnClickListener onSettingsChanged) {
        View content = View.inflate(context, de.danoeh.antennapod.ui.preferences.R.layout.choose_home_screen_order_dialog, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.configure_home);
        builder.setView(content);
        RecyclerView recyclerView = content.findViewById(de.danoeh.antennapod.ui.preferences.R.id.recyclerView);
        HomeScreenSettingDialogAdapter adapter = new HomeScreenSettingDialogAdapter(context);

        ItemTouchCallback itemMoveCallback = new ItemTouchCallback(adapter);
        ItemTouchHelper itemTouchHelper;
        itemTouchHelper = new ItemTouchHelper(itemMoveCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.setDragListener(itemTouchHelper::startDrag);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            saveChanges(context, adapter);
            onSettingsChanged.onClick(dialog, which);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    private static void saveChanges(Context context, HomeScreenSettingDialogAdapter adapter) {
        SharedPreferences prefs = context.getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
        final List<String> sectionOrder = adapter.getOrderedSectionTags();
        final List<String> hiddenSections = adapter.getHiddenSectionTags();

        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(HomeFragment.PREF_HIDDEN_SECTIONS, TextUtils.join(",", hiddenSections));
        edit.putString(HomeFragment.PREF_SECTION_ORDER, TextUtils.join(",", sectionOrder));
        edit.apply();
    }
}
