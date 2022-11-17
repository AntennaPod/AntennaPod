package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.snackbar.Snackbar;
import com.leinardi.android.speeddial.SpeedDialView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.adapter.actionbutton.DeleteActionButton;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloadLogEvent;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.event.FeedItemEvent;
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
import de.danoeh.antennapod.view.LiftOnScrollListener;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Displays all completed downloads and provides a button to delete them.
 */
public class CompletedDownloadsFragment extends Fragment
        implements EpisodeItemListAdapter.OnSelectModeListener, MaterialToolbar.OnMenuItemClickListener {
    public static final String TAG = "DownloadsFragment";
    public static final String ARG_SHOW_LOGS = "show_logs";
    private static final String KEY_UP_ARROW = "up_arrow";
    private static final String PREF_PREVIOUS_EPISODE_COUNT = "episodeCount";

    private long[] runningDownloads = new long[0];
    private List<FeedItem> items = new ArrayList<>();
    private CompletedDownloadsListAdapter adapter;
    private EpisodeItemListRecyclerView recyclerView;
    private Disposable disposable;
    private EmptyViewHandler emptyView;
    private boolean displayUpArrow;
    private SpeedDialView speedDialView;
    private SwipeActions swipeActions;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.simple_list_fragment, container, false);
        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
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

        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        adapter = new CompletedDownloadsListAdapter((MainActivity) getActivity());
        adapter.setOnSelectModeListener(this);
        int previousEpisodesCount = getContext().getSharedPreferences(TAG, Context.MODE_PRIVATE)
                .getInt(PREF_PREVIOUS_EPISODE_COUNT, 5);
        adapter.setDummyViews(Math.max(1, previousEpisodesCount));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new LiftOnScrollListener(root.findViewById(R.id.appbar)));
        swipeActions = new SwipeActions(this, TAG).attachTo(recyclerView);
        swipeActions.setFilter(new FeedItemFilter(FeedItemFilter.DOWNLOADED));

        speedDialView = root.findViewById(R.id.fabSD);
        speedDialView.setOverlayLayout(root.findViewById(R.id.fabSDOverlay));
        speedDialView.inflate(R.menu.episodes_apply_action_speeddial);
        speedDialView.removeActionItemById(R.id.download_batch);
        speedDialView.removeActionItemById(R.id.mark_read_batch);
        speedDialView.removeActionItemById(R.id.mark_unread_batch);
        speedDialView.removeActionItemById(R.id.remove_from_queue_batch);
        speedDialView.removeActionItemById(R.id.remove_all_inbox_item);
        speedDialView.setOnChangeListener(new SpeedDialView.OnChangeListener() {
            @Override
            public boolean onMainActionSelected() {
                return false;
            }

            @Override
            public void onToggleChanged(boolean open) {
                if (open && adapter.getSelectedCount() == 0) {
                    ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.no_items_selected,
                            Snackbar.LENGTH_SHORT);
                    speedDialView.close();
                }
            }
        });
        speedDialView.setOnActionSelectedListener(actionItem -> {
            new EpisodeMultiSelectActionHandler(((MainActivity) getActivity()), actionItem.getId())
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
        getContext().getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
                .putInt(PREF_PREVIOUS_EPISODE_COUNT, adapter.getItemCount())
                .apply();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext());
            return true;
        } else if (item.getItemId() == R.id.action_download_logs) {
            new DownloadLogFragment().show(getChildFragmentManager(), null);
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance());
            return true;
        }
        return false;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (!Arrays.equals(event.update.mediaIds, runningDownloads)) {
            runningDownloads = event.update.mediaIds;
            loadItems();
            return; // Refreshed anyway
        }
        if (event.update.mediaIds.length > 0) {
            for (long mediaId : event.update.mediaIds) {
                int pos = FeedItemUtil.indexOfItemWithMediaId(items, mediaId);
                if (pos >= 0) {
                    adapter.notifyItemChangedCompat(pos);
                }
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
            int pos = FeedItemUtil.indexOfItemWithId(items, item.getId());
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

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        emptyView.hide();
        disposable = Observable.fromCallable(() -> {
            List<FeedItem> downloadedItems = DBReader.getDownloadedItems();
            List<Long> mediaIds = new ArrayList<>();
            if (runningDownloads == null) {
                return downloadedItems;
            }
            for (long id : runningDownloads) {
                if (FeedItemUtil.indexOfItemWithMediaId(downloadedItems, id) != -1) {
                    continue; // Already in list
                }
                mediaIds.add(id);
            }
            List<FeedItem> currentDownloads = DBReader.getFeedItemsWithMedia(mediaIds.toArray(new Long[0]));
            currentDownloads.addAll(downloadedItems);
            return currentDownloads;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
                result -> {
                    items = result;
                    adapter.setDummyViews(0);
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
        speedDialView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onEndSelectMode() {
        speedDialView.close();
        speedDialView.setVisibility(View.GONE);
        swipeActions.attachTo(recyclerView);
    }

    private class CompletedDownloadsListAdapter extends EpisodeItemListAdapter {

        public CompletedDownloadsListAdapter(MainActivity mainActivity) {
            super(mainActivity);
        }

        @Override
        public void afterBindViewHolder(EpisodeItemViewHolder holder, int pos) {
            if (!inActionMode()) {
                if (holder.getFeedItem().isDownloaded()) {
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
    }
}
