package de.danoeh.antennapod.menuhandler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.widget.SearchView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.ThemeUtils;
import de.danoeh.antennapod.fragment.SearchFragment;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for menu items.
 */
public class MenuItemUtils extends de.danoeh.antennapod.core.menuhandler.MenuItemUtils {

    public static void refreshLockItem(Context context, Menu menu) {
        final MenuItem queueLock = menu.findItem(R.id.queue_lock);
        if (UserPreferences.isQueueLocked()) {
            queueLock.setTitle(de.danoeh.antennapod.R.string.unlock_queue);
            queueLock.setIcon(ThemeUtils.getDrawableFromAttr(context, R.attr.ic_lock_open));
        } else {
            queueLock.setTitle(de.danoeh.antennapod.R.string.lock_queue);
            queueLock.setIcon(ThemeUtils.getDrawableFromAttr(context, R.attr.ic_lock_closed));
        }
    }

    public static void setupSearchItem(Menu menu, MainActivity activity, long feedId, String feedTitle) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView sv = (SearchView) searchItem.getActionView();
        sv.setBackgroundColor(ThemeUtils.getColorFromAttr(activity, android.R.attr.windowBackground));
        sv.setQueryHint(activity.getString(R.string.search_label));
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                sv.clearFocus();
                activity.loadChildFragment(SearchFragment.newInstance(s, feedId, feedTitle));
                searchItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            private final Map<Integer, Integer> oldShowAsActionState = new HashMap<>();

            @Override
            public boolean onMenuItemActionExpand(MenuItem clickedItem) {
                oldShowAsActionState.clear();
                for (int i = 0; i < menu.size(); i++) {
                    MenuItem item = menu.getItem(i);
                    if (item.getItemId() != searchItem.getItemId()) {
                        oldShowAsActionState.put(item.getItemId(), getShowAsActionFlag(item));
                        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    }
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem collapsedItem) {
                for (int i = 0; i < menu.size(); i++) {
                    MenuItem item = menu.getItem(i);
                    if (item.getItemId() != searchItem.getItemId()
                            && oldShowAsActionState.containsKey(item.getItemId())) {
                        item.setShowAsAction(oldShowAsActionState.get(item.getItemId()));
                    }
                }
                return true;
            }
        });
    }

    @SuppressLint("RestrictedApi")
    private static int getShowAsActionFlag(MenuItem item) {
        if (!(item instanceof MenuItemImpl)) {
            return MenuItemImpl.SHOW_AS_ACTION_NEVER;
        }
        MenuItemImpl itemImpl = ((MenuItemImpl) item);
        if (itemImpl.requiresActionButton()) {
            return MenuItemImpl.SHOW_AS_ACTION_ALWAYS;
        } else if (itemImpl.requestsActionButton()) {
            return MenuItemImpl.SHOW_AS_ACTION_IF_ROOM;
        } else if (itemImpl.showsTextAsAction()) {
            return MenuItemImpl.SHOW_AS_ACTION_WITH_TEXT;
        } else {
            return MenuItemImpl.SHOW_AS_ACTION_NEVER;
        }
    }
}
