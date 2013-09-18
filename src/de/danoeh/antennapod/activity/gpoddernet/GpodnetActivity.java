package de.danoeh.antennapod.activity.gpoddernet;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.preferences.UserPreferences;

/**
 * Created by daniel on 23.08.13.
 */
public class GpodnetActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItemCompat.setShowAsAction(menu.add(Menu.NONE, R.id.search_item, Menu.NONE, R.string.search_label)
                .setIcon(
                        obtainStyledAttributes(
                                new int[]{R.attr.action_search})
                                .getDrawable(0)),
                MenuItem.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(menu.findItem(R.id.search_item), new SearchView(this));

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search_item));
        searchView.setIconifiedByDefault(true);
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }
}
