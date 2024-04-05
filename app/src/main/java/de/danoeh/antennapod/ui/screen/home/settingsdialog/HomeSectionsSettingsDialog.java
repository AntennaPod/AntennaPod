package de.danoeh.antennapod.ui.screen.home.settingsdialog;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.ChooseHomeScreenOrderDialogBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeSectionsSettingsDialog {
    public static void open(Context context, Runnable onSettingsChanged) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        ChooseHomeScreenOrderDialogBinding viewBinding = ChooseHomeScreenOrderDialogBinding.inflate(layoutInflater);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.configure_home);
        builder.setView(viewBinding.getRoot());
        RecyclerView recyclerView = viewBinding.recyclerView;
        List<HomeScreenSettingsDialogItem> dialogItems = initialItemsSettingsDialog(context);
        HomeScreenSettingDialogAdapter adapter = new HomeScreenSettingDialogAdapter(dialogItems);

        configureRecyclerView(recyclerView, adapter, context);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            final List<String> sectionOrder = adapter.getOrderedSectionTags();
            final List<String> hiddenSections = adapter.getHiddenSectionTags();
            HomePreferences.saveChanges(context, hiddenSections, sectionOrder);
            onSettingsChanged.run();
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.setNeutralButton(R.string.reset, (dialog, which) -> {
            HomePreferences.saveChanges(context, Collections.emptyList(), Collections.emptyList());
            onSettingsChanged.run();
        });
        builder.show();
    }

    private static void configureRecyclerView(RecyclerView recyclerView,
                                              HomeScreenSettingDialogAdapter adapter, Context context) {
        ItemTouchCallback itemMoveCallback = new ItemTouchCallback() {
            @Override
            protected boolean onItemMove(int fromPosition, int toPosition) {
                return adapter.onItemMove(fromPosition, toPosition);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemMoveCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.setDragListener(itemTouchHelper::startDrag);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
    }

    @NonNull
    private static List<HomeScreenSettingsDialogItem> initialItemsSettingsDialog(@NonNull Context context) {
        final List<String> sectionTags = HomePreferences.getSortedSectionTags(context);
        final List<String> hiddenSectionTags = HomePreferences.getHiddenSectionTags(context);

        ArrayList<HomeScreenSettingsDialogItem> settingsDialogItems = new ArrayList<>();
        for (String sectionTag: sectionTags) {
            settingsDialogItems.add(new HomeScreenSettingsDialogItem(
                    HomeScreenSettingsDialogItem.ViewType.Section, sectionTag));
        }
        String hiddenText = context.getString(R.string.section_hidden);
        settingsDialogItems.add(new HomeScreenSettingsDialogItem(
                HomeScreenSettingsDialogItem.ViewType.Header, hiddenText));
        for (String sectionTag: hiddenSectionTags) {
            settingsDialogItems.add(new HomeScreenSettingsDialogItem(
                    HomeScreenSettingsDialogItem.ViewType.Section, sectionTag));
        }

        return settingsDialogItems;
    }
}
