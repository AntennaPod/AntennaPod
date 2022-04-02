package de.danoeh.antennapod.core.menuhandler;

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
     * bar, otherwise the progress bar will be hidden.
     *
     * @param menu    The menu that the MenuItem belongs to
     * @param resId   The id of the MenuItem
     * @param checker Is used for checking whether to show the progress indicator or not.
     * @return The returned value of the UpdateRefreshMenuItemChecker's isRefreshing() method.
     */
    public static boolean updateRefreshMenuItem(Menu menu, int resId, UpdateRefreshMenuItemChecker checker) {
        // expand actionview if feeds are being downloaded, collapse otherwise
        MenuItem refreshItem = menu.findItem(resId);
        if (checker.isRefreshing()) {
            refreshItem.setActionView(R.layout.refresh_action_view);
            return true;
        } else {
            refreshItem.setActionView(null);
            return false;
        }
    }

    public interface UpdateRefreshMenuItemChecker {
        boolean isRefreshing();
    }

    /**
     * When pressing a context menu item, Android calls onContextItemSelected
     * for ALL fragments in arbitrary order, not just for the fragment that the
     * context menu was created from. This assigns the listener to every menu item,
     * so that the correct fragment is always called first and can consume the click.
     * <p />
     * Note that Android still calls the onContextItemSelected methods of all fragments
     * when the passed listener returns false.
     */
    public static void setOnClickListeners(Menu menu, MenuItem.OnMenuItemClickListener listener) {
        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getSubMenu() != null) {
                setOnClickListeners(menu.getItem(i).getSubMenu(), listener);
            }
            menu.getItem(i).setOnMenuItemClickListener(listener);
        }
    }
}
