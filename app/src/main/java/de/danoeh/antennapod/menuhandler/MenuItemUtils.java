package de.danoeh.antennapod.menuhandler;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.SearchView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.ThemeUtils;
import de.danoeh.antennapod.fragment.SearchFragment;

/**
 * Utilities for menu items
 */
public class MenuItemUtils extends de.danoeh.antennapod.core.menuhandler.MenuItemUtils {

    @SuppressWarnings("ResourceType")
    public static void refreshLockItem(Context context, Menu menu) {
        final MenuItem queueLock = menu.findItem(de.danoeh.antennapod.R.id.queue_lock);
        int[] lockIcons = new int[] { de.danoeh.antennapod.R.attr.ic_lock_open, de.danoeh.antennapod.R.attr.ic_lock_closed };
        TypedArray ta = context.obtainStyledAttributes(lockIcons);
        if (UserPreferences.isQueueLocked()) {
            queueLock.setTitle(de.danoeh.antennapod.R.string.unlock_queue);
            queueLock.setIcon(ta.getDrawable(0));
        } else {
            queueLock.setTitle(de.danoeh.antennapod.R.string.lock_queue);
            queueLock.setIcon(ta.getDrawable(1));
        }
        ta.recycle();
    }

    public static void setupSearchItem(Menu menu, MainActivity activity, long feedId) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView sv = (SearchView) searchItem.getActionView();
        sv.setBackgroundColor(ThemeUtils.getColorFromAttr(activity, android.R.attr.windowBackground));
        sv.setQueryHint(activity.getString(R.string.search_label));
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                sv.clearFocus();
                activity.loadChildFragment(SearchFragment.newInstance(s, feedId));
                searchItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                for (int i = 0; i < menu.size(); i++) {
                    if (menu.getItem(i).getItemId() != searchItem.getItemId()) {
                        menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    }
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                activity.invalidateOptionsMenu();
                return true;
            }
        });
    }
}
