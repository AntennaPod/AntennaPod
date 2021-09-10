package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.joanzapata.iconify.Iconify;
import com.leinardi.android.speeddial.SpeedDialView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.FeedTagAdapter;
import de.danoeh.antennapod.adapter.SubscriptionsRecyclerAdapter;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.FeedListUpdateEvent;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.core.feed.TagFilter;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.dialog.FeedSortDialog;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
import de.danoeh.antennapod.dialog.RenameFeedDialog;
import de.danoeh.antennapod.dialog.SubscriptionsFilterDialog;
import de.danoeh.antennapod.dialog.TagSettingsDialog;
import de.danoeh.antennapod.fragment.actions.FeedMultiSelectActionHandler;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.util.FeedSorter;
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
    public static final String PREF_TAG_FILTER = "prefTagFilter";
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

    private List<NavDrawerData.DrawerItem> tagFilteredFeeds;
    private NavDrawerData.FolderDrawerItem rootFolder;
    private RecyclerView tagRecycler;
    private FeedTagAdapter feedTagAdapter;
    private ChipGroup folderChipGroup;

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

        Button expandTagsButton = root.findViewById(R.id.expandTagsButton);
        expandTagsButton.setOnClickListener(v -> {
            if(folderChipGroup.getVisibility() == View.GONE) {
                folderChipGroup.setVisibility(View.VISIBLE);
            } else {
                folderChipGroup.setVisibility(View.GONE);
            }
        });

        tagRecycler = root.findViewById(R.id.tagRecycler);
        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
        tagRecycler.setLayoutManager(linearLayoutManager);

        folderChipGroup = root.findViewById(R.id.feedChipGroup);


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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
                        if (item.type == NavDrawerData.DrawerItem.Type.FOLDER
                                && item.getTitle().equals(displayedFolder)) {
                            return ((NavDrawerData.FolderDrawerItem) item).children;
                        }
                    }
                    return items;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            Pair<List<NavDrawerData.DrawerItem>,
                                    List<NavDrawerData.FolderDrawerItem>> feedsAndTags =
                                    extractFeedsAndTags(result);
                            tagFilteredFeeds = feedsAndTags.first;
                            List<NavDrawerData.FolderDrawerItem> tags = feedsAndTags.second;

                            initTagViews(tags);

                            subscriptionAdapter.setItems(sortFeeds(tagFilteredFeeds));
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

    private void initTagViews(List<NavDrawerData.FolderDrawerItem> tags) {
        feedTagAdapter = new FeedTagAdapter(getContext(), new ArrayList<>());
        Set<String> tagFilterIds = getTagFilterIds();

        for (NavDrawerData.FolderDrawerItem folder : tags) {
            if (tagFilterIds.contains(String.valueOf(folder.id))) {
                feedTagAdapter.addItem(folder);
            }
        }

        tagRecycler.setAdapter(feedTagAdapter);

        initTagChipView(tags, tagFilterIds);
    }

    public Pair<List<NavDrawerData.DrawerItem>, List<NavDrawerData.FolderDrawerItem>> extractFeedsAndTags(List<NavDrawerData.DrawerItem> drawerItems) {
        List<NavDrawerData.DrawerItem> feeds = new ArrayList<>();
        List<NavDrawerData.FolderDrawerItem> tags = new ArrayList<>();
        for (NavDrawerData.DrawerItem drawerItem : drawerItems) {
            if (drawerItem.type.equals(NavDrawerData.DrawerItem.Type.FOLDER)) {
                tags.add((NavDrawerData.FolderDrawerItem) drawerItem);
                if (((NavDrawerData.FolderDrawerItem) drawerItem).name.equals(FeedPreferences.TAG_ROOT)) {
                    rootFolder = (NavDrawerData.FolderDrawerItem) drawerItem;
                }
            } else {
                feeds.add(drawerItem);
            }
        }

        List<NavDrawerData.DrawerItem> tagFilteredFeeds = getTagFilteredFeeds(tags);

        Pair<List<NavDrawerData.DrawerItem>, List<NavDrawerData.FolderDrawerItem>> feedsAndTags = new Pair(tagFilteredFeeds, tags);
        return feedsAndTags;
    }

    private List<NavDrawerData.DrawerItem> getTagFilteredFeeds(List<NavDrawerData.FolderDrawerItem> tags) {
        Set<String> tagFilterIds = getTagFilterIds();
        TagFilter tagFilter = new TagFilter(tagFilterIds);
        List<NavDrawerData.DrawerItem> tagFilteredFeeds = tagFilter.filter(tags);

        return tagFilteredFeeds;
    }

    private void initTagChipView(List<NavDrawerData.FolderDrawerItem> feedFolders, Set<String> tagFilterIds) {
        Chip rootChip = null;
        folderChipGroup.removeAllViews();
        for (NavDrawerData.FolderDrawerItem folderItem : feedFolders) {
            Chip folderChip = new Chip(getActivity());
            if (folderItem.name.equals(FeedPreferences.TAG_ROOT)) {
                folderChip.setText("All");
                rootChip = folderChip;
            } else {
                folderChip.setText(folderItem.name);
//                folderChip.setChipIcon(getResources().getDrawable(android.R.drawable.ic_input_add));
//                folderChip.setCheckedIcon(getResources().getDrawable(android.R.drawable.ic_delete));
            }
            folderChip.setCheckable(true);
            Chip finalRootChip = rootChip;
            folderChip.setOnClickListener(v ->  {
                tagChipOnClickListener(folderItem, folderChip, finalRootChip);
            });

            folderChip.setChecked(tagFilterIds.contains(String.valueOf(folderItem.id)));

            folderChipGroup.addView(folderChip);
        }
    }

    private void tagChipOnClickListener(NavDrawerData.FolderDrawerItem folderItem, Chip folderChip, Chip finalRootChip) {
        if (folderItem.name.equals(FeedPreferences.TAG_ROOT)) {
            if (folderChip.isChecked()) {
                feedTagAdapter.clear();
                folderChipGroup.clearCheck();
                activateAllChip(folderChip, true);
                updateDisplayedSubscriptions(false);
            }
        } else {
            if (folderChip.isChecked()) {
                addTagFilterId(folderItem.id);
                feedTagAdapter.addItem(folderItem);
            } else {
                removeTagFilterId(folderItem.id);
                feedTagAdapter.removeItem(folderItem);
            }

            boolean tagsSelected = !feedTagAdapter.isEmpyty();
            updateDisplayedSubscriptions(tagsSelected);
            activateAllChip(finalRootChip, !tagsSelected);
        }
    }

    private void updateDisplayedSubscriptions(boolean tagsSelected) {
        if (tagsSelected)  {
            Set<NavDrawerData.DrawerItem> allChildren = new HashSet<>();
            for (NavDrawerData.FolderDrawerItem item : feedTagAdapter.getFeedFolders()) {
                allChildren.addAll(item.children);
            }
            tagFilteredFeeds = new ArrayList(allChildren);
        } else {
            tagFilteredFeeds = new ArrayList(rootFolder.children);
        }
        subscriptionAdapter.setItems(sortFeeds(tagFilteredFeeds));
    }

    private int getDefaultNumOfColumns() {
        return getResources().getInteger(R.integer.subscriptions_default_num_of_columns);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Feed feed = subscriptionAdapter.getSelectedFeed();
        if (feed == null) {
            return false;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.remove_all_new_flags_item) {
            displayConfirmationDialog(
                    R.string.remove_all_new_flags_label,
                    R.string.remove_all_new_flags_confirmation_msg,
                    () -> DBWriter.removeFeedNewFlag(feed.getId()));
            return true;
        } else if (itemId == R.id.add_to_folder) {
            TagSettingsDialog.newInstance(feed.getPreferences()).show(getChildFragmentManager(), TagSettingsDialog.TAG);
        } else if (itemId == R.id.mark_all_read_item) { displayConfirmationDialog(
                    R.string.mark_all_read_label,
                    R.string.mark_all_read_confirmation_msg,
                    () -> DBWriter.markFeedRead(feed.getId()));
            return true;
        } else if (itemId == R.id.rename_item) {
            new RenameFeedDialog(getActivity(), feed).show();
            return true;
        } else if (itemId == R.id.remove_item) {
            RemoveFeedDialog.show(getContext(), feed, null);
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
            () -> DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds();

    @Override
    public void onEndSelectMode() {
        speedDialView.close();
        speedDialView.setVisibility(View.GONE);
        subscriptionAdapter.setItems(tagFilteredFeeds);
    }

    @Override
    public void onStartSelectMode() {
        List<NavDrawerData.DrawerItem> feedsOnly = new ArrayList<>();
        for (NavDrawerData.DrawerItem item : tagFilteredFeeds) {
            if (item.type == NavDrawerData.DrawerItem.Type.FEED) {
                feedsOnly.add(item);
            }
        }
        subscriptionAdapter.setItems(feedsOnly);
    }

    private List<NavDrawerData.DrawerItem> sortFeeds(List<NavDrawerData.DrawerItem> items) {
        return FeedSorter.sortFeeds(items);
    }

    private void activateAllChip(Chip chip, boolean enabled) {
        chip.setChecked(enabled);
        chip.setEnabled(!enabled);
    }
    public Set<String> getTagFilterIds() {
        return prefs.getStringSet(PREF_TAG_FILTER, new HashSet<>());
    }
    public void addTagFilterId(long tagFilterId) {
        Set<String> tagFilterIds = new HashSet<>(prefs.getStringSet(PREF_TAG_FILTER, new HashSet<>()));
        tagFilterIds.add(String.valueOf(tagFilterId));
        prefs.edit().putStringSet(PREF_TAG_FILTER, null).apply();
        prefs.edit().putStringSet(PREF_TAG_FILTER, tagFilterIds)
                .apply();
    }

    public void removeTagFilterId(long tagFilterId) {
        Set<String> tagFilterIds = new HashSet<>(prefs.getStringSet(PREF_TAG_FILTER, new HashSet<>()));
        tagFilterIds.remove(String.valueOf(tagFilterId));
        prefs.edit().putStringSet(PREF_TAG_FILTER, null).apply();
        prefs.edit().putStringSet(PREF_TAG_FILTER, tagFilterIds)
                .apply();
    }
}
