package de.danoeh.antennapod.menuhandler;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import de.danoeh.antennapod.core.R;

/**
 * Utilities for menu items
 */
public class MenuItemUtils extends de.danoeh.antennapod.core.menuhandler.MenuItemUtils {

    public static MenuItem addSearchItem(Menu menu, SearchView searchView) {
        MenuItem item = menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(item, searchView);
        return item;
    }

    /**
     * Checks if the navigation drawer of the DrawerActivity is opened. This can be useful for Fragments
     * that hide their menu if the navigation drawer is open.
     *
     * @return True if the drawer is open, false otherwise (also if the parameter is null)
     */
    public static boolean isActivityDrawerOpen(NavDrawerActivity activity) {
        return activity != null && activity.isDrawerOpen();
    }
}
