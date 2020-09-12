package de.danoeh.antennapod.dialog;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.core.preferences.UserPreferences;

public class FeedSortDialog {
    public static void showDialog(Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(context.getString(R.string.pref_nav_drawer_feed_order_title));
        dialog.setNegativeButton(android.R.string.cancel, (d, listener) -> d.dismiss());

        int selected = UserPreferences.getFeedOrder();
        List<String> entryValues =
                Arrays.asList(context.getResources().getStringArray(R.array.nav_drawer_feed_order_values));
        final int selectedIndex = entryValues.indexOf("" + selected);

        String[] items = context.getResources().getStringArray(R.array.nav_drawer_feed_order_options);
        dialog.setSingleChoiceItems(items, selectedIndex, (d, which) -> {
            if (selectedIndex != which) {
                UserPreferences.setFeedOrder(entryValues.get(which));
                //Update subscriptions
                EventBus.getDefault().post(new UnreadItemsUpdateEvent());
            }
            d.dismiss();
        });
        dialog.show();
    }
}
