package de.danoeh.antennapod.core.menuhandler;

import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import de.danoeh.antennapod.core.R;

/**
 * Utilities for menu items
 */
public class MenuItemUtils {

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
            MenuItemCompat.setActionView(refreshItem, R.layout.refresh_action_view);
            return true;
        } else {
            return false;
        }
    }

    public interface UpdateRefreshMenuItemChecker {
        boolean isRefreshing();
    }
}
