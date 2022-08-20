package de.danoeh.antennapod.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;
import com.leinardi.android.speeddial.SpeedDialView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.fragment.actions.EpisodeMultiSelectActionHandler;
import de.danoeh.antennapod.fragment.swipeactions.SwipeActions;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.view.EmptyViewHandler;
import de.danoeh.antennapod.view.EpisodeItemListRecyclerView;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows unread or recently published episodes
 */
public abstract class EpisodesListFragment extends Fragment
        implements EpisodeItemListAdapter.OnSelectModeListener, Toolbar.OnMenuItemClickListener {
    public static final String TAG = "EpisodesListFragment";
    private static final String KEY_UP_ARROW = "up_arrow";
    protected static final int EPISODES_PER_PAGE = 150;
    protected int page = 1;
    protected boolean isLoadingMore = false;
    protected boolean hasMoreItems = true;
    private boolean displayUpArrow;

    EpisodeItemListRecyclerView recyclerView;
    EpisodeItemListAdapter listAdapter;
    ProgressBar progLoading;
    View loadingMoreView;
    EmptyViewHandler emptyView;
    SpeedDialView speedDialView;
    Toolbar toolbar;
    SwipeActions swipeActions;

    @NonNull
    List<FeedItem> episodes = new ArrayList<>();

    private volatile boolean isUpdatingFeeds;
    protected Disposable disposable;
    protected TextView txtvInformation;


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        loadItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerForContextMenu(recyclerView);
    }

    @Override
    public void onPause() {
        super.onPause();
        recyclerView.saveScrollPosition(getPrefName());
        unregisterForContextMenu(recyclerView);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadService.isDownloadingFeeds();

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }
        final int itemId = item.getItemId();
        if (itemId == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext());
            return true;
        } else if (itemId == R.id.action_search) {
            ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance());
            return true;
        }
        return false;
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onContextItemSelected() called with: " + "item = [" + item + "]");
        if (!getUserVisibleHint() || !isVisible() || !isMenuVisible()) {
            // The method is called on all fragments in a ViewPager, so this needs to be ignored in invisible ones.
            // Apparently, none of the visibility check method works reliably on its own, so we just use all.
            return false;
        } else if (listAdapter.getLongPressedItem() == null) {
            Log.i(TAG, "Selected item or listAdapter was null, ignoring selection");
            return super.onContextItemSelected(item);
        } else if (listAdapter.onContextItemSelected(item)) {
            return true;
        }
        FeedItem selectedItem = listAdapter.getLongPressedItem();
        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedItem);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.episodes_list_fragment, container, false);
        txtvInformation = root.findViewById(R.id.txtvInformation);
        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setOnLongClickListener(v -> {
            recyclerView.scrollToPosition(5);
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(0));
            return false;
        });
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar, displayUpArrow);

        recyclerView = root.findViewById(android.R.id.list);
        recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        setupLoadMoreScrollListener();

        swipeActions = new SwipeActions(this, getFragmentTag()).attachTo(recyclerView);
        swipeActions.setFilter(getFilter());

        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setDistanceToTriggerSync(getResources().getInteger(R.integer.swipe_refresh_distance));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            AutoUpdateManager.runImmediate(requireContext());
            new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false),
                    getResources().getInteger(R.integer.swipe_to_refresh_duration_in_ms));
        });

        progLoading = root.findViewById(R.id.progLoading);
        progLoading.setVisibility(View.VISIBLE);
        loadingMoreView = root.findViewById(R.id.loadingMore);

        emptyView = new EmptyViewHandler(getContext());
        emptyView.attachToRecyclerView(recyclerView);
        emptyView.setIcon(R.drawable.ic_feed);
        emptyView.setTitle(R.string.no_all_episodes_head_label);
        emptyView.setMessage(R.string.no_all_episodes_label);

        createRecycleAdapter(recyclerView, emptyView);
        emptyView.hide();

        speedDialView = root.findViewById(R.id.fabSD);
        speedDialView.setOverlayLayout(root.findViewById(R.id.fabSDOverlay));
        speedDialView.inflate(R.menu.episodes_apply_action_speeddial);
        speedDialView.setOnChangeListener(new SpeedDialView.OnChangeListener() {
            @Override
            public boolean onMainActionSelected() {
                return false;
            }

            @Override
            public void onToggleChanged(boolean open) {
                if (open && listAdapter.getSelectedCount() == 0) {
                    ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.no_items_selected,
                            Snackbar.LENGTH_SHORT);
                    speedDialView.close();
                }
            }
        });
        speedDialView.setOnActionSelectedListener(actionItem -> {
            int confirmationString = 0;
            if (listAdapter.getSelectedItems().size() >= 25 || listAdapter.shouldSelectLazyLoadedItems()) {
                // Should ask for confirmation
                if (actionItem.getId() == R.id.mark_read_batch) {
                    confirmationString = R.string.multi_select_mark_played_confirmation;
                } else if (actionItem.getId() == R.id.mark_unread_batch) {
                    confirmationString = R.string.multi_select_mark_unplayed_confirmation;
                }
            }
            if (confirmationString == 0) {
                performMultiSelectAction(actionItem.getId());
            } else {
                new ConfirmationDialog(getActivity(), R.string.multi_select, confirmationString) {
                    @Override
                    public void onConfirmButtonPressed(DialogInterface dialog) {
                        performMultiSelectAction(actionItem.getId());
                    }
                }.createNewDialog().show();
            }
            return true;
        });

        return root;
    }

    private void performMultiSelectAction(int actionItemId) {
        EpisodeMultiSelectActionHandler handler =
                new EpisodeMultiSelectActionHandler(((MainActivity) getActivity()), actionItemId);
        Completable.fromAction(
                () -> {
                    handler.handleAction(listAdapter.getSelectedItems());
                    if (listAdapter.shouldSelectLazyLoadedItems()) {
                        int applyPage = page + 1;
                        List<FeedItem> nextPage;
                        do {
                            nextPage = loadMoreData(applyPage);
                            handler.handleAction(nextPage);
                            applyPage++;
                        } while (nextPage.size() == EPISODES_PER_PAGE);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> listAdapter.endSelectMode(),
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void setupLoadMoreScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView view, int deltaX, int deltaY) {
                super.onScrolled(view, deltaX, deltaY);
                if (!isLoadingMore && hasMoreItems && recyclerView.isScrolledToBottom()) {
                    /* The end of the list has been reached. Load more data. */
                    page++;
                    loadMoreItems();
                    isLoadingMore = true;
                }
            }
        });
    }

    private void loadMoreItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        isLoadingMore = true;
        loadingMoreView.setVisibility(View.VISIBLE);
        disposable = Observable.fromCallable(() -> loadMoreData(page))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    if (data.size() < EPISODES_PER_PAGE) {
                        hasMoreItems = false;
                    }
                    episodes.addAll(data);
                    updateAdapterWithNewItems();
                    if (listAdapter.shouldSelectLazyLoadedItems()) {
                        listAdapter.setSelected(episodes.size() - data.size(), episodes.size(), true);
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)),
                    () -> {
                        recyclerView.post(() -> isLoadingMore = false); // Make sure to not always load 2 pages at once
                        progLoading.setVisibility(View.GONE);
                        loadingMoreView.setVisibility(View.GONE);
                    });
    }

    protected void updateAdapterWithNewItems() {
        boolean restoreScrollPosition = listAdapter.getItemCount() == 0;
        if (episodes.size() == 0) {
            createRecycleAdapter(recyclerView, emptyView);
        } else {
            listAdapter.updateItems(episodes);
        }
        if (restoreScrollPosition) {
            recyclerView.restoreScrollPosition(getPrefName());
        }
    }

    /**
     * Currently, we need to recreate the list adapter in order to be able to undo last item via the
     * snackbar. See #3084 for details.
     */
    private void createRecycleAdapter(RecyclerView recyclerView, EmptyViewHandler emptyViewHandler) {
        MainActivity mainActivity = (MainActivity) getActivity();
        listAdapter = new EpisodeItemListAdapter(mainActivity) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);
                if (!inActionMode()) {
                    menu.findItem(R.id.multi_select).setVisible(true);
                }
                MenuItemUtils.setOnClickListeners(menu, EpisodesListFragment.this::onContextItemSelected);
            }
        };
        listAdapter.setOnSelectModeListener(this);
        listAdapter.updateItems(episodes);
        recyclerView.setAdapter(listAdapter);
        emptyViewHandler.updateAdapter(listAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listAdapter != null) {
            listAdapter.endSelectMode();
        }
        listAdapter = null;
    }

    @Override
    public void onStartSelectMode() {
        speedDialView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onEndSelectMode() {
        speedDialView.close();
        speedDialView.setVisibility(View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        for (FeedItem item : event.items) {
            int pos = FeedItemUtil.indexOfItemWithId(episodes, item.getId());
            if (pos >= 0) {
                episodes.remove(pos);
                if (getFilter().matches(item)) {
                    episodes.add(pos, item);
                    listAdapter.notifyItemChangedCompat(pos);
                } else {
                    listAdapter.notifyItemRemoved(pos);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (listAdapter != null) {
            for (int i = 0; i < listAdapter.getItemCount(); i++) {
                EpisodeItemViewHolder holder = (EpisodeItemViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null && holder.isCurrentlyPlayingItem()) {
                    holder.notifyPlaybackPositionUpdated(event);
                    break;
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onKeyUp(KeyEvent event) {
        if (!isAdded() || !isVisible() || !isMenuVisible()) {
            return;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_T:
                recyclerView.smoothScrollToPosition(0);
                break;
            case KeyEvent.KEYCODE_B:
                recyclerView.smoothScrollToPosition(listAdapter.getItemCount() - 1);
                break;
            default:
                break;
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            updateToolbar();
        }
        if (update.mediaIds.length > 0) {
            for (long mediaId : update.mediaIds) {
                int pos = FeedItemUtil.indexOfItemWithMediaId(episodes, mediaId);
                if (pos >= 0) {
                    listAdapter.notifyItemChangedCompat(pos);
                }
            }
        }
    }

    private void updateUi() {
        loadItems();
        if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing()) {
            updateToolbar();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        updateUi();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        updateUi();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        updateUi();
    }

    void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> new Pair<>(loadData(), loadTotalItemCount()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    progLoading.setVisibility(View.GONE);
                    loadingMoreView.setVisibility(View.GONE);
                    hasMoreItems = true;
                    episodes = data.first;
                    listAdapter.notifyDataSetChanged();
                    listAdapter.setTotalNumberOfItems(data.second);
                    updateAdapterWithNewItems();
                    updateToolbar();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    protected List<FeedItem> loadData() {
        return DBReader.getRecentlyPublishedEpisodes(0, page * EPISODES_PER_PAGE, getFilter());
    }

    @NonNull
    protected List<FeedItem> loadMoreData(int page) {
        return DBReader.getRecentlyPublishedEpisodes((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE, getFilter());
    }

    protected int loadTotalItemCount() {
        return DBReader.getTotalEpisodeCount(getFilter());
    }

    protected abstract FeedItemFilter getFilter();

    protected abstract String getFragmentTag();

    protected abstract String getPrefName();

    protected void updateToolbar() {
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(toolbar.getMenu(),
                R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }
}
