package de.danoeh.antennapod.ui.screen.home.settingsdialog;

import android.content.Context;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.screen.preferences.ReorderDialog;
import de.danoeh.antennapod.ui.screen.preferences.ReorderDialogItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeSectionsSettingsDialog extends ReorderDialog {
    private final Runnable onSettingsChanged;

    public HomeSectionsSettingsDialog(Context context, Runnable onSettingsChanged) {
        super(context);
        this.onSettingsChanged = onSettingsChanged;
    }

    @Override
    protected int getTitle() {
        return R.string.configure_home;
    }

    @NonNull
    protected List<ReorderDialogItem> getInitialItems() {
        final List<String> sectionTags = HomePreferences.getSortedSectionTags(context);
        final List<String> hiddenSectionTags = HomePreferences.getHiddenSectionTags(context);

        ArrayList<ReorderDialogItem> settingsDialogItems = new ArrayList<>();
        for (String sectionTag: sectionTags) {
            settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Section, sectionTag));
        }
        String hiddenText = context.getString(R.string.section_hidden);
        settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Header, hiddenText));
        for (String sectionTag: hiddenSectionTags) {
            settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Section, sectionTag));
        }
        return settingsDialogItems;
    }

    @Override
    protected void onReset() {
        HomePreferences.saveChanges(context, Collections.emptyList(), Collections.emptyList());
        onSettingsChanged.run();
    }

    @Override
    protected void onConfirmed() {
        final List<String> sectionOrder = getItemsWithoutHeaders();
        final List<String> hiddenSections = getHiddenSectionTags();
        HomePreferences.saveChanges(context, hiddenSections, sectionOrder);
        onSettingsChanged.run();
    }

    @NonNull
    public List<String> getHiddenSectionTags() {
        List<String> hiddenSections = new ArrayList<>();
        for (int i = dialogItems.size() - 1; i >= 0; i--) {
            ReorderDialogItem item = dialogItems.get(i);
            if (item.getViewType() == ReorderDialogItem.ViewType.Header) {
                return hiddenSections;
            }
            hiddenSections.add(item.getTitle());
        }
        Collections.reverse(hiddenSections);
        return hiddenSections;
    }
}
