package de.danoeh.antennapod.menuhandler;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Utilities for menu items
 */
public class MenuItemUtils extends de.danoeh.antennapod.core.menuhandler.MenuItemUtils {

    public static MenuItem addSearchItem(Menu menu, SearchView searchView) {
        MenuItem item = menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(item, searchView);

        if(Build.VERSION.SDK_INT < 14) {
            SearchView.SearchAutoComplete textField = (SearchView.SearchAutoComplete) searchView.findViewById(de.danoeh.antennapod.R.id.search_src_text);
            if(UserPreferences.getTheme() == de.danoeh.antennapod.R.style.Theme_AntennaPod_Dark) {
                textField.setTextColor(Resources.getSystem().getColor(android.R.color.white));
            } else {
                textField.setTextColor(Resources.getSystem().getColor(android.R.color.black));
            }
        }

        return item;
    }

    public static void refreshLockItem(Context context, Menu menu, MenuItem queueLock) {
        queueLock = menu.findItem(de.danoeh.antennapod.R.id.queue_lock);
        int[] lockIcons = new int[] { de.danoeh.antennapod.R.attr.ic_lock_open, de.danoeh.antennapod.R.attr.ic_lock_closed };
        TypedArray ta = context.obtainStyledAttributes(lockIcons);
        if (UserPreferences.isQueueLocked()) {
            queueLock.setTitle(de.danoeh.antennapod.R.string.unlock_queue);
            queueLock.setIcon(ta.getDrawable(1));
        } else {
            queueLock.setTitle(de.danoeh.antennapod.R.string.lock_queue);
            queueLock.setIcon(ta.getDrawable(0));
        }
    }

}
