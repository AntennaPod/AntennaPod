package de.danoeh.antennapod.dialog;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;

public class FeedFilterDialog {
    public static void showDialog(Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(context.getString(R.string.pref_filter_feed_title));
        dialog.setPositiveButton(null, null);
        dialog.setNegativeButton(android.R.string.cancel, (d, listener) -> d.dismiss());

        int selectedIndexTemp = 0;
        int selected = UserPreferences.getFeedFilter();
        String[] entryValues = context.getResources().getStringArray(R.array.nav_drawer_feed_filter_values);
        for (int i = 0; i < entryValues.length; i++) {
            if (Integer.parseInt(entryValues[i]) == selected)
                selectedIndexTemp = i;
        }

        final int selectedIndex = selectedIndexTemp;
        dialog.setSingleChoiceItems(context.getResources().getStringArray(R.array.nav_drawer_feed_filter_options), selectedIndex, (d, which) -> {
            if (selectedIndex != which) {
                UserPreferences.setFeedFilter(entryValues[which]);
                //TODO Update Subscriptions
            }
            d.dismiss();
        });
        dialog.show();
    }
}
