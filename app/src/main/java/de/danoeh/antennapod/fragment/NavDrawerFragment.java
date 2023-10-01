package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.adapter.NavListAdapter;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.dialog.DrawerPreferencesDialog;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
import de.danoeh.antennapod.dialog.RenameItemDialog;
import de.danoeh.antennapod.dialog.SubscriptionsFilterDialog;
import de.danoeh.antennapod.dialog.TagSettingsDialog;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.QueueEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.home.HomeFragment;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class NavDrawerFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @VisibleForTesting
    public static final String PREF_LAST_FRAGMENT_TAG = "prefLastFragmentTag";
    private static final String PREF_OPEN_FOLDERS = "prefOpenFolders";
    @VisibleForTesting
    public static final String PREF_NAME = "NavDrawerPrefs";
    public static final String TAG = "NavDrawerFragment";

    public static final String[] NAV_DRAWER_TAGS = {
            HomeFragment.TAG,
            QueueFragment.TAG,
            InboxFragment.TAG,
            AllEpisodesFragment.TAG,
            SubscriptionFragment.TAG,
            CompletedDownloadsFragment.TAG,
            PlaybackHistoryFragment.TAG,
            AddFeedFragment.TAG,
            NavListAdapter.SUBSCRIPTION_LIST_TAG
    };

    private NavDrawerData navDrawerData;
    private List<NavDrawerData.DrawerItem> flatItemList;
    private NavDrawerData.DrawerItem contextPressedItem = null;
    private NavListAdapter navAdapter;
    private Disposable disposable;
    private ProgressBar progressBar;
    private Set<String> openFolders = new HashSet<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.nav_list, container, false);
        setupDrawerRoundBackground(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, 0);
            float navigationBarHeight = 0;
            Activity activity = getActivity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && activity != null) {
                navigationBarHeight = getActivity().getWindow().getNavigationBarDividerColor() == Color.TRANSPARENT
                        ? 0 : 1 * getResources().getDisplayMetrics().density; // Assuming the divider is 1dp in height
            }
            float bottomInset = Math.max(0f, Math.round(bars.bottom - navigationBarHeight));
            ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin = (int) bottomInset;
            return insets;
        });

        SharedPreferences preferences = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        openFolders = new HashSet<>(preferences.getStringSet(PREF_OPEN_FOLDERS, new HashSet<>())); // Must not modify

        progressBar = root.findViewById(R.id.progressBar);
        RecyclerView navList = root.findViewById(R.id.nav_list);
        navAdapter = new NavListAdapter(itemAccess, getActivity());
        navAdapter.setHasStableIds(true);
        navList.setAdapter(navAdapter);
        navList.setLayoutManager(new LinearLayoutManager(getContext()));

        root.findViewById(R.id.nav_settings).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), PreferenceActivity.class)));

        preferences.registerOnSharedPreferenceChangeListener(this);
        return root;
    }

    private void setupDrawerRoundBackground(View root) {
        // Akin to this logic:
        //   https://github.com/material-components/material-components-android/blob/8938da8c/lib/java/com/google/android/material/navigation/NavigationView.java#L405
        ShapeAppearanceModel.Builder shapeBuilder = ShapeAppearanceModel.builder();
        float cornerSize = getResources().getDimension(R.dimen.drawer_corner_size);
        boolean isRtl = getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        if (isRtl) {
            shapeBuilder.setTopLeftCornerSize(cornerSize).setBottomLeftCornerSize(cornerSize);
        } else {
            shapeBuilder.setTopRightCornerSize(cornerSize).setBottomRightCornerSize(cornerSize);
        }
        MaterialShapeDrawable drawable = new MaterialShapeDrawable(shapeBuilder.build());
        int themeColor = ThemeUtils.getColorFromAttr(root.getContext(), android.R.attr.colorBackground);
        drawable.setFillColor(ColorStateList.valueOf(themeColor));
        root.setBackground(drawable);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
        getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        menu.setHeaderTitle(contextPressedItem.getTitle());
        if (contextPressedItem.type == NavDrawerData.DrawerItem.Type.FEED) {
            inflater.inflate(R.menu.nav_feed_context, menu);
            // episodes are not loaded, so we cannot check if the podcast has new or unplayed ones!
        } else {
            inflater.inflate(R.menu.nav_folder_context, menu);
        }
        MenuItemUtils.setOnClickListeners(menu, this::onContextItemSelected);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        NavDrawerData.DrawerItem pressedItem = contextPressedItem;
        contextPressedItem = null;
        if (pressedItem == null) {
            return false;
        }
        if (pressedItem.type == NavDrawerData.DrawerItem.Type.FEED) {
            return onFeedContextMenuClicked(((NavDrawerData.FeedDrawerItem) pressedItem).feed, item);
        } else {
            return onTagContextMenuClicked(pressedItem, item);
        }
    }

    private boolean onFeedContextMenuClicked(Feed feed, MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.remove_all_inbox_item) {
            ConfirmationDialog removeAllNewFlagsConfirmationDialog = new ConfirmationDialog(getContext(),
                    R.string.remove_all_inbox_label,
                    R.string.remove_all_inbox_confirmation_msg) {
                @Override
                public void onConfirmButtonPressed(DialogInterface dialog) {
                    dialog.dismiss();
                    DBWriter.removeFeedNewFlag(feed.getId());
                }
            };
            removeAllNewFlagsConfirmationDialog.createNewDialog().show();
            return true;
        } else if (itemId == R.id.edit_tags) {
            TagSettingsDialog.newInstance(Collections.singletonList(feed.getPreferences()))
                    .show(getChildFragmentManager(), TagSettingsDialog.TAG);
            return true;
        } else if (itemId == R.id.rename_item) {
            new RenameItemDialog(getActivity(), feed).show();
            return true;
        } else if (itemId == R.id.remove_feed) {
            RemoveFeedDialog.show(getContext(), feed, () -> {
                if (String.valueOf(feed.getId()).equals(getLastNavFragment(getContext()))) {
                    ((MainActivity) getActivity()).loadFragment(UserPreferences.getDefaultPage(), null);
                    // Make sure fragment is hidden before actually starting to delete
                    getActivity().getSupportFragmentManager().executePendingTransactions();
                }
            });
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private boolean onTagContextMenuClicked(NavDrawerData.DrawerItem drawerItem, MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.rename_folder_item) {
            new RenameItemDialog(getActivity(), drawerItem).show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadData();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        loadData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onQueueChanged(QueueEvent event) {
        Log.d(TAG, "onQueueChanged(" + event + ")");
        // we are only interested in the number of queue items, not download status or position
        if (event.action == QueueEvent.Action.DELETED_MEDIA
                || event.action == QueueEvent.Action.SORTED
                || event.action == QueueEvent.Action.MOVED) {
            return;
        }
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private final NavListAdapter.ItemAccess itemAccess = new NavListAdapter.ItemAccess() {
        @Override
        public int getCount() {
            if (flatItemList != null) {
                return flatItemList.size();
            } else {
                return 0;
            }
        }

        @Override
        public NavDrawerData.DrawerItem getItem(int position) {
            if (flatItemList != null && 0 <= position && position < flatItemList.size()) {
                return flatItemList.get(position);
            } else {
                return null;
            }
        }

        @Override
        public boolean isSelected(int position) {
            String lastNavFragment = getLastNavFragment(getContext());
            if (position < navAdapter.getSubscriptionOffset()) {
                return navAdapter.getFragmentTags().get(position).equals(lastNavFragment);
            } else if (StringUtils.isNumeric(lastNavFragment)) { // last fragment was not a list, but a feed
                long feedId = Long.parseLong(lastNavFragment);
                if (navDrawerData != null) {
                    NavDrawerData.DrawerItem itemToCheck = flatItemList.get(
                            position - navAdapter.getSubscriptionOffset());
                    if (itemToCheck.type == NavDrawerData.DrawerItem.Type.FEED) {
                        // When the same feed is displayed multiple times, it should be highlighted multiple times.
                        return ((NavDrawerData.FeedDrawerItem) itemToCheck).feed.getId() == feedId;
                    }
                }
            }
            return false;
        }

        @Override
        public int getQueueSize() {
            return (navDrawerData != null) ? navDrawerData.queueSize : 0;
        }

        @Override
        public int getNumberOfNewItems() {
            return (navDrawerData != null) ? navDrawerData.numNewItems : 0;
        }

        @Override
        public int getNumberOfDownloadedItems() {
            return (navDrawerData != null) ? navDrawerData.numDownloadedItems : 0;
        }

        @Override
        public int getReclaimableItems() {
            return (navDrawerData != null) ? navDrawerData.reclaimableSpace : 0;
        }

        @Override
        public int getFeedCounterSum() {
            if (navDrawerData == null) {
                return 0;
            }
            int sum = 0;
            for (int counter : navDrawerData.feedCounters.values()) {
                sum += counter;
            }
            return sum;
        }

        @Override
        public void onItemClick(int position) {
            int viewType = navAdapter.getItemViewType(position);
            if (viewType != NavListAdapter.VIEW_TYPE_SECTION_DIVIDER) {
                if (position < navAdapter.getSubscriptionOffset()) {
                    String tag = navAdapter.getFragmentTags().get(position);
                    ((MainActivity) getActivity()).loadFragment(tag, null);
                    ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    int pos = position - navAdapter.getSubscriptionOffset();
                    NavDrawerData.DrawerItem clickedItem = flatItemList.get(pos);

                    if (clickedItem.type == NavDrawerData.DrawerItem.Type.FEED) {
                        long feedId = ((NavDrawerData.FeedDrawerItem) clickedItem).feed.getId();
                        ((MainActivity) getActivity()).loadFeedFragmentById(feedId, null);
                        ((MainActivity) getActivity()).getBottomSheet()
                                .setState(BottomSheetBehavior.STATE_COLLAPSED);
                    } else {
                        NavDrawerData.TagDrawerItem folder = ((NavDrawerData.TagDrawerItem) clickedItem);
                        if (openFolders.contains(folder.name)) {
                            openFolders.remove(folder.name);
                        } else {
                            openFolders.add(folder.name);
                        }

                        getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putStringSet(PREF_OPEN_FOLDERS, openFolders)
                                .apply();

                        disposable = Observable.fromCallable(() -> makeFlatDrawerData(navDrawerData.items, 0))
                                .subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        result -> {
                                            flatItemList = result;
                                            navAdapter.notifyDataSetChanged();
                                        }, error -> Log.e(TAG, Log.getStackTraceString(error)));
                    }
                }
            } else if (UserPreferences.getSubscriptionsFilter().isEnabled()
                    && navAdapter.showSubscriptionList) {
                SubscriptionsFilterDialog.showDialog(requireContext());
            }
        }

        @Override
        public boolean onItemLongClick(int position) {
            if (position < navAdapter.getFragmentTags().size()) {
                DrawerPreferencesDialog.show(getContext(), () -> {
                    navAdapter.notifyDataSetChanged();
                    if (UserPreferences.getHiddenDrawerItems().contains(getLastNavFragment(getContext()))) {
                        new MainActivityStarter(getContext())
                                .withFragmentLoaded(UserPreferences.getDefaultPage())
                                .withDrawerOpen()
                                .start();
                    }
                });
                return true;
            } else {
                contextPressedItem = flatItemList.get(position - navAdapter.getSubscriptionOffset());
                return false;
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            NavDrawerFragment.this.onCreateContextMenu(menu, v, menuInfo);
        }
    };

    private void loadData() {
        disposable = Observable.fromCallable(
                () -> {
                    NavDrawerData data = DBReader.getNavDrawerData(UserPreferences.getSubscriptionsFilter());
                    return new Pair<>(data, makeFlatDrawerData(data.items, 0));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            navDrawerData = result.first;
                            flatItemList = result.second;
                            navAdapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE); // Stays hidden once there is something in the list
                        }, error -> {
                            Log.e(TAG, Log.getStackTraceString(error));
                            progressBar.setVisibility(View.GONE);
                        });
    }

    private List<NavDrawerData.DrawerItem> makeFlatDrawerData(List<NavDrawerData.DrawerItem> items, int layer) {
        List<NavDrawerData.DrawerItem> flatItems = new ArrayList<>();
        for (NavDrawerData.DrawerItem item : items) {
            item.setLayer(layer);
            flatItems.add(item);
            if (item.type == NavDrawerData.DrawerItem.Type.TAG) {
                NavDrawerData.TagDrawerItem folder = ((NavDrawerData.TagDrawerItem) item);
                folder.isOpen = openFolders.contains(folder.name);
                if (folder.isOpen) {
                    flatItems.addAll(makeFlatDrawerData(((NavDrawerData.TagDrawerItem) item).children, layer + 1));
                }
            }
        }
        return flatItems;
    }

    public static void saveLastNavFragment(Context context, String tag) {
        Log.d(TAG, "saveLastNavFragment(tag: " + tag + ")");
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        if (tag != null) {
            edit.putString(PREF_LAST_FRAGMENT_TAG, tag);
        } else {
            edit.remove(PREF_LAST_FRAGMENT_TAG);
        }
        edit.apply();
    }

    public static String getLastNavFragment(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastFragment = prefs.getString(PREF_LAST_FRAGMENT_TAG, HomeFragment.TAG);
        Log.d(TAG, "getLastNavFragment() -> " + lastFragment);
        return lastFragment;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_LAST_FRAGMENT_TAG.equals(key)) {
            navAdapter.notifyDataSetChanged(); // Update selection
        }
    }
}
