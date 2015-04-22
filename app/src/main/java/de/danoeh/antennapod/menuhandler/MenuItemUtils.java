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
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        MenuItemCompat.setActionView(item, searchView);
        return item;
    }

}
