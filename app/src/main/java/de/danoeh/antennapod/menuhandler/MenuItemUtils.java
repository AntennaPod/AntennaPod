package de.danoeh.antennapod.menuhandler;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import de.danoeh.antennapod.core.R;

/**
 * Utilities for menu items
 */
public class MenuItemUtils {

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

    /**
     * Changes the appearance of a MenuItem depending on whether the given UpdateRefreshMenuItemChecker
     * is refreshing or not. If it returns true, the menu item will be replaced by an indeterminate progress
     * bar, otherwise nothing will happen.
     *
     * @param menu    The menu that the MenuItem belongs to
     * @param resId   The id of the MenuItem
     * @param checker Is used for checking whether to show the progress indicator or not.
     * @return The returned value of the UpdateRefreshMenuItemChecker's isRefreshing() method.
     */
    public static boolean updateRefreshMenuItem(Menu menu, int resId, UpdateRefreshMenuItemChecker checker) {
        // expand actionview if feeds are being downloaded, collapse otherwise
        if (checker.isRefreshing()) {
            MenuItem refreshItem = menu.findItem(resId);
            MenuItemCompat.setActionView(refreshItem, de.danoeh.antennapod.R.layout.refresh_action_view);
            return true;
        } else {
            return false;
        }
    }

    public static interface UpdateRefreshMenuItemChecker {
        public boolean isRefreshing();
    }
}
