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

        final List<String> drawerItemOrder = UserPreferences.getVisibleDrawerItemOrder();
        for (String tag : drawerItemOrder) {
            settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Section,
                    tag, context.getString(NavListAdapter.getLabel(tag))));
        }

        String hiddenText = context.getString(R.string.section_hidden);
        settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Header, TAG_HIDDEN, hiddenText));

        final List<String> hiddenDrawerItems = UserPreferences.getHiddenDrawerItems();
        for (String sectionTag : hiddenDrawerItems) {
            settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Section,
                    sectionTag, context.getString(NavListAdapter.getLabel(sectionTag))));
        }
        return settingsDialogItems;
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
