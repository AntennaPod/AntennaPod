package de.danoeh.antennapod.ui.screen.subscriptions;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.ui.screen.AddFeedFragment;
import de.danoeh.antennapod.ui.screen.SearchFragment;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.ui.view.FloatingSelectMenu;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.ui.MenuItemUtils;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.NavDrawerData;
import de.danoeh.antennapod.ui.screen.feed.RenameFeedDialog;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.statistics.StatisticsFragment;
import de.danoeh.antennapod.ui.view.EmptyViewHandler;
import de.danoeh.antennapod.ui.view.LiftOnScrollListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Fragment for displaying feed subscriptions
 */
public class SubscriptionFragment extends Fragment
        implements MaterialToolbar.OnMenuItemClickListener,
        SubscriptionsRecyclerAdapter.OnSelectModeListener {
    public static final String TAG = "SubscriptionFragment";
    private static final String PREFS = "SubscriptionFragment";
    private static final String PREF_NUM_COLUMNS = "columns";
    private static final String KEY_UP_ARROW = "up_arrow";
    private static final String ARGUMENT_FOLDER = "folder";

    private static final int MIN_NUM_COLUMNS = 1;
    private static final int[] COLUMN_CHECKBOX_IDS = {
            R.id.subscription_display_list,
            R.id.subscription_num_columns_2,
            R.id.subscription_num_columns_3,
            R.id.subscription_num_columns_4,
            R.id.subscription_num_columns_5};

    private RecyclerView subscriptionRecycler;
    private SubscriptionsRecyclerAdapter subscriptionAdapter;
    private EmptyViewHandler emptyView;
    private View feedsFilteredMsg;
    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private String displayedFolder = null;
    private boolean displayUpArrow;

    private Disposable disposable;
    private SharedPreferences prefs;

    private FloatingActionButton subscriptionAddButton;
    private FloatingSelectMenu floatingSelectMenu;
    private RecyclerView.ItemDecoration itemDecoration;
    private List<NavDrawerData.DrawerItem> listItems;

    public static SubscriptionFragment newInstance(String folderTitle) {
        SubscriptionFragment fragment = new SubscriptionFragment();
        Bundle args = new Bundle();
        args.putString(ARGUMENT_FOLDER, folderTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscriptions, container, false);
        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setOnLongClickListener(v -> {
            subscriptionRecycler.scrollToPosition(5);
            subscriptionRecycler.post(() -> subscriptionRecycler.smoothScrollToPosition(0));
            return false;
        });
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar, displayUpArrow);
        toolbar.inflateMenu(R.menu.subscriptions);
        for (int i = 1; i < COLUMN_CHECKBOX_IDS.length; i++) {
            // Do this in Java to localize numbers
            toolbar.getMenu().findItem(COLUMN_CHECKBOX_IDS[i])
                    .setTitle(String.format(Locale.getDefault(), "%d", i + MIN_NUM_COLUMNS));
        }
        refreshToolbarState();

        if (getArguments() != null) {
            displayedFolder = getArguments().getString(ARGUMENT_FOLDER, null);
            if (displayedFolder != null) {
                toolbar.setTitle(displayedFolder);
            }
        }

        subscriptionRecycler = root.findViewById(R.id.subscriptions_grid);
        registerForContextMenu(subscriptionRecycler);
        subscriptionRecycler.addOnScrollListener(new LiftOnScrollListener(root.findViewById(R.id.appbar)));
        subscriptionAdapter = new SubscriptionsRecyclerAdapter((MainActivity) getActivity()) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);
                MenuItemUtils.setOnClickListeners(menu, SubscriptionFragment.this::onContextItemSelected);
            }
        };
        setColumnNumber(prefs.getInt(PREF_NUM_COLUMNS, getDefaultNumOfColumns()));
        subscriptionAdapter.setOnSelectModeListener(this);
        subscriptionRecycler.setAdapter(subscriptionAdapter);
        setupEmptyView();

        progressBar = root.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        subscriptionAddButton = root.findViewById(R.id.subscriptions_add);
        subscriptionAddButton.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadChildFragment(new AddFeedFragment());
            }
        });

        feedsFilteredMsg = root.findViewById(R.id.feeds_filtered_message);
        feedsFilteredMsg.setOnClickListener((l) ->
                new SubscriptionsFilterDialog().show(getChildFragmentManager(), "filter"));
        boolean largePadding = displayUpArrow || !UserPreferences.isBottomNavigationEnabled();
        int paddingHorizontal = (int) (getResources().getDisplayMetrics().density * (largePadding ? 60 : 16));
        int paddingVertical = (int) (getResources().getDisplayMetrics().density * 4);
        feedsFilteredMsg.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setDistanceToTriggerSync(getResources().getInteger(R.integer.swipe_refresh_distance));
        swipeRefreshLayout.setOnRefreshListener(() -> FeedUpdateManager.getInstance().runOnceOrAsk(requireContext()));

        floatingSelectMenu = root.findViewById(R.id.floatingSelectMenu);
        floatingSelectMenu.inflate(R.menu.nav_feed_action_speeddial);
        floatingSelectMenu.setOnMenuItemClickListener(menuItem -> {
            new FeedMultiSelectActionHandler((MainActivity) getActivity(), subscriptionAdapter.getSelectedItems())
                    .handleAction(menuItem.getItemId());
            return true;
        });

        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    private void refreshToolbarState() {
        int columns = prefs.getInt(PREF_NUM_COLUMNS, getDefaultNumOfColumns());
        toolbar.getMenu().findItem(COLUMN_CHECKBOX_IDS[columns - MIN_NUM_COLUMNS]).setChecked(true);
        toolbar.getMenu().findItem(R.id.pref_show_subscription_title).setVisible(columns > 1);
        toolbar.getMenu().findItem(R.id.pref_show_subscription_title)
                .setChecked(UserPreferences.shouldShowSubscriptionTitle());
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedUpdateRunningEvent event) {
        swipeRefreshLayout.setRefreshing(event.isFeedUpdateRunning);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.refresh_item) {
            FeedUpdateManager.getInstance().runOnceOrAsk(requireContext());
            return true;
        } else if (itemId == R.id.subscriptions_filter) {
            new SubscriptionsFilterDialog().show(getChildFragmentManager(), "filter");
            return true;
        } else if (itemId == R.id.subscriptions_sort) {
            FeedSortDialog.showDialog(requireContext());
            return true;
        } else if (itemId == R.id.subscription_display_list) {
            setColumnNumber(1);
            return true;
        } else if (itemId == R.id.subscription_num_columns_2) {
            setColumnNumber(2);
            return true;
        } else if (itemId == R.id.subscription_num_columns_3) {
            setColumnNumber(3);
            return true;
        } else if (itemId == R.id.subscription_num_columns_4) {
            setColumnNumber(4);
            return true;
        } else if (itemId == R.id.subscription_num_columns_5) {
            setColumnNumber(5);
            return true;
        } else if (itemId == R.id.action_search) {
            ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance());
            return true;
        } else if (itemId == R.id.action_statistics) {
            ((MainActivity) getActivity()).loadChildFragment(new StatisticsFragment());
            return true;
        } else if (itemId == R.id.pref_show_subscription_title) {
            item.setChecked(!item.isChecked());
            UserPreferences.setShouldShowSubscriptionTitle(item.isChecked());
            subscriptionAdapter.notifyDataSetChanged();
        }
        return false;
    }

    private void setColumnNumber(int columns) {
        if (itemDecoration != null) {
            subscriptionRecycler.removeItemDecoration(itemDecoration);
            itemDecoration = null;
        }
        RecyclerView.LayoutManager layoutManager;
        if (columns == 1 && getDefaultNumOfColumns() == 5) { // Tablet
            layoutManager = new GridLayoutManager(getContext(), 2, RecyclerView.VERTICAL, false);
        } else if (columns == 1) {
            layoutManager = new GridLayoutManager(getContext(), 1, RecyclerView.VERTICAL, false);
        } else {
            layoutManager = new GridLayoutManager(getContext(), columns, RecyclerView.VERTICAL, false);
            itemDecoration = new SubscriptionsRecyclerAdapter.GridDividerItemDecorator();
            subscriptionRecycler.addItemDecoration(itemDecoration);
        }
        subscriptionAdapter.setColumnCount(columns);
        subscriptionRecycler.setLayoutManager(layoutManager);
        prefs.edit().putInt(PREF_NUM_COLUMNS, columns).apply();
        refreshToolbarState();
    }

    private void setupEmptyView() {
        emptyView = new EmptyViewHandler(getContext());
        emptyView.setIcon(R.drawable.ic_subscriptions);
        emptyView.setTitle(R.string.no_subscriptions_head_label);
        emptyView.setMessage(R.string.no_subscriptions_label);
        emptyView.attachToRecyclerView(subscriptionRecycler);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        loadSubscriptions();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
        if (subscriptionAdapter != null) {
            subscriptionAdapter.endSelectMode();
        }
    }

    private void loadSubscriptions() {
        if (disposable != null) {
            disposable.dispose();
        }
        emptyView.hide();
        disposable = Observable.fromCallable(
                () -> {
                    NavDrawerData data = DBReader.getNavDrawerData(UserPreferences.getSubscriptionsFilter(),
                            UserPreferences.getFeedOrder(), UserPreferences.getFeedCounterSetting());
                    List<NavDrawerData.DrawerItem> items = data.items;
                    for (NavDrawerData.DrawerItem item : items) {
                        if (item.type == NavDrawerData.DrawerItem.Type.TAG
                                && item.getTitle().equals(displayedFolder)) {
                            return ((NavDrawerData.TagDrawerItem) item).getChildren();
                        }
                    }
                    return items;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (listItems != null && listItems.size() > result.size()) {
                            // We have fewer items. This can result in items being selected that are no longer visible.
                            subscriptionAdapter.endSelectMode();
                        }
                        listItems = result;
                        progressBar.setVisibility(View.GONE);
                        subscriptionAdapter.setItems(result);
                        emptyView.updateVisibility();
                    }, error -> {
                        Log.e(TAG, Log.getStackTraceString(error));
                    });

        updateFilterVisibility();
    }

    private void updateFilterVisibility() {
        if (!UserPreferences.getSubscriptionsFilter().isEnabled()) {
            feedsFilteredMsg.setVisibility(View.GONE);
        } else if (subscriptionAdapter.inActionMode()) {
            feedsFilteredMsg.setVisibility(View.INVISIBLE);
        } else {
            feedsFilteredMsg.setVisibility(View.VISIBLE);
        }
    }

    private int getDefaultNumOfColumns() {
        return getResources().getInteger(R.integer.subscriptions_default_num_of_columns);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        NavDrawerData.DrawerItem drawerItem = subscriptionAdapter.getSelectedItem();
        if (drawerItem == null) {
            return false;
        }
        int itemId = item.getItemId();
        if (drawerItem.type == NavDrawerData.DrawerItem.Type.TAG) {
            if (itemId == R.id.rename_folder_item) {
                new RenameFeedDialog(getActivity(), drawerItem).show();
                return true;
            } else if (itemId == R.id.delete_folder_item) {
                ConfirmationDialog dialog = new ConfirmationDialog(
                        getContext(), R.string.delete_tag_label,
                        getString(R.string.delete_tag_confirmation, drawerItem.getTitle())) {

                    @Override
                    public void onConfirmButtonPressed(DialogInterface dialog) {
                        List<NavDrawerData.DrawerItem> feeds = ((NavDrawerData.TagDrawerItem) drawerItem).getChildren();

                        for (NavDrawerData.DrawerItem feed : feeds) {
                            FeedPreferences preferences = ((NavDrawerData.FeedDrawerItem) feed).feed.getPreferences();
                            preferences.getTags().remove(drawerItem.getTitle());
                            DBWriter.setFeedPreferences(preferences);
                        }
                    }
                };
                dialog.createNewDialog().show();

                return true;
            }
        }

        Feed feed = ((NavDrawerData.FeedDrawerItem) drawerItem).feed;
        if (itemId == R.id.multi_select) {
            return subscriptionAdapter.onContextItemSelected(item);
        }
        return FeedMenuHandler.onMenuItemClicked(this, item.getItemId(), feed, this::loadSubscriptions);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        loadSubscriptions();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadSubscriptions();
    }

    @Override
    public void onEndSelectMode() {
        floatingSelectMenu.setVisibility(View.GONE);
        subscriptionAddButton.setVisibility(View.VISIBLE);
        subscriptionAdapter.setItems(listItems);
        updateFilterVisibility();
    }

    @Override
    public void onStartSelectMode() {
        List<NavDrawerData.DrawerItem> feedsOnly = new ArrayList<>();
        for (NavDrawerData.DrawerItem item : listItems) {
            if (item.type == NavDrawerData.DrawerItem.Type.FEED) {
                feedsOnly.add(item);
            }
        }
        subscriptionAdapter.setItems(feedsOnly);
        floatingSelectMenu.setVisibility(View.VISIBLE);
        subscriptionAddButton.setVisibility(View.GONE);
        updateFilterVisibility();
    }
}
