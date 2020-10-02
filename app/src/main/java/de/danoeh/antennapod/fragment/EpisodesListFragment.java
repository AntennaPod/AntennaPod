package de.danoeh.antennapod.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.event.FeedListUpdateEvent;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.event.PlayerStatusEvent;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.view.EpisodeItemListRecyclerView;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Shows unread or recently published episodes
 */
public abstract class EpisodesListFragment extends Fragment {

    public static final String TAG = "EpisodesListFragment";
    protected static final int EPISODES_PER_PAGE = 150;
    protected int page = 1;
    protected boolean isLoadingMore = false;
    protected boolean hasMoreItems = true;

    EpisodeItemListRecyclerView recyclerView;
    EpisodeItemListAdapter listAdapter;
    ProgressBar progLoading;
    View loadingMoreView;
    EmptyViewHandler emptyView;

    @NonNull
    List<FeedItem> episodes = new ArrayList<>();

    private volatile boolean isUpdatingFeeds;
    private boolean isMenuVisible = true;
    protected Disposable disposable;
    protected TextView txtvInformation;

    String getPrefName() {
        return TAG;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        loadItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(true);
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
            () -> DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds();

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        isMenuVisible = visible;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.episodes, menu);
        MenuItemUtils.setupSearchItem(menu, (MainActivity) getActivity(), 0);
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            switch (item.getItemId()) {
                case R.id.refresh_item:
                    AutoUpdateManager.runImmediate(requireContext());
                    return true;
                case R.id.mark_all_read_item:
                    ConfirmationDialog markAllReadConfirmationDialog = new ConfirmationDialog(getActivity(),
                            R.string.mark_all_read_label,
                            R.string.mark_all_read_confirmation_msg) {

                        @Override
                        public void onConfirmButtonPressed(DialogInterface dialog) {
                            dialog.dismiss();
                            DBWriter.markAllItemsRead();
                            ((MainActivity) getActivity()).showSnackbarAbovePlayer(
                                    R.string.mark_all_read_msg, Toast.LENGTH_SHORT);
                        }
                    };
                    markAllReadConfirmationDialog.createNewDialog().show();
                    return true;
                case R.id.remove_all_new_flags_item:
                    ConfirmationDialog removeAllNewFlagsConfirmationDialog = new ConfirmationDialog(getActivity(),
                            R.string.remove_all_new_flags_label,
                            R.string.remove_all_new_flags_confirmation_msg) {

                        @Override
                        public void onConfirmButtonPressed(DialogInterface dialog) {
                            dialog.dismiss();
                            DBWriter.removeAllNewFlags();
                            ((MainActivity) getActivity()).showSnackbarAbovePlayer(
                                    R.string.removed_all_new_flags_msg, Toast.LENGTH_SHORT);
                        }
                    };
                    removeAllNewFlagsConfirmationDialog.createNewDialog().show();
                    return true;
                default:
                    return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onContextItemSelected() called with: " + "item = [" + item + "]");
        if (!getUserVisibleHint()) {
            return false;
        }
        if (!isVisible()) {
            return false;
        }
        if (item.getItemId() == R.id.share_item) {
            return true; // avoids that the position is reset when we need it in the submenu
        }

        if (listAdapter.getSelectedItem() == null) {
            Log.i(TAG, "Selected item or listAdapter was null, ignoring selection");
            return super.onContextItemSelected(item);
        }
        FeedItem selectedItem = listAdapter.getSelectedItem();

        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedItem);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.all_episodes_fragment, container, false);
        txtvInformation = root.findViewById(R.id.txtvInformation);

        recyclerView = root.findViewById(android.R.id.list);
        recyclerView.setVisibility(View.GONE);
        recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        setupLoadMoreScrollListener();

        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            AutoUpdateManager.runImmediate(requireContext());
            new Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false),
                    getResources().getInteger(R.integer.swipe_to_refresh_duration_in_ms));
        });

        progLoading = root.findViewById(R.id.progLoading);
        progLoading.setVisibility(View.VISIBLE);
        loadingMoreView = root.findViewById(R.id.loadingMore);

        emptyView = new EmptyViewHandler(getContext());
        emptyView.attachToRecyclerView(recyclerView);
        emptyView.setIcon(R.attr.feed);
        emptyView.setTitle(R.string.no_all_episodes_head_label);
        emptyView.setMessage(R.string.no_all_episodes_label);

        createRecycleAdapter(recyclerView, emptyView);
        emptyView.hide();

        return root;
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
        disposable = Observable.fromCallable(this::loadMoreData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    if (data.size() < EPISODES_PER_PAGE) {
                        hasMoreItems = false;
                    }
                    episodes.addAll(data);
                    onFragmentLoaded(episodes);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)),
                    () -> {
                        recyclerView.post(() -> isLoadingMore = false); // Make sure to not always load 2 pages at once
                        progLoading.setVisibility(View.GONE);
                        loadingMoreView.setVisibility(View.GONE);
                    });
    }

    protected void onFragmentLoaded(List<FeedItem> episodes) {
        boolean restoreScrollPosition = listAdapter.getItemCount() == 0;
        if (episodes.size() == 0) {
            createRecycleAdapter(recyclerView, emptyView);
        } else {
            listAdapter.updateItems(episodes);
        }
        if (restoreScrollPosition) {
            recyclerView.restoreScrollPosition(getPrefName());
        }
        if (isMenuVisible && isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing()) {
            requireActivity().invalidateOptionsMenu();
        }
    }

    /**
     * Currently, we need to recreate the list adapter in order to be able to undo last item via the
     * snackbar. See #3084 for details.
     */
    private void createRecycleAdapter(RecyclerView recyclerView, EmptyViewHandler emptyViewHandler) {
        MainActivity mainActivity = (MainActivity) getActivity();
        listAdapter = new EpisodeItemListAdapter(mainActivity);
        listAdapter.updateItems(episodes);
        recyclerView.setAdapter(listAdapter);
        emptyViewHandler.updateAdapter(listAdapter);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        for (FeedItem item : event.items) {
            int pos = FeedItemUtil.indexOfItemWithId(episodes, item.getId());
            if (pos >= 0) {
                episodes.remove(pos);
                if (shouldUpdatedItemRemainInList(item)) {
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

    protected boolean shouldUpdatedItemRemainInList(FeedItem item) {
        return true;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        if (isMenuVisible && event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            requireActivity().invalidateOptionsMenu();
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
        if (isMenuVisible && isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing()) {
            requireActivity().invalidateOptionsMenu();
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
        disposable = Observable.fromCallable(this::loadData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    progLoading.setVisibility(View.GONE);
                    hasMoreItems = true;
                    episodes = data;
                    onFragmentLoaded(episodes);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    protected abstract List<FeedItem> loadData();

    @NonNull
    protected abstract List<FeedItem> loadMoreData();
}
