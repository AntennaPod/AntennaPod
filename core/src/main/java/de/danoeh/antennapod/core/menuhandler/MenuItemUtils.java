package de.danoeh.antennapod.core.menuhandler;

import android.view.Menu;
import android.view.MenuItem;

import de.danoeh.antennapod.core.R;

/**
 * Utilities for menu items
 */
public class MenuItemUtils {

    /**
     * @param menu    The menu that the MenuItem belongs to
     * @param resId   The id of the MenuItem
     */
    public static void updateRefreshMenuItem(Menu menu, int resId, boolean isRefreshing) {
        // expand actionview if feeds are being downloaded, collapse otherwise
        MenuItem refreshItem = menu.findItem(resId);
        if (isRefreshing) {
            if (refreshItem.getActionView() == null) {
                refreshItem.setActionView(R.layout.refresh_action_view);
            }
        } else {
            refreshItem.setActionView(null);
        }
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
