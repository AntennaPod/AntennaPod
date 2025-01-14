package de.danoeh.antennapod.ui.screen.download;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemListAdapter;
import de.danoeh.antennapod.actionbutton.DeleteActionButton;
import de.danoeh.antennapod.event.DownloadLogEvent;
import de.danoeh.antennapod.ui.MenuItemUtils;
import de.danoeh.antennapod.ui.screen.SearchFragment;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.screen.feed.ItemSortDialog;
import de.danoeh.antennapod.event.EpisodeDownloadEvent;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.ui.episodeslist.EpisodeMultiSelectActionHandler;
import de.danoeh.antennapod.ui.swipeactions.SwipeActions;
import de.danoeh.antennapod.ui.episodeslist.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.view.EmptyViewHandler;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemListRecyclerView;
import de.danoeh.antennapod.ui.view.FloatingSelectMenu;
import de.danoeh.antennapod.ui.view.LiftOnScrollListener;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemViewHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Displays all completed downloads and provides a button to delete them.
 */
public class CompletedDownloadsFragment extends Fragment
        implements EpisodeItemListAdapter.OnSelectModeListener, MaterialToolbar.OnMenuItemClickListener {
    public static final String TAG = "DownloadsFragment";
    public static final String ARG_SHOW_LOGS = "show_logs";
    private static final String KEY_UP_ARROW = "up_arrow";

    private Set<String> runningDownloads = new HashSet<>();
    private List<FeedItem> items = new ArrayList<>();
    private CompletedDownloadsListAdapter adapter;
    private EpisodeItemListRecyclerView recyclerView;
    private Disposable disposable;
    private EmptyViewHandler emptyView;
    private boolean displayUpArrow;
    private FloatingSelectMenu floatingSelectMenu;
    private SwipeActions swipeActions;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.simple_list_fragment, container, false);
        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.downloads_label);
        toolbar.inflateMenu(R.menu.downloads_completed);
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

        swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setDistanceToTriggerSync(getResources().getInteger(R.integer.swipe_refresh_distance));
        swipeRefreshLayout.setOnRefreshListener(() -> FeedUpdateManager.getInstance().runOnceOrAsk(requireContext()));

        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        adapter = new CompletedDownloadsListAdapter((MainActivity) getActivity());
        adapter.setOnSelectModeListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new LiftOnScrollListener(root.findViewById(R.id.appbar)));
        swipeActions = new SwipeActions(this, TAG).attachTo(recyclerView);
        swipeActions.setFilter(new FeedItemFilter(FeedItemFilter.DOWNLOADED));

        progressBar = root.findViewById(R.id.progLoading);
        progressBar.setVisibility(View.VISIBLE);

        floatingSelectMenu = root.findViewById(R.id.floatingSelectMenu);
        floatingSelectMenu.inflate(R.menu.episodes_apply_action_speeddial);
        floatingSelectMenu.setOnMenuItemClickListener(menuItem -> {
            if (adapter.getSelectedCount() == 0) {
                ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.no_items_selected,
                        Snackbar.LENGTH_SHORT);
                return false;
            }
            new EpisodeMultiSelectActionHandler(getActivity(), menuItem.getItemId())
                    .handleAction(adapter.getSelectedItems());
            adapter.endSelectMode();
            return true;
        });
        if (getArguments() != null && getArguments().getBoolean(ARG_SHOW_LOGS, false)) {
            new DownloadLogFragment().show(getChildFragmentManager(), null);
        }

        addEmptyView();
        EventBus.getDefault().register(this);
        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        adapter.endSelectMode();
        if (toolbar != null) {
            toolbar.setOnMenuItemClickListener(null);
            toolbar.setOnLongClickListener(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadItems();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.refresh_item) {
            FeedUpdateManager.getInstance().runOnceOrAsk(requireContext());
            return true;
        } else if (item.getItemId() == R.id.action_download_logs) {
            new DownloadLogFragment().show(getChildFragmentManager(), null);
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance());
            return true;
        } else if (item.getItemId() == R.id.downloads_sort) {
            new DownloadsSortDialog().show(getChildFragmentManager(), "SortDialog");
            return true;
        } else if (item.getItemId() == R.id.action_delete_downloads_played) {
            ConfirmationDialog dialog = new ConfirmationDialog(getActivity(),
                    R.string.delete_downloads_played,  R.string.delete_downloads_played_confirmation) {
                @Override
                public void onConfirmButtonPressed(DialogInterface clickedDialog) {
                    clickedDialog.dismiss();
                    Observable.fromCallable(() -> DBReader.getEpisodes(0, Integer.MAX_VALUE,
                                    new FeedItemFilter(FeedItemFilter.DOWNLOADED, FeedItemFilter.INCLUDE_NOT_SUBSCRIBED,
                                            FeedItemFilter.PLAYED), SortOrder.DATE_OLD_NEW))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(items -> new EpisodeMultiSelectActionHandler(getActivity(), R.id.remove_item)
                                    .handleAction(items), error -> Log.e(TAG, Log.getStackTraceString(error)));
                }
            };
            dialog.createNewDialog().show();
            return true;
        }
        return false;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EpisodeDownloadEvent event) {
        Set<String> newRunningDownloads = new HashSet<>();
        for (String url : event.getUrls()) {
            if (DownloadServiceInterface.get().isDownloadingEpisode(url)) {
                newRunningDownloads.add(url);
            }
        }
        if (!newRunningDownloads.equals(runningDownloads)) {
            runningDownloads = newRunningDownloads;
            loadItems();
            return; // Refreshed anyway
        }
        for (String downloadUrl : event.getUrls()) {
            int pos = EpisodeDownloadEvent.indexOfItemWithDownloadUrl(items, downloadUrl);
            if (pos >= 0) {
                adapter.notifyItemChangedCompat(pos);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        FeedItem selectedItem = adapter.getLongPressedItem();
        if (selectedItem == null) {
            Log.i(TAG, "Selected item at current position was null, ignoring selection");
            return super.onContextItemSelected(item);
        }
        if (adapter.onContextItemSelected(item)) {
            return true;
        }

        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedItem);
    }

    private void addEmptyView() {
        emptyView = new EmptyViewHandler(getActivity());
        emptyView.setIcon(R.drawable.ic_download);
        emptyView.setTitle(R.string.no_comp_downloads_head_label);
        emptyView.setMessage(R.string.no_comp_downloads_label);
        emptyView.attachToRecyclerView(recyclerView);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (items == null) {
            return;
        } else if (adapter == null) {
            loadItems();
            return;
        }
        for (int i = 0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemEvent.indexOfItemWithId(items, item.getId());
            if (pos >= 0) {
                items.remove(pos);
                if (item.getMedia().isDownloaded()) {
                    items.add(pos, item);
                    adapter.notifyItemChangedCompat(pos);
                } else {
                    adapter.notifyItemRemoved(pos);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (adapter != null) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                EpisodeItemViewHolder holder = (EpisodeItemViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null && holder.isCurrentlyPlayingItem()) {
                    holder.notifyPlaybackPositionUpdated(event);
                    break;
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadLogChanged(DownloadLogEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadItems();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedUpdateRunningEvent event) {
        swipeRefreshLayout.setRefreshing(event.isFeedUpdateRunning);
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        emptyView.hide();
        disposable = Observable.fromCallable(() -> {
            SortOrder sortOrder = UserPreferences.getDownloadsSortedOrder();
            List<FeedItem> downloadedItems = DBReader.getEpisodes(0, Integer.MAX_VALUE,
                    new FeedItemFilter(FeedItemFilter.DOWNLOADED, FeedItemFilter.INCLUDE_NOT_SUBSCRIBED), sortOrder);

            List<String> mediaUrls = new ArrayList<>();
            if (runningDownloads == null) {
                return downloadedItems;
            }
            for (String url : runningDownloads) {
                if (EpisodeDownloadEvent.indexOfItemWithDownloadUrl(downloadedItems, url) != -1) {
                    continue; // Already in list
                }
                mediaUrls.add(url);
            }
            List<FeedItem> currentDownloads = DBReader.getFeedItemsWithUrl(mediaUrls);
            currentDownloads.addAll(downloadedItems);
            return currentDownloads;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
                result -> {
                    items = result;
                    adapter.setDummyViews(0);
                    progressBar.setVisibility(View.GONE);
                    adapter.updateItems(result);
                }, error -> {
                    adapter.setDummyViews(0);
                    adapter.updateItems(Collections.emptyList());
                    Log.e(TAG, Log.getStackTraceString(error));
                });
    }

    @Override
    public void onStartSelectMode() {
        swipeActions.detach();
        floatingSelectMenu.setVisibility(View.VISIBLE);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                (int) getResources().getDimension(R.dimen.floating_select_menu_height));
    }

    @Override
    public void onEndSelectMode() {
        floatingSelectMenu.setVisibility(View.GONE);
        swipeActions.attachTo(recyclerView);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(), 0);
    }

    private class CompletedDownloadsListAdapter extends EpisodeItemListAdapter {

        public CompletedDownloadsListAdapter(MainActivity mainActivity) {
            super(mainActivity);
        }

        @Override
        public void afterBindViewHolder(EpisodeItemViewHolder holder, int pos) {
            if (!inActionMode()) {
                if (holder.getFeedItem().isDownloaded()
                        && !UserPreferences.shouldDownloadsButtonActionPlay()) {
                    DeleteActionButton actionButton = new DeleteActionButton(getItem(pos));
                    actionButton.configure(holder.secondaryActionButton, holder.secondaryActionIcon, getActivity());
                }
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            if (!inActionMode()) {
                menu.findItem(R.id.multi_select).setVisible(true);
            }
            MenuItemUtils.setOnClickListeners(menu, CompletedDownloadsFragment.this::onContextItemSelected);
        }

        @Override
        protected void onSelectedItemsUpdated() {
            super.onSelectedItemsUpdated();
            FeedItemMenuHandler.onPrepareMenu(floatingSelectMenu.getMenu(), getSelectedItems());
            floatingSelectMenu.updateItemVisibility();
        }
    }

    public static class DownloadsSortDialog extends ItemSortDialog {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            sortOrder = UserPreferences.getDownloadsSortedOrder();
        }

        @Override
        protected void onAddItem(int title, SortOrder ascending, SortOrder descending, boolean ascendingIsDefault) {
            if (ascending == SortOrder.DATE_OLD_NEW || ascending == SortOrder.DURATION_SHORT_LONG
                    || ascending == SortOrder.EPISODE_TITLE_A_Z || ascending == SortOrder.SIZE_SMALL_LARGE) {
                super.onAddItem(title, ascending, descending, ascendingIsDefault);
            }
        }

        @Override
        protected void onSelectionChanged() {
            super.onSelectionChanged();
            UserPreferences.setDownloadsSortedOrder(sortOrder);
            EventBus.getDefault().post(DownloadLogEvent.listUpdated());
        }
    }
}
