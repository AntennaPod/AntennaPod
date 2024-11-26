package de.danoeh.antennapod.ui.episodeslist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import de.danoeh.antennapod.ui.screen.SearchFragment;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.ui.view.FloatingSelectMenu;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.ui.MenuItemUtils;
import de.danoeh.antennapod.event.EpisodeDownloadEvent;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.ui.swipeactions.SwipeActions;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.ui.view.EmptyViewHandler;
import de.danoeh.antennapod.ui.view.LiftOnScrollListener;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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
    protected boolean hasMoreItems = false;
    protected boolean displayUpArrow;

    protected EpisodeItemListRecyclerView recyclerView;
    protected EpisodeItemListAdapter listAdapter;
    protected EmptyViewHandler emptyView;
    protected FloatingSelectMenu floatingSelectMenu;
    protected MaterialToolbar toolbar;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected SwipeActions swipeActions;
    private ProgressBar progressBar;
    @NonNull
    protected List<FeedItem> episodes = new ArrayList<>();
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

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.refresh_item) {
            FeedUpdateManager.getInstance().runOnceOrAsk(requireContext());
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

        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        setupLoadMoreScrollListener();
        recyclerView.addOnScrollListener(new LiftOnScrollListener(root.findViewById(R.id.appbar)));

        swipeActions = new SwipeActions(this, getFragmentTag()).attachTo(recyclerView);
        swipeActions.setFilter(getFilter());

        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setDistanceToTriggerSync(getResources().getInteger(R.integer.swipe_refresh_distance));
        swipeRefreshLayout.setOnRefreshListener(() -> FeedUpdateManager.getInstance().runOnceOrAsk(requireContext()));

        listAdapter = new EpisodeItemListAdapter(getActivity()) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);
                if (!inActionMode()) {
                    menu.findItem(R.id.multi_select).setVisible(true);
                }
                MenuItemUtils.setOnClickListeners(menu, EpisodesListFragment.this::onContextItemSelected);
            }

            @Override
            protected void onSelectedItemsUpdated() {
                super.onSelectedItemsUpdated();
                FeedItemMenuHandler.onPrepareMenu(floatingSelectMenu.getMenu(), getSelectedItems());
                floatingSelectMenu.updateItemVisibility();
            }
        };
        listAdapter.setOnSelectModeListener(this);
        recyclerView.setAdapter(listAdapter);
        progressBar = root.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        emptyView = new EmptyViewHandler(getContext());
        emptyView.attachToRecyclerView(recyclerView);
        emptyView.setIcon(R.drawable.ic_feed);
        emptyView.setTitle(R.string.no_all_episodes_head_label);
        emptyView.setMessage(R.string.no_all_episodes_label);
        emptyView.updateAdapter(listAdapter);
        emptyView.hide();

        floatingSelectMenu = root.findViewById(R.id.floatingSelectMenu);
        floatingSelectMenu.inflate(R.menu.episodes_apply_action_speeddial);
        floatingSelectMenu.setOnMenuItemClickListener(menuItem -> {
            if (listAdapter.getSelectedCount() == 0) {
                ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.no_items_selected,
                        Snackbar.LENGTH_SHORT);
                return false;
            }
            int confirmationString = 0;
            if (listAdapter.getSelectedItems().size() >= 25 || listAdapter.shouldSelectLazyLoadedItems()) {
                // Should ask for confirmation
                if (menuItem.getItemId() == R.id.mark_read_item) {
                    confirmationString = R.string.multi_select_mark_played_confirmation;
                } else if (menuItem.getItemId() == R.id.mark_unread_item) {
                    confirmationString = R.string.multi_select_mark_unplayed_confirmation;
                }
            }
            if (confirmationString == 0) {
                performMultiSelectAction(menuItem.getItemId());
            } else {
                new ConfirmationDialog(getActivity(), R.string.multi_select, confirmationString) {
                    @Override
                    public void onConfirmButtonPressed(DialogInterface dialog) {
                        performMultiSelectAction(menuItem.getItemId());
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
        listAdapter.setDummyViews(1);
        listAdapter.notifyItemInserted(listAdapter.getItemCount() - 1);
        disposable = Observable.fromCallable(() -> loadMoreData(page))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        data -> {
                            if (data.size() < EPISODES_PER_PAGE) {
                                hasMoreItems = false;
                            }
                            episodes.addAll(data);
                            listAdapter.setDummyViews(0);
                            listAdapter.updateItems(episodes);
                            if (listAdapter.shouldSelectLazyLoadedItems()) {
                                listAdapter.setSelected(episodes.size() - data.size(), episodes.size(), true);
                            }
                        }, error -> {
                            listAdapter.setDummyViews(0);
                            listAdapter.updateItems(Collections.emptyList());
                            Log.e(TAG, Log.getStackTraceString(error));
                        }, () -> {
                            // Make sure to not always load 2 pages at once
                            recyclerView.post(() -> isLoadingMore = false);
                        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listAdapter.endSelectMode();
    }

    @Override
    public void onStartSelectMode() {
        floatingSelectMenu.setVisibility(View.VISIBLE);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                (int) getResources().getDimension(R.dimen.floating_select_menu_height));
    }

    @Override
    public void onEndSelectMode() {
        floatingSelectMenu.setVisibility(View.GONE);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(), 0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        for (FeedItem item : event.items) {
            int pos = FeedItemEvent.indexOfItemWithId(episodes, item.getId());
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
        for (int i = 0; i < listAdapter.getItemCount(); i++) {
            EpisodeItemViewHolder holder = (EpisodeItemViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null && holder.isCurrentlyPlayingItem()) {
                holder.notifyPlaybackPositionUpdated(event);
                break;
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
    public void onEventMainThread(EpisodeDownloadEvent event) {
        for (String downloadUrl : event.getUrls()) {
            int pos = EpisodeDownloadEvent.indexOfItemWithDownloadUrl(episodes, downloadUrl);
            if (pos >= 0) {
                listAdapter.notifyItemChangedCompat(pos);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        loadItems();
    }

    protected void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> new Pair<>(loadData(), loadTotalItemCount()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        data -> {
                            final boolean restoreScrollPosition = episodes.isEmpty();
                            episodes = data.first;
                            hasMoreItems = !(page == 1 && episodes.size() < EPISODES_PER_PAGE);
                            progressBar.setVisibility(View.GONE);
                            listAdapter.setDummyViews(0);
                            listAdapter.updateItems(episodes);
                            listAdapter.setTotalNumberOfItems(data.second);
                            if (restoreScrollPosition) {
                                recyclerView.restoreScrollPosition(getPrefName());
                            }
                            updateToolbar();
                        }, error -> {
                            listAdapter.setDummyViews(0);
                            listAdapter.updateItems(Collections.emptyList());
                            Log.e(TAG, Log.getStackTraceString(error));
                        });
    }

    @NonNull
    protected abstract List<FeedItem> loadData();

    @NonNull
    protected abstract List<FeedItem> loadMoreData(int page);

    protected abstract int loadTotalItemCount();

    protected abstract FeedItemFilter getFilter();

    protected abstract String getFragmentTag();

    protected abstract String getPrefName();

    protected void updateToolbar() {
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedUpdateRunningEvent event) {
        swipeRefreshLayout.setRefreshing(event.isFeedUpdateRunning);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }
}
