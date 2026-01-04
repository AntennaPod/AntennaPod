package de.danoeh.antennapod.ui.screen.drawer;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.ListPopupWindow;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.internal.ViewUtils;
import com.google.android.material.navigation.NavigationBarView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class BottomNavigation {
    private static final String TAG = "BottomNavigation";

    private final BottomNavigationView bottomNavigationView;
    private final Context context;

    private Disposable bottomNavigationBadgeLoader = null;

    public BottomNavigation(BottomNavigationView bottomNavigationView) {
        this.bottomNavigationView = bottomNavigationView;
        this.context = bottomNavigationView.getContext();
        ViewUtils.doOnApplyWindowInsets(bottomNavigationView, (view, insets, initialPadding) -> insets);
    }

    public void buildMenu() {
        List<String> drawerItems = UserPreferences.getVisibleDrawerItemOrder();
        drawerItems.remove(NavListAdapter.SUBSCRIPTION_LIST_TAG);

        Menu menu = bottomNavigationView.getMenu();
        menu.clear();
        int maxItems = Math.min(5, bottomNavigationView.getMaxItemCount());
        for (int i = 0; i < drawerItems.size() && i < maxItems - 1; i++) {
            String tag = drawerItems.get(i);
            MenuItem item = menu.add(0, NavigationNames.getBottomNavigationItemId(tag),
                    0, context.getString(NavigationNames.getShortLabel(tag)));
            item.setIcon(NavigationNames.getDrawable(tag));
        }
        MenuItem moreItem = menu.add(0, R.id.bottom_navigation_more, 0, context.getString(R.string.overflow_more));
        moreItem.setIcon(R.drawable.dots_vertical);
        bottomNavigationView.setOnItemSelectedListener(bottomItemSelectedListener);
        updateBottomNavigationBadgeIfNeeded();
    }

    private void updateBottomNavigationBadgeIfNeeded() {
        if (bottomNavigationView == null) {
            return;
        } else if (bottomNavigationView.getMenu().findItem(R.id.bottom_navigation_inbox) == null) {
            return;
        }
        if (bottomNavigationBadgeLoader != null) {
            bottomNavigationBadgeLoader.dispose();
        }
        bottomNavigationBadgeLoader = Observable.fromCallable(
                        () -> DBReader.getTotalEpisodeCount(new FeedItemFilter(FeedItemFilter.NEW)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.bottom_navigation_inbox);
                    badge.setVisible(result > 0);
                    badge.setNumber(result);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private final NavigationBarView.OnItemSelectedListener bottomItemSelectedListener = item -> {
        if (item.getItemId() == R.id.bottom_navigation_more) {
            showBottomNavigationMorePopup();
            return false;
        } else {
            onItemSelected(item.getItemId());
            return true;
        }
    };

    private void showBottomNavigationMorePopup() {
        List<String> drawerItems = UserPreferences.getVisibleDrawerItemOrder();
        drawerItems.remove(NavListAdapter.SUBSCRIPTION_LIST_TAG);

        final List<MenuItem> popupMenuItems = new ArrayList<>();
        for (int i = bottomNavigationView.getMaxItemCount() - 1; i < drawerItems.size(); i++) {
            String tag = drawerItems.get(i);
            MenuItem item = new MenuBuilder(context).add(0, NavigationNames.getBottomNavigationItemId(tag),
                    0, context.getString(NavigationNames.getLabel(tag)));
            item.setIcon(NavigationNames.getDrawable(tag));
            popupMenuItems.add(item);
        }
        MenuItem customizeItem = new MenuBuilder(context).add(0, R.id.bottom_navigation_customize,
                0, context.getString(R.string.pref_nav_drawer_items_title));
        customizeItem.setIcon(R.drawable.ic_pencil);
        popupMenuItems.add(customizeItem);

        MenuItem settingsItem = new MenuBuilder(context).add(0, R.id.bottom_navigation_settings,
                0, context.getString(R.string.settings_label));
        settingsItem.setIcon(R.drawable.ic_settings);
        popupMenuItems.add(settingsItem);

        final ListPopupWindow listPopupWindow = new ListPopupWindow(context);
        listPopupWindow.setWidth((int) (250 * context.getResources().getDisplayMetrics().density));
        listPopupWindow.setAnchorView(bottomNavigationView);
        listPopupWindow.setAdapter(new BottomNavigationMoreAdapter(context, popupMenuItems));
        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            int itemId = popupMenuItems.get(position).getItemId();
            if (itemId == R.id.bottom_navigation_customize) {
                new DrawerPreferencesDialog(context, this::buildMenu).show();
            } else {
                onItemSelected(itemId);
            }
            listPopupWindow.dismiss();
        });
        listPopupWindow.setDropDownGravity(Gravity.END | Gravity.BOTTOM);
        listPopupWindow.setModal(true);
        listPopupWindow.show();
    }

    public void updateSelectedItem(String tag) {
        int bottomSelectedItem = NavigationNames.getBottomNavigationItemId(tag);
        if (bottomNavigationView.getMenu().findItem(bottomSelectedItem) == null) {
            bottomSelectedItem = R.id.bottom_navigation_more;
        }
        bottomNavigationView.setOnItemSelectedListener(null);
        bottomNavigationView.setSelectedItemId(bottomSelectedItem);
        bottomNavigationView.setOnItemSelectedListener(bottomItemSelectedListener);
    }

    public void onItemSelected(@IdRes int itemId) {
    }

    public void hide() {
        bottomNavigationView.setVisibility(View.GONE);
    }

    public void onCreateView() {
        EventBus.getDefault().register(this);
    }

    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        if (bottomNavigationBadgeLoader != null) {
            bottomNavigationBadgeLoader.dispose();
            bottomNavigationBadgeLoader = null;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        updateBottomNavigationBadgeIfNeeded();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        updateBottomNavigationBadgeIfNeeded();
    }
}
