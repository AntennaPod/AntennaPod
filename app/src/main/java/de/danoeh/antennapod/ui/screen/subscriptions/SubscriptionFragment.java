package de.danoeh.antennapod.ui.screen.subscriptions;

import android.content.Context;
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
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.SubscriptionsFilter;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.NavDrawerData;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.MenuItemUtils;
import de.danoeh.antennapod.ui.screen.AddFeedFragment;
import de.danoeh.antennapod.ui.screen.SearchFragment;
import de.danoeh.antennapod.ui.statistics.StatisticsFragment;
import de.danoeh.antennapod.ui.view.EmptyViewHandler;
import de.danoeh.antennapod.ui.view.FloatingSelectMenu;
import de.danoeh.antennapod.ui.view.ItemOffsetDecoration;
import de.danoeh.antennapod.ui.view.LiftOnScrollListener;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for displaying feed subscriptions
 */
public class SubscriptionFragment extends Fragment
        implements MaterialToolbar.OnMenuItemClickListener,
        SubscriptionsRecyclerAdapter.OnSelectModeListener {
    public static final String TAG = "SubscriptionFragment";
    private static final String PREFS = "SubscriptionFragment";
    private static final String PREF_NUM_COLUMNS = "columns";
    private static final String PREF_LAST_TAG = "last_tag";
    private static final String KEY_UP_ARROW = "up_arrow";
    private static final String ARGUMENT_STATE = "state";

    private static final int MIN_NUM_COLUMNS = 1;
    private static final int[] COLUMN_CHECKBOX_IDS = {
            R.id.subscription_display_list,
            R.id.subscription_num_columns_2,
            R.id.subscription_num_columns_3,
            R.id.subscription_num_columns_4,
            R.id.subscription_num_columns_5};

    private RecyclerView subscriptionRecycler;
    private SubscriptionsRecyclerAdapter subscriptionAdapter;
    private RecyclerView tagsRecycler;
    private SubscriptionTagAdapter tagAdapter;
    private EmptyViewHandler emptyView;
    private View feedsFilteredMsg;
    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private CollapsingToolbarLayout collapsingContainer;
    private boolean displayUpArrow;
    private boolean shouldShowTags = false;

    private Disposable disposable;
    private SharedPreferences prefs;
    private static Pair<Integer, Integer> scrollPosition = null;

    private FloatingActionButton subscriptionAddButton;
    private FloatingSelectMenu floatingSelectMenu;
    private RecyclerView.ItemDecoration itemDecoration;
    private List<Feed> feeds;
    private int stateToShow = Feed.STATE_SUBSCRIBED;

    public static SubscriptionFragment newInstance(int state) {
        SubscriptionFragment fragment = new SubscriptionFragment();
        Bundle args = new Bundle();
        args.putInt(ARGUMENT_STATE, state);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (getArguments() != null) {
            stateToShow = getArguments().getInt(ARGUMENT_STATE, Feed.STATE_SUBSCRIBED);
        }
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

        collapsingContainer = root.findViewById(R.id.collapsing_container);
        subscriptionRecycler = root.findViewById(R.id.subscriptions_grid);
        registerForContextMenu(subscriptionRecycler);
        subscriptionRecycler.addOnScrollListener(new LiftOnScrollListener(root.findViewById(R.id.appbar)));
        subscriptionRecycler.addOnScrollListener(new LiftOnScrollListener(collapsingContainer));
        subscriptionAdapter = new SubscriptionsRecyclerAdapter((MainActivity) getActivity()) {
            @Override
            protected void onSelectedItemsUpdated() {
                super.onSelectedItemsUpdated();
                FeedMenuHandler.onPrepareMenu(floatingSelectMenu.getMenu(), getSelectedItems());
                floatingSelectMenu.updateItemVisibility();
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
        if (stateToShow == Feed.STATE_ARCHIVED) {
            toolbar.setTitle(R.string.archive_feed_label_noun);
            toolbar.getMenu().removeItem(R.id.subscriptions_filter);
            toolbar.getMenu().removeItem(R.id.refresh_item);
            toolbar.getMenu().removeItem(R.id.subscriptions_counter);
            toolbar.getMenu().removeItem(R.id.show_archive);
            floatingSelectMenu.getMenu().removeItem(R.id.keep_updated);
            floatingSelectMenu.getMenu().removeItem(R.id.notify_new_episodes);
            floatingSelectMenu.getMenu().removeItem(R.id.autodownload);
            floatingSelectMenu.getMenu().removeItem(R.id.autoDeleteDownload);
            floatingSelectMenu.getMenu().removeItem(R.id.playback_speed);
            subscriptionAddButton.setVisibility(View.GONE);
        }
        floatingSelectMenu.setOnMenuItemClickListener(menuItem -> {
            List<Feed> selection = subscriptionAdapter.getSelectedItems();
            new FeedMultiSelectActionHandler(getActivity(), selection)
                    .handleAction(menuItem.getItemId());
            if (selection.size() <= 1) {
                subscriptionAdapter.endSelectMode();
            }
            return true;
        });

        tagsRecycler = root.findViewById(R.id.tags_recycler);
        tagsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        tagsRecycler.addItemDecoration(new ItemOffsetDecoration(getContext(), 4, 0));
        registerForContextMenu(tagsRecycler);
        tagAdapter = new SubscriptionTagAdapter(getActivity()) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);
                MenuItemUtils.setOnClickListeners(menu, SubscriptionFragment.this::onTagContextItemSelected);
            }

            @Override
            protected void onTagClick(NavDrawerData.TagItem tag) {
                tagAdapter.setSelectedTag(tag.getTitle());
                loadSubscriptionsAndTags();
            }
        };
        if (stateToShow == Feed.STATE_SUBSCRIBED) {
            tagAdapter.setSelectedTag(prefs.getString(PREF_LAST_TAG, FeedPreferences.TAG_ROOT));
        } else {
            tagAdapter.setSelectedTag(FeedPreferences.TAG_ROOT);
        }
        tagsRecycler.setAdapter(tagAdapter);
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
        } else if (itemId == R.id.subscriptions_counter) {
            FeedCounterDialog.showDialog(requireContext());
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
            if (stateToShow == Feed.STATE_ARCHIVED) {
                ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstanceArchive());
            } else {
                ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance());
            }
            return true;
        } else if (itemId == R.id.action_statistics) {
            ((MainActivity) getActivity()).loadChildFragment(new StatisticsFragment());
            return true;
        } else if (itemId == R.id.pref_show_subscription_title) {
            item.setChecked(!item.isChecked());
            UserPreferences.setShouldShowSubscriptionTitle(item.isChecked());
            subscriptionAdapter.notifyDataSetChanged();
        } else if (itemId == R.id.show_archive) {
            Fragment fragment = SubscriptionFragment.newInstance(Feed.STATE_ARCHIVED);
            ((MainActivity) getActivity()).loadChildFragment(fragment);
            return true;
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
        if (stateToShow == Feed.STATE_ARCHIVED) {
            emptyView.setTitle(R.string.no_archive_head_label);
            emptyView.setMessage(R.string.no_archive_label);
        } else {
            emptyView.setTitle(R.string.no_subscriptions_head_label);
            emptyView.setMessage(R.string.no_subscriptions_label);
        }
        emptyView.attachToRecyclerView(subscriptionRecycler);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        loadSubscriptionsAndTags();
    }

    @Override
    public void onPause() {
        super.onPause();
        scrollPosition = getScrollPosition();
        if (stateToShow == Feed.STATE_SUBSCRIBED) {
            prefs.edit().putString(PREF_LAST_TAG, tagAdapter.getSelectedTag()).apply();
        }
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

    private void loadSubscriptionsAndTags() {
        if (disposable != null) {
            disposable.dispose();
        }
        SubscriptionsFilter filter = stateToShow == Feed.STATE_SUBSCRIBED
                ? UserPreferences.getSubscriptionsFilter() : new SubscriptionsFilter("");
        emptyView.hide();
        disposable = Observable.fromCallable(
                        () -> {
                            NavDrawerData navDrawerData = DBReader.getNavDrawerData(filter,
                                    UserPreferences.getFeedOrder(), UserPreferences.getFeedCounterSetting(),
                                    stateToShow);
                            List<NavDrawerData.TagItem> tags = DBReader.getAllTags(stateToShow);
                            return new Pair<>(navDrawerData, tags);
                        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        List<Feed> openedFolderFeeds = Collections.emptyList();
                        if (FeedPreferences.TAG_ROOT.equals(tagAdapter.getSelectedTag())) {
                            openedFolderFeeds = result.first.feeds;
                        } else {
                            boolean tagExists = false;
                            for (NavDrawerData.TagItem tag : result.first.tags) { // Filtered list
                                if (tag.getTitle().equals(tagAdapter.getSelectedTag())) {
                                    openedFolderFeeds = tag.getFeeds();
                                    tagExists = true;
                                    break;
                                }
                            }
                            if (!tagExists) {
                                tagAdapter.setSelectedTag(FeedPreferences.TAG_ROOT);
                                openedFolderFeeds = result.first.feeds;
                            }
                        }

                        final boolean firstLoaded = feeds == null || feeds.isEmpty();
                        if (feeds != null && feeds.size() > openedFolderFeeds.size()) {
                            // We have fewer items. This can result in items being selected that are no longer visible.
                            subscriptionAdapter.endSelectMode();
                        }
                        feeds = openedFolderFeeds;
                        progressBar.setVisibility(View.GONE);
                        subscriptionAdapter.setItems(feeds, result.first.feedCounters);
                        if (firstLoaded) {
                            restoreScrollPosition(scrollPosition);
                        }
                        emptyView.updateVisibility();
                        shouldShowTags = false;
                        if (tagAdapter != null) {
                            tagAdapter.setTags(result.second);
                            for (NavDrawerData.TagItem tag : result.second) {
                                if (!FeedPreferences.TAG_ROOT.equals(tag.getTitle())
                                        && !FeedPreferences.TAG_UNTAGGED.equals(tag.getTitle())) {
                                    shouldShowTags = true;
                                    break;
                                }
                            }
                            tagsRecycler.setVisibility(shouldShowTags ? View.VISIBLE : View.GONE);
                            // Scroll to center the selected tag
                            tagsRecycler.post(() -> {
                                int selectedPosition = tagAdapter.getSelectedTagPosition();
                                if (selectedPosition < 0) {
                                    return;
                                }
                                LinearLayoutManager layoutManager =
                                        (LinearLayoutManager) tagsRecycler.getLayoutManager();
                                // Calculate offset to center the selected chip
                                View selectedView = layoutManager.findViewByPosition(selectedPosition);
                                if (selectedView != null) {
                                    int recyclerWidth = tagsRecycler.getWidth();
                                    int chipWidth = selectedView.getWidth();
                                    int offset = (recyclerWidth - chipWidth) / 2;
                                    layoutManager.scrollToPositionWithOffset(selectedPosition, offset);
                                } else {
                                    tagsRecycler.scrollToPosition(selectedPosition);
                                }
                            });
                        }
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

    private boolean onTagContextItemSelected(MenuItem item) {
        NavDrawerData.TagItem selectedTag = tagAdapter.getLongPressedItem();
        if (selectedTag == null) {
            return false;
        }
        return TagMenuHandler.onMenuItemClicked(this, selectedTag, item, tagAdapter);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        loadSubscriptionsAndTags();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadSubscriptionsAndTags();
    }

    private void setCollapsingToolbarFlags(int flags) {
        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) collapsingContainer.getLayoutParams();
        params.setScrollFlags(flags);
        collapsingContainer.setLayoutParams(params);
    }

    @Override
    public void onEndSelectMode() {
        floatingSelectMenu.setVisibility(View.GONE);
        subscriptionAddButton.setVisibility(View.VISIBLE);
        tagsRecycler.setVisibility(shouldShowTags ? View.VISIBLE : View.GONE);
        updateFilterVisibility();
        setCollapsingToolbarFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                | AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED);
    }

    @Override
    public void onStartSelectMode() {
        floatingSelectMenu.setVisibility(View.VISIBLE);
        subscriptionAddButton.setVisibility(View.GONE);
        tagsRecycler.setVisibility(shouldShowTags ? View.INVISIBLE : View.GONE);
        updateFilterVisibility();
        setCollapsingToolbarFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                | AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED);
    }

    public Pair<Integer, Integer> getScrollPosition() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) subscriptionRecycler.getLayoutManager();
        int firstItem = layoutManager.findFirstVisibleItemPosition();
        View firstItemView = layoutManager.findViewByPosition(firstItem);
        int topOffset = firstItemView == null ? 0 : firstItemView.getTop();
        return new Pair<>(firstItem, topOffset);
    }

    public void restoreScrollPosition(Pair<Integer, Integer> scrollPosition) {
        if (scrollPosition == null || (scrollPosition.first == 0 && scrollPosition.second == 0)) {
            return;
        }
        LinearLayoutManager layoutManager = (LinearLayoutManager) subscriptionRecycler.getLayoutManager();
        layoutManager.scrollToPositionWithOffset(scrollPosition.first, scrollPosition.second);
    }
}
