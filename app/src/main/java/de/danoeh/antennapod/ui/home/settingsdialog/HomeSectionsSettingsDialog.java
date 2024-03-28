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
        final List<String> hiddenSections = HomeFragment.getHiddenSections(context);
        String[] sectionLabels = context.getResources().getStringArray(R.array.home_section_titles);
        String[] sectionTags = context.getResources().getStringArray(R.array.home_section_tags);
        final boolean[] checked = new boolean[sectionLabels.length];
        for (int i = 0; i < sectionLabels.length; i++) {
            String tag = sectionTags[i];
            if (!hiddenSections.contains(tag)) {
                checked[i] = true;
            }
        }

        View content = View.inflate(context, de.danoeh.antennapod.ui.preferences.R.layout.choose_home_screen_order_dialog, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.configure_home);
        builder.setView(content);
//        builder.setMultiChoiceItems(sectionLabels, checked, (dialog, which, isChecked) -> {
//            if (isChecked) {
//                hiddenSections.remove(sectionTags[which]);
//            } else {
//                hiddenSections.add(sectionTags[which]);
//            }
//        });
        RecyclerView recyclerView = content.findViewById(de.danoeh.antennapod.ui.preferences.R.id.recyclerView);
        HomeScreenSettingDialogAdapter adapter = new HomeScreenSettingDialogAdapter(context);

        ItemMoveCallback itemMoveCallback = new ItemMoveCallback(adapter);
        ItemTouchHelper itemTouchHelper;
        itemTouchHelper = new ItemTouchHelper(itemMoveCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.addDragListener(itemTouchHelper::startDrag);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);


        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            SharedPreferences prefs = context.getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(HomeFragment.PREF_HIDDEN_SECTIONS, TextUtils.join(",", hiddenSections)).apply();
            onSettingsChanged.onClick(dialog, which);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }
}
