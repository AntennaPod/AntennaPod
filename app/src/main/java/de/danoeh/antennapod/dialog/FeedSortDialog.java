package de.danoeh.antennapod.dialog;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.core.preferences.UserPreferences;

public class FeedSortDialog {
    public static void showDialog(Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(context.getString(R.string.pref_nav_drawer_feed_order_title));
        dialog.setNegativeButton(android.R.string.cancel, (d, listener) -> d.dismiss());

        int selectedIndexTemp = 0;
        int selected = UserPreferences.getFeedOrder();
        String[] entryValues = context.getResources().getStringArray(R.array.nav_drawer_feed_order_values);
        for (int i = 0; i < entryValues.length; i++) {
            if (Integer.parseInt(entryValues[i]) == selected) {
                selectedIndexTemp = i;
            }
        }

        final int selectedIndex = selectedIndexTemp;
        String[] items = context.getResources().getStringArray(R.array.nav_drawer_feed_order_options);
        dialog.setSingleChoiceItems(items, selectedIndex, (d, which) -> {
            if (selectedIndex != which) {
                UserPreferences.setFeedOrder(entryValues[which]);
                //Update subscriptions
                EventBus.getDefault().post(new UnreadItemsUpdateEvent());
            }
            d.dismiss();
        });
        dialog.show();
    }
}
