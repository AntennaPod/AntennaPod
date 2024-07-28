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

        final List<String> hiddenDrawerItems = UserPreferences.getHiddenDrawerItems();
        for (int i = 0; i < NavDrawerFragment.NAV_DRAWER_TAGS.length; i++) {
            String tag = NavDrawerFragment.NAV_DRAWER_TAGS[i];
            if (!hiddenDrawerItems.contains(tag)) {
                settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Section,
                        tag, context.getString(NavListAdapter.getLabel(tag))));
            }
        }

        String hiddenText = context.getString(R.string.section_hidden);
        settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Header, TAG_HIDDEN, hiddenText));

        for (String sectionTag : hiddenDrawerItems) {
            settingsDialogItems.add(new ReorderDialogItem(ReorderDialogItem.ViewType.Section,
                    sectionTag, context.getString(NavListAdapter.getLabel(sectionTag))));
        }
        return settingsDialogItems;
    }

    @Override
    protected void onReset() {
        UserPreferences.setHiddenDrawerItems(Collections.emptyList());
        if (onSettingsChanged != null) {
            onSettingsChanged.run();
        }
    }

    @Override
    protected void onConfirmed() {
        final List<String> hiddenDrawerItems = getTagsAfterHeader(TAG_HIDDEN);
        UserPreferences.setHiddenDrawerItems(hiddenDrawerItems);

        if (hiddenDrawerItems.contains(UserPreferences.getDefaultPage())) {
            for (String tag : NavDrawerFragment.NAV_DRAWER_TAGS) {
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
