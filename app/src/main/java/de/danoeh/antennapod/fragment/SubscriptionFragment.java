package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.joanzapata.iconify.Iconify;
import com.leinardi.android.speeddial.SpeedDialView;

import de.danoeh.antennapod.dialog.TagSettingsDialog;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.SubscriptionsRecyclerAdapter;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.dialog.FeedSortDialog;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
import de.danoeh.antennapod.dialog.RenameItemDialog;
import de.danoeh.antennapod.dialog.SubscriptionsFilterDialog;
import de.danoeh.antennapod.fragment.actions.FeedMultiSelectActionHandler;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Fragment for displaying feed subscriptions
 */
public class SubscriptionFragment extends Fragment
        implements Toolbar.OnMenuItemClickListener,
        SubscriptionsRecyclerAdapter.OnSelectModeListener {
    public static final String TAG = "SubscriptionFragment";
    private static final String PREFS = "SubscriptionFragment";
    private static final String PREF_NUM_COLUMNS = "columns";
    private static final String KEY_UP_ARROW = "up_arrow";
    private static final String ARGUMENT_FOLDER = "folder";

    private static final int MIN_NUM_COLUMNS = 2;
    private static final int[] COLUMN_CHECKBOX_IDS = {
            R.id.subscription_num_columns_2,
            R.id.subscription_num_columns_3,
            R.id.subscription_num_columns_4,
            R.id.subscription_num_columns_5};

    private RecyclerView subscriptionRecycler;
    private SubscriptionsRecyclerAdapter subscriptionAdapter;
    private FloatingActionButton subscriptionAddButton;
    private ProgressBar progressBar;
    private EmptyViewHandler emptyView;
    private TextView feedsFilteredMsg;
    private Toolbar toolbar;
    private String displayedFolder = null;
    private boolean isUpdatingFeeds = false;
    private boolean displayUpArrow;

    private Disposable disposable;
    private SharedPreferences prefs;

    private SpeedDialView speedDialView;

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
        setRetainInstance(true);

        prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_subscriptions, container, false);
        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this);
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar, displayUpArrow);
        toolbar.inflateMenu(R.menu.subscriptions);
        for (int i = 0; i < COLUMN_CHECKBOX_IDS.length; i++) {
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
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(),
                prefs.getInt(PREF_NUM_COLUMNS, getDefaultNumOfColumns()),
                RecyclerView.VERTICAL,
                false);
        subscriptionRecycler.setLayoutManager(gridLayoutManager);
        subscriptionRecycler.addItemDecoration(new SubscriptionsRecyclerAdapter.GridDividerItemDecorator());
        gridLayoutManager.setSpanCount(prefs.getInt(PREF_NUM_COLUMNS, getDefaultNumOfColumns()));
        registerForContextMenu(subscriptionRecycler);
        subscriptionAddButton = root.findViewById(R.id.subscriptions_add);
        progressBar = root.findViewById(R.id.progLoading);

        feedsFilteredMsg = root.findViewById(R.id.feeds_filtered_message);
        feedsFilteredMsg.setOnClickListener((l) -> SubscriptionsFilterDialog.showDialog(requireContext()));

        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setDistanceToTriggerSync(getResources().getInteger(R.integer.swipe_refresh_distance));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            AutoUpdateManager.runImmediate(requireContext());
            new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false),
                    getResources().getInteger(R.integer.swipe_to_refresh_duration_in_ms));
        });

        speedDialView = root.findViewById(R.id.fabSD);
        speedDialView.setOverlayLayout(root.findViewById(R.id.fabSDOverlay));
        speedDialView.inflate(R.menu.nav_feed_action_speeddial);
        speedDialView.setOnChangeListener(new SpeedDialView.OnChangeListener() {
            @Override
            public boolean onMainActionSelected() {
                return false;
            }

            @Override
            public void onToggleChanged(boolean isOpen) {
            }
        });
        speedDialView.setOnActionSelectedListener(actionItem -> {
            new FeedMultiSelectActionHandler((MainActivity) getActivity(), subscriptionAdapter.getSelectedItems())
                    .handleAction(actionItem.getId());
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

        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(toolbar.getMenu(),
                R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext());
            return true;
        } else if (itemId == R.id.subscriptions_filter) {
            SubscriptionsFilterDialog.showDialog(requireContext());
            return true;
        } else if (itemId == R.id.subscriptions_sort) {
            FeedSortDialog.showDialog(requireContext());
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
        }
        return false;
    }

    private void setColumnNumber(int columns) {
        GridLayoutManager gridLayoutManager = (GridLayoutManager) subscriptionRecycler.getLayoutManager();
        gridLayoutManager.setSpanCount(columns);
        subscriptionAdapter.notifyDataSetChanged();
        prefs.edit().putInt(PREF_NUM_COLUMNS, columns).apply();
        refreshToolbarState();
    }

    private void setupEmptyView() {
        emptyView = new EmptyViewHandler(getContext());
        emptyView.setIcon(R.drawable.ic_folder);
        emptyView.setTitle(R.string.no_subscriptions_head_label);
        emptyView.setMessage(R.string.no_subscriptions_label);
        emptyView.attachToRecyclerView(subscriptionRecycler);
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        subscriptionAdapter = new SubscriptionsRecyclerAdapter((MainActivity) getActivity());
        subscriptionAdapter.setOnSelectModeListener(this);
        subscriptionRecycler.setAdapter(subscriptionAdapter);
        setupEmptyView();
        subscriptionAddButton.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadChildFragment(new AddFeedFragment());
            }
        });
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
                    NavDrawerData data = DBReader.getNavDrawerData();
                    List<NavDrawerData.DrawerItem> items = data.items;
                    for (NavDrawerData.DrawerItem item : items) {
                        if (item.type == NavDrawerData.DrawerItem.Type.TAG
                                && item.getTitle().equals(displayedFolder)) {
                            return ((NavDrawerData.TagDrawerItem) item).children;
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
                        subscriptionAdapter.setItems(result);
                        subscriptionAdapter.notifyDataSetChanged();
                        emptyView.updateVisibility();
                        progressBar.setVisibility(View.GONE); // Keep hidden to avoid flickering while refreshing
                    }, error -> {
                        Log.e(TAG, Log.getStackTraceString(error));
                        progressBar.setVisibility(View.GONE);
                    });

        if (UserPreferences.getSubscriptionsFilter().isEnabled()) {
            feedsFilteredMsg.setText("{md-info-outline} " + getString(R.string.subscriptions_are_filtered));
            Iconify.addIcons(feedsFilteredMsg);
            feedsFilteredMsg.setVisibility(View.VISIBLE);
        } else {
            feedsFilteredMsg.setVisibility(View.GONE);
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
        if (drawerItem.type == NavDrawerData.DrawerItem.Type.TAG && itemId == R.id.rename_folder_item) {
            new RenameItemDialog(getActivity(), drawerItem).show();
            return true;
        }

        Feed feed = ((NavDrawerData.FeedDrawerItem) drawerItem).feed;
        if (itemId == R.id.remove_all_new_flags_item) {
            displayConfirmationDialog(
                    R.string.remove_all_new_flags_label,
                    R.string.remove_all_new_flags_confirmation_msg,
                    () -> DBWriter.removeFeedNewFlag(feed.getId()));
            return true;
        } else if (itemId == R.id.edit_tags) {
            TagSettingsDialog.newInstance(Collections.singletonList(feed.getPreferences()))
                    .show(getChildFragmentManager(), TagSettingsDialog.TAG);
            return true;
        } else if (itemId == R.id.rename_item) {
            new RenameItemDialog(getActivity(), feed).show();
            return true;
        } else if (itemId == R.id.remove_item) {
            RemoveFeedDialog.show(getContext(), feed);
            return true;
        } else if (itemId == R.id.multi_select) {
            speedDialView.setVisibility(View.VISIBLE);
            return subscriptionAdapter.onContextItemSelected(item);
        }
        return super.onContextItemSelected(item);
    }

    private <T> void displayConfirmationDialog(@StringRes int title, @StringRes int message, Callable<? extends T> task) {
        ConfirmationDialog dialog = new ConfirmationDialog(getActivity(), title, message) {
            @Override
            @SuppressLint("CheckResult")
            public void onConfirmButtonPressed(DialogInterface clickedDialog) {
                clickedDialog.dismiss();
                Observable.fromCallable(task)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> loadSubscriptions(),
                                error -> Log.e(TAG, Log.getStackTraceString(error)));
            }
        };
        dialog.createNewDialog().show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        loadSubscriptions();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadSubscriptions();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            refreshToolbarState();
        }
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadService.isDownloadingFeeds();

    @Override
    public void onEndSelectMode() {
        speedDialView.close();
        speedDialView.setVisibility(View.GONE);
        subscriptionAdapter.setItems(listItems);
        subscriptionAdapter.notifyDataSetChanged();
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
        subscriptionAdapter.notifyDataSetChanged();
    }
}
