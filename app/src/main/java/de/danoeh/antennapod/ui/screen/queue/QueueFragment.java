package de.danoeh.antennapod.ui.screen.queue;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import de.danoeh.antennapod.event.playback.SpeedChangedEvent;
import de.danoeh.antennapod.ui.screen.SearchFragment;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.ui.episodes.PlaybackSpeedUtils;
import de.danoeh.antennapod.ui.view.FloatingSelectMenu;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemListAdapter;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.ui.MenuItemUtils;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.screen.feed.ItemSortDialog;
import de.danoeh.antennapod.event.EpisodeDownloadEvent;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.QueueEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.ui.episodeslist.EpisodeMultiSelectActionHandler;
import de.danoeh.antennapod.ui.swipeactions.SwipeActions;
import de.danoeh.antennapod.ui.episodeslist.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.view.EmptyViewHandler;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemListRecyclerView;
import de.danoeh.antennapod.ui.view.LiftOnScrollListener;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemViewHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Shows all items in the queue.
 */
public class QueueFragment extends Fragment implements MaterialToolbar.OnMenuItemClickListener,
        EpisodeItemListAdapter.OnSelectModeListener {
    public static final String TAG = "QueueFragment";
    private static final String KEY_UP_ARROW = "up_arrow";

    private TextView infoBar;
    private EpisodeItemListRecyclerView recyclerView;
    private QueueRecyclerAdapter recyclerAdapter;
    private EmptyViewHandler emptyView;
    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean displayUpArrow;

    private List<FeedItem> queue;

    private static final String PREFS = "QueueFragment";
    private static final String PREF_SHOW_LOCK_WARNING = "show_lock_warning";

    private Disposable disposable;
    private SwipeActions swipeActions;
    private SharedPreferences prefs;

    private FloatingSelectMenu floatingSelectMenu;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (queue != null) {
            recyclerView.restoreScrollPosition(QueueFragment.TAG);
        }
        loadItems(true);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        recyclerView.saveScrollPosition(QueueFragment.TAG);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(QueueEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (queue == null) {
            return;
        } else if (recyclerAdapter == null) {
            loadItems(true);
            return;
        }
        switch(event.action) {
            case ADDED:
                queue.add(event.position, event.item);
                recyclerAdapter.notifyItemInserted(event.position);
                break;
            case SET_QUEUE:
            case SORTED: //Deliberate fall-through
                queue = event.items;
                recyclerAdapter.updateItems(event.items);
                break;
            case REMOVED:
            case IRREVERSIBLE_REMOVED:
                int position = FeedItemEvent.indexOfItemWithId(queue, event.item.getId());
                queue.remove(position);
                recyclerAdapter.notifyItemRemoved(position);
                break;
            case CLEARED:
                queue.clear();
                recyclerAdapter.updateItems(queue);
                break;
            case MOVED:
                return;
        }
        recyclerAdapter.updateDragDropEnabled();
        refreshToolbarState();
        recyclerView.saveScrollPosition(QueueFragment.TAG);
        refreshInfoBar();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (queue == null) {
            return;
        } else if (recyclerAdapter == null) {
            loadItems(true);
            return;
        }
        for (int i = 0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemEvent.indexOfItemWithId(queue, item.getId());
            if (pos >= 0) {
                queue.remove(pos);
                queue.add(pos, item);
                recyclerAdapter.notifyItemChangedCompat(pos);
                refreshInfoBar();
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EpisodeDownloadEvent event) {
        if (queue == null) {
            return;
        }
        for (String downloadUrl : event.getUrls()) {
            int pos = EpisodeDownloadEvent.indexOfItemWithDownloadUrl(queue, downloadUrl);
            if (pos >= 0) {
                recyclerAdapter.notifyItemChangedCompat(pos);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (recyclerAdapter != null) {
            for (int i = 0; i < recyclerAdapter.getItemCount(); i++) {
                EpisodeItemViewHolder holder = (EpisodeItemViewHolder)
                        recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null && holder.isCurrentlyPlayingItem()) {
                    holder.notifyPlaybackPositionUpdated(event);
                    break;
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems(false);
        refreshToolbarState();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        // Sent when playback position is reset
        loadItems(false);
        refreshToolbarState();
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
                recyclerView.smoothScrollToPosition(recyclerAdapter.getItemCount() - 1);
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recyclerAdapter != null) {
            recyclerAdapter.endSelectMode();
        }
        recyclerAdapter = null;
        if (toolbar != null) {
            toolbar.setOnMenuItemClickListener(null);
            toolbar.setOnLongClickListener(null);
        }
    }

    private void refreshToolbarState() {
        boolean keepSorted = UserPreferences.isQueueKeepSorted();
        toolbar.getMenu().findItem(R.id.queue_lock).setChecked(UserPreferences.isQueueLocked());
        toolbar.getMenu().findItem(R.id.queue_lock).setVisible(!keepSorted);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedUpdateRunningEvent event) {
        swipeRefreshLayout.setRefreshing(event.isFeedUpdateRunning);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateSpeed(SpeedChangedEvent event) {
        refreshInfoBar();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.queue_lock) {
            toggleQueueLock();
            return true;
        } else if (itemId == R.id.queue_sort) {
            new QueueSortDialog().show(getChildFragmentManager().beginTransaction(), "SortDialog");
            return true;
        } else if (itemId == R.id.refresh_item) {
            FeedUpdateManager.getInstance().runOnceOrAsk(requireContext());
            return true;
        } else if (itemId == R.id.clear_queue) {
            // make sure the user really wants to clear the queue
            ConfirmationDialog conDialog = new ConfirmationDialog(getActivity(),
                    R.string.clear_queue_label,
                    R.string.clear_queue_confirmation_msg) {

                @Override
                public void onConfirmButtonPressed(
                        DialogInterface dialog) {
                    dialog.dismiss();
                    DBWriter.clearQueue();
                }
            };
            conDialog.createNewDialog().show();
            return true;
        } else if (itemId == R.id.action_search) {
            ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance());
            return true;
        }
        return false;
    }

    private void toggleQueueLock() {
        boolean isLocked = UserPreferences.isQueueLocked();
        if (isLocked) {
            setQueueLocked(false);
        } else {
            boolean shouldShowLockWarning = prefs.getBoolean(PREF_SHOW_LOCK_WARNING, true);
            if (!shouldShowLockWarning) {
                setQueueLocked(true);
            } else {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
                builder.setTitle(R.string.lock_queue);
                builder.setMessage(R.string.queue_lock_warning);

                View view = View.inflate(getContext(), R.layout.checkbox_do_not_show_again, null);
                CheckBox checkDoNotShowAgain = view.findViewById(R.id.checkbox_do_not_show_again);
                builder.setView(view);

                builder.setPositiveButton(R.string.lock_queue, (dialog, which) -> {
                    prefs.edit().putBoolean(PREF_SHOW_LOCK_WARNING, !checkDoNotShowAgain.isChecked()).apply();
                    setQueueLocked(true);
                });
                builder.setNegativeButton(R.string.cancel_label, null);
                builder.show();
            }
        }
    }

    private void setQueueLocked(boolean locked) {
        UserPreferences.setQueueLocked(locked);
        refreshToolbarState();
        if (recyclerAdapter != null) {
            recyclerAdapter.updateDragDropEnabled();
        }
        if (queue.size() == 0) {
            if (locked) {
                ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.queue_locked, Snackbar.LENGTH_SHORT);
            } else {
                ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.queue_unlocked, Snackbar.LENGTH_SHORT);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d(TAG, "onContextItemSelected() called with: " + "item = [" + item + "]");
        if (!isVisible() || recyclerAdapter == null) {
            return false;
        }
        FeedItem selectedItem = recyclerAdapter.getLongPressedItem();
        if (selectedItem == null) {
            Log.i(TAG, "Selected item was null, ignoring selection");
            return super.onContextItemSelected(item);
        }

        int position = FeedItemEvent.indexOfItemWithId(queue, selectedItem.getId());
        if (position < 0) {
            Log.i(TAG, "Selected item no longer exist, ignoring selection");
            return super.onContextItemSelected(item);
        }
        if (recyclerAdapter.onContextItemSelected(item)) {
            return true;
        }

        final int itemId = item.getItemId();
        if (itemId == R.id.move_to_top_item) {
            queue.add(0, queue.remove(position));
            recyclerAdapter.notifyItemMoved(position, 0);
            DBWriter.moveQueueItemToTop(selectedItem.getId(), true);
            return true;
        } else if (itemId == R.id.move_to_bottom_item) {
            queue.add(queue.size() - 1, queue.remove(position));
            recyclerAdapter.notifyItemMoved(position, queue.size() - 1);
            DBWriter.moveQueueItemToBottom(selectedItem.getId(), true);
            return true;
        }
        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedItem);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.queue_fragment, container, false);
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
        toolbar.inflateMenu(R.menu.queue);
        refreshToolbarState();
        progressBar = root.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        infoBar = root.findViewById(R.id.info_bar);
        boolean largePadding = displayUpArrow || !UserPreferences.isBottomNavigationEnabled();
        int paddingHorizontal = (int) (getResources().getDisplayMetrics().density * (largePadding ? 60 : 16));
        infoBar.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);

        recyclerView = root.findViewById(R.id.recyclerView);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        registerForContextMenu(recyclerView);
        recyclerView.addOnScrollListener(new LiftOnScrollListener(root.findViewById(R.id.appbar)));

        swipeActions = new QueueSwipeActions();
        swipeActions.setFilter(new FeedItemFilter(FeedItemFilter.QUEUED));
        swipeActions.attachTo(recyclerView);

        recyclerAdapter = new QueueRecyclerAdapter((MainActivity) getActivity(), swipeActions) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);
                MenuItemUtils.setOnClickListeners(menu, QueueFragment.this::onContextItemSelected);
            }

            @Override
            protected void onSelectedItemsUpdated() {
                super.onSelectedItemsUpdated();
                FeedItemMenuHandler.onPrepareMenu(floatingSelectMenu.getMenu(), getSelectedItems(),
                        R.id.add_to_queue_item, R.id.remove_inbox_item);
                floatingSelectMenu.updateItemVisibility();
            }
        };
        recyclerAdapter.setOnSelectModeListener(this);
        recyclerView.setAdapter(recyclerAdapter);

        swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setDistanceToTriggerSync(getResources().getInteger(R.integer.swipe_refresh_distance));
        swipeRefreshLayout.setOnRefreshListener(() -> FeedUpdateManager.getInstance().runOnceOrAsk(requireContext()));

        emptyView = new EmptyViewHandler(getContext());
        emptyView.attachToRecyclerView(recyclerView);
        emptyView.setIcon(R.drawable.ic_playlist_play);
        emptyView.setTitle(R.string.no_items_header_label);
        emptyView.setMessage(R.string.no_items_label);
        emptyView.updateAdapter(recyclerAdapter);

        floatingSelectMenu = root.findViewById(R.id.floatingSelectMenu);
        floatingSelectMenu.inflate(R.menu.episodes_apply_action_speeddial);
        floatingSelectMenu.setOnMenuItemClickListener(menuItem -> {
            if (recyclerAdapter.getSelectedCount() == 0) {
                ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.no_items_selected,
                        Snackbar.LENGTH_SHORT);
                return false;
            }
            new EpisodeMultiSelectActionHandler(getActivity(), menuItem.getItemId())
                    .handleAction(recyclerAdapter.getSelectedItems());
            recyclerAdapter.endSelectMode();
            return true;
        });
        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    private void refreshInfoBar() {
        String info = getResources().getQuantityString(R.plurals.num_episodes, queue.size(), queue.size());
        if (!queue.isEmpty()) {
            long timeLeft = 0;
            for (FeedItem item : queue) {
                float playbackSpeed = 1;
                if (UserPreferences.timeRespectsSpeed()) {
                    playbackSpeed = PlaybackSpeedUtils.getCurrentPlaybackSpeed(item.getMedia());
                }
                if (item.getMedia() != null) {
                    long itemTimeLeft = item.getMedia().getDuration() - item.getMedia().getPosition();
                    timeLeft += itemTimeLeft / playbackSpeed;
                }
            }
            info += " • ";
            info += getString(R.string.time_left_label);
            info += Converter.getDurationStringLocalized(getResources(), timeLeft, false);
        }
        infoBar.setText(info);

        if (recyclerAdapter.inActionMode()) {
            infoBar.setVisibility(View.INVISIBLE);
        } else {
            infoBar.setVisibility(View.VISIBLE);
        }
    }

    private void loadItems(final boolean restoreScrollPosition) {
        Log.d(TAG, "loadItems()");
        if (disposable != null) {
            disposable.dispose();
        }
        if (queue == null) {
            emptyView.hide();
        }
        disposable = Observable.fromCallable(DBReader::getQueue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(items -> {
                    queue = items;
                    progressBar.setVisibility(View.GONE);
                    recyclerAdapter.setDummyViews(0);
                    recyclerAdapter.updateItems(queue);
                    if (restoreScrollPosition) {
                        recyclerView.restoreScrollPosition(QueueFragment.TAG);
                    }
                    refreshInfoBar();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Override
    public void onStartSelectMode() {
        swipeActions.detach();
        floatingSelectMenu.setVisibility(View.VISIBLE);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                (int) getResources().getDimension(R.dimen.floating_select_menu_height));
        refreshToolbarState();
        refreshInfoBar();
    }

    @Override
    public void onEndSelectMode() {
        floatingSelectMenu.setVisibility(View.GONE);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(), 0);
        infoBar.setVisibility(View.VISIBLE);
        swipeActions.attachTo(recyclerView);
        refreshInfoBar();
    }

    public static class QueueSortDialog extends ItemSortDialog {
        boolean turnedOffKeepSortedForRandom = false;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            if (UserPreferences.isQueueKeepSorted()) {
                sortOrder = UserPreferences.getQueueKeepSortedOrder();
            }
            final View view = super.onCreateView(inflater, container, savedInstanceState);
            viewBinding.keepSortedCheckbox.setVisibility(View.VISIBLE);
            viewBinding.keepSortedCheckbox.setChecked(UserPreferences.isQueueKeepSorted());
            // Disable until something gets selected
            viewBinding.keepSortedCheckbox.setEnabled(UserPreferences.isQueueKeepSorted());
            return view;
        }

        @Override
        protected void onAddItem(int title, SortOrder ascending, SortOrder descending, boolean ascendingIsDefault) {
            if (ascending != SortOrder.EPISODE_FILENAME_A_Z && ascending != SortOrder.SIZE_SMALL_LARGE) {
                super.onAddItem(title, ascending, descending, ascendingIsDefault);
            }
        }

        @Override
        protected void onSelectionChanged() {
            super.onSelectionChanged();
            if (sortOrder == SortOrder.RANDOM) {
                turnedOffKeepSortedForRandom |= viewBinding.keepSortedCheckbox.isChecked();
                viewBinding.keepSortedCheckbox.setChecked(false);
                viewBinding.keepSortedCheckbox.setEnabled(false);
            } else {
                if (turnedOffKeepSortedForRandom) {
                    viewBinding.keepSortedCheckbox.setChecked(true);
                    turnedOffKeepSortedForRandom = false;
                }
                viewBinding.keepSortedCheckbox.setEnabled(true);
            }
            UserPreferences.setQueueKeepSorted(viewBinding.keepSortedCheckbox.isChecked());
            UserPreferences.setQueueKeepSortedOrder(sortOrder);
            DBWriter.reorderQueue(sortOrder, true);
        }
    }

    private class QueueSwipeActions extends SwipeActions {

        // Position tracking whilst dragging
        int dragFrom = -1;
        int dragTo = -1;

        public QueueSwipeActions() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, QueueFragment.this, TAG);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getBindingAdapterPosition();
            int toPosition = target.getBindingAdapterPosition();

            // Update tracked position
            if (dragFrom == -1) {
                dragFrom =  fromPosition;
            }
            dragTo = toPosition;

            int from = viewHolder.getBindingAdapterPosition();
            int to = target.getBindingAdapterPosition();
            Log.d(TAG, "move(" + from + ", " + to + ") in memory");
            if (queue == null || from >= queue.size() || to >= queue.size() || from < 0 || to < 0) {
                return false;
            }
            queue.add(to, queue.remove(from));
            recyclerAdapter.notifyItemMoved(from, to);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            if (disposable != null) {
                disposable.dispose();
            }

            //SwipeActions
            super.onSwiped(viewHolder, direction);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            // Check if drag finished
            if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                reallyMoved(dragFrom, dragTo);
            }

            dragFrom = dragTo = -1;
        }

        private void reallyMoved(int from, int to) {
            // Write drag operation to database
            Log.d(TAG, "Write to database move(" + from + ", " + to + ")");
            DBWriter.moveQueueItem(from, to, true);
        }

    }
}
