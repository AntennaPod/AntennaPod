package de.danoeh.antennapod.dialog;

import android.content.Context;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.fragment.NavDrawerFragment;

import java.util.List;

public class DrawerPreferencesDialog {
    public static void show(Context context, Runnable callback) {
        final List<String> hiddenDrawerItems = UserPreferences.getHiddenDrawerItems();
        final String[] navTitles = context.getResources().getStringArray(R.array.nav_drawer_titles);
        boolean[] checked = new boolean[NavDrawerFragment.NAV_DRAWER_TAGS.length];
        for (int i = 0; i < NavDrawerFragment.NAV_DRAWER_TAGS.length; i++) {
            String tag = NavDrawerFragment.NAV_DRAWER_TAGS[i];
            if (!hiddenDrawerItems.contains(tag)) {
                checked[i] = true;
            }
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.drawer_preferences);
        builder.setMultiChoiceItems(navTitles, checked, (dialog, which, isChecked) -> {
            if (isChecked) {
                hiddenDrawerItems.remove(NavDrawerFragment.NAV_DRAWER_TAGS[which]);
            } else {
                hiddenDrawerItems.add(NavDrawerFragment.NAV_DRAWER_TAGS[which]);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            UserPreferences.setHiddenDrawerItems(hiddenDrawerItems);

            if (hiddenDrawerItems.contains(UserPreferences.getDefaultPage())) {
                for (String tag : NavDrawerFragment.NAV_DRAWER_TAGS) {
                    if (!hiddenDrawerItems.contains(tag)) {
                        UserPreferences.setDefaultPage(tag);
                        break;
                    }
                }
            }

            if (callback != null) {
                callback.run();
            }
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }
}
