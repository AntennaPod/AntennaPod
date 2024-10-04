package de.danoeh.antennapod.ui.screen.drawer;

import android.content.Context;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.screen.preferences.ReorderDialog;
import de.danoeh.antennapod.ui.screen.preferences.ReorderDialogItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DrawerPreferencesDialog extends ReorderDialog {
    private static final String TAG_HIDDEN = "hidden";
    private static final String TAG_SHOWN = "shown";
    private final Runnable onSettingsChanged;

    public DrawerPreferencesDialog(Context context, Runnable onSettingsChanged) {
        super(context);
        this.onSettingsChanged = onSettingsChanged;
    }

    @Override
    protected int getTitle() {
        return R.string.drawer_preferences;
    }

    @NonNull
    protected List<ReorderDialogItem> getInitialItems() {
        ArrayList<ReorderDialogItem> settingsDialogItems = new ArrayList<>();
        settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Header,
                TAG_SHOWN, context.getString(R.string.section_shown)));

        final List<String> drawerItemOrder = UserPreferences.getVisibleDrawerItemOrder();
        for (String tag : drawerItemOrder) {
            settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Section,
                    tag, context.getString(NavigationNames.getLabel(tag))));
        }

        settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Header,
                TAG_HIDDEN, context.getString(R.string.section_hidden)));

        final List<String> hiddenDrawerItems = UserPreferences.getHiddenDrawerItems();
        for (String sectionTag : hiddenDrawerItems) {
            settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Section,
                    sectionTag, context.getString(NavigationNames.getLabel(sectionTag))));
        }
        return settingsDialogItems;
    }

    @Override
    protected boolean onItemMove(int fromPosition, int toPosition) {
        if (toPosition == 0 || fromPosition == 0) {
            return false;
        }
        return super.onItemMove(fromPosition, toPosition);
    }

    @Override
    protected void onReset() {
        UserPreferences.setDrawerItemOrder(Collections.emptyList(), Collections.emptyList());
        if (onSettingsChanged != null) {
            onSettingsChanged.run();
        }
    }

    @Override
    protected void onConfirmed() {
        final List<String> hiddenDrawerItems = getTagsAfterHeader(TAG_HIDDEN);
        UserPreferences.setDrawerItemOrder(hiddenDrawerItems, getTagsWithoutHeaders());

        if (hiddenDrawerItems.contains(UserPreferences.getDefaultPage())) {
            for (String tag : context.getResources().getStringArray(R.array.nav_drawer_section_tags)) {
                if (!hiddenDrawerItems.contains(tag)) {
                    UserPreferences.setDefaultPage(tag);
                    break;
                }
            }
        }

        if (onSettingsChanged != null) {
            onSettingsChanged.run();
        }
    }
}
