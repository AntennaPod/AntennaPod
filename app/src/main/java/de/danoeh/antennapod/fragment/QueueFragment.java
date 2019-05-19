package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.adapter.QueueRecyclerAdapter;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.event.QueueEvent;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.QueueSorter;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;

import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Shows all items in the queue
 */
public class QueueFragment extends Fragment {

    public static final String TAG = "QueueFragment";

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED |
            EventDistributor.UNREAD_ITEMS_UPDATE | // sent when playback position is reset
            EventDistributor.PLAYER_STATUS_UPDATE;

    private TextView infoBar;
    private RecyclerView recyclerView;
    private QueueRecyclerAdapter recyclerAdapter;
    private EmptyViewHandler emptyView;
    private ProgressBar progLoading;

    private List<FeedItem> queue;
    private List<Downloader> downloaderList;

    private boolean isUpdatingFeeds = false;

    private static final String PREFS = "QueueFragment";
    private static final String PREF_SCROLL_POSITION = "scroll_position";
    private static final String PREF_SCROLL_OFFSET = "scroll_offset";

    private Disposable disposable;
    private LinearLayoutManager layoutManager;
    private ItemTouchHelper itemTouchHelper;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (queue != null) {
            onFragmentLoaded(true);
        }
        loadItems(true);
        EventDistributor.getInstance().register(contentUpdate);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveScrollPosition();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventDistributor.getInstance().unregister(contentUpdate);
        EventBus.getDefault().unregister(this);
        if(disposable != null) {
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
                queue = event.items;
                recyclerAdapter.notifyDataSetChanged();
                break;
            case REMOVED:
            case IRREVERSIBLE_REMOVED:
                int position = FeedItemUtil.indexOfItemWithId(queue, event.item.getId());
                queue.remove(position);
                recyclerAdapter.notifyItemRemoved(position);
                break;
            case CLEARED:
                queue.clear();
                recyclerAdapter.notifyDataSetChanged();
                break;
            case SORTED:
                queue = event.items;
                recyclerAdapter.notifyDataSetChanged();
                break;
            case MOVED:
                return;
        }
        saveScrollPosition();
        onFragmentLoaded(false);
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
        for(int i=0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemUtil.indexOfItemWithId(queue, item.getId());
            if(pos >= 0) {
                queue.remove(pos);
                queue.add(pos, item);
                recyclerAdapter.notifyItemChanged(pos);
                refreshInfoBar();
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        downloaderList = update.downloaders;
        if (isUpdatingFeeds != update.feedIds.length > 0) {
            getActivity().supportInvalidateOptionsMenu();
        }
        if (recyclerAdapter != null && update.mediaIds.length > 0) {
            for (long mediaId : update.mediaIds) {
                int pos = FeedItemUtil.indexOfItemWithMediaId(queue, mediaId);
                if (pos >= 0) {
                    recyclerAdapter.notifyItemChanged(pos);
                }
            }
        }
    }

    private void saveScrollPosition() {
        int firstItem = layoutManager.findFirstVisibleItemPosition();
        View firstItemView = layoutManager.findViewByPosition(firstItem);
        float topOffset;
        if(firstItemView == null) {
            topOffset = 0;
        } else {
            topOffset = firstItemView.getTop();
        }

        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_SCROLL_POSITION, firstItem);
        editor.putFloat(PREF_SCROLL_OFFSET, topOffset);
        editor.commit();
    }

    private void restoreScrollPosition() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int position = prefs.getInt(PREF_SCROLL_POSITION, 0);
        float offset = prefs.getFloat(PREF_SCROLL_OFFSET, 0.0f);
        if (position > 0 || offset > 0) {
            layoutManager.scrollToPositionWithOffset(position, (int) offset);
        }
    }

    private void resetViewState() {
        recyclerAdapter = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        resetViewState();
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(!isAdded()) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
        if (queue != null) {
            inflater.inflate(R.menu.queue, menu);

            MenuItem searchItem = menu.findItem(R.id.action_search);
            final SearchView sv = (SearchView) MenuItemCompat.getActionView(searchItem);
            MenuItemUtils.adjustTextColor(getActivity(), sv);
            sv.setQueryHint(getString(R.string.search_hint));
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    sv.clearFocus();
                    ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance(s));
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    return false;
                }
            });

            MenuItemUtils.refreshLockItem(getActivity(), menu);

            isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            switch (item.getItemId()) {
                case R.id.queue_lock:
                    boolean newLockState = !UserPreferences.isQueueLocked();
                    UserPreferences.setQueueLocked(newLockState);
                    getActivity().supportInvalidateOptionsMenu();
                    if (recyclerAdapter != null) {
                        recyclerAdapter.setLocked(newLockState);
                    }
                    if (newLockState) {
                        Snackbar.make(getActivity().findViewById(R.id.content), R.string
                                .queue_locked, Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(getActivity().findViewById(R.id.content), R.string
                                .queue_unlocked, Snackbar.LENGTH_SHORT).show();
                    }
                    return true;
                case R.id.refresh_item:
                    List<Feed> feeds = ((MainActivity) getActivity()).getFeeds();
                    if (feeds != null) {
                        DBTasks.refreshAllFeeds(getActivity(), feeds);
                    }
                    return true;
                case R.id.clear_queue:
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
                case R.id.queue_sort_episode_title_asc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.EPISODE_TITLE_ASC, true);
                    return true;
                case R.id.queue_sort_episode_title_desc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.EPISODE_TITLE_DESC, true);
                    return true;
                case R.id.queue_sort_date_asc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.DATE_ASC, true);
                    return true;
                case R.id.queue_sort_date_desc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.DATE_DESC, true);
                    return true;
                case R.id.queue_sort_duration_asc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.DURATION_ASC, true);
                    return true;
                case R.id.queue_sort_duration_desc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.DURATION_DESC, true);
                    return true;
                case R.id.queue_sort_feed_title_asc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.FEED_TITLE_ASC, true);
                    return true;
                case R.id.queue_sort_feed_title_desc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.FEED_TITLE_DESC, true);
                    return true;
                case R.id.queue_sort_random:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.RANDOM, true);
                    return true;
                case R.id.queue_sort_smart_shuffle_asc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.SMART_SHUFFLE_ASC, true);
                    return true;
                case R.id.queue_sort_smart_shuffle_desc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.SMART_SHUFFLE_DESC, true);
                    return true;
                default:
                    return false;
            }
        } else {
            return true;
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d(TAG, "onContextItemSelected() called with: " + "item = [" + item + "]");
        if(!isVisible()) {
            return false;
        }
        FeedItem selectedItem = recyclerAdapter.getSelectedItem();
        if (selectedItem == null) {
            Log.i(TAG, "Selected item was null, ignoring selection");
            return super.onContextItemSelected(item);
        }

        switch(item.getItemId()) {
            case R.id.move_to_top_item:
                int position = FeedItemUtil.indexOfItemWithId(queue, selectedItem.getId());
                queue.add(0, queue.remove(position));
                recyclerAdapter.notifyItemMoved(position, 0);
                DBWriter.moveQueueItemToTop(selectedItem.getId(), true);
                return true;
            case R.id.move_to_bottom_item:
                position = FeedItemUtil.indexOfItemWithId(queue, selectedItem.getId());
                queue.add(queue.size()-1, queue.remove(position));
                recyclerAdapter.notifyItemMoved(position, queue.size()-1);
                DBWriter.moveQueueItemToBottom(selectedItem.getId(), true);
                return true;
            default:
                return FeedItemMenuHandler.onMenuItemClicked(getActivity(), item.getItemId(), selectedItem);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.queue_label);

        View root = inflater.inflate(R.layout.queue_fragment, container, false);
        infoBar = root.findViewById(R.id.info_bar);
        recyclerView = root.findViewById(R.id.recyclerView);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity()).build());
        recyclerView.setHasFixedSize(true);
        registerForContextMenu(recyclerView);

        itemTouchHelper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {

                // Position tracking whilst dragging
                int dragFrom = -1;
                int dragTo = -1;

                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    int fromPosition = viewHolder.getAdapterPosition();
                    int toPosition = target.getAdapterPosition();

                    // Update tracked position
                    if(dragFrom == -1) {
                        dragFrom =  fromPosition;
                    }
                    dragTo = toPosition;

                    int from = viewHolder.getAdapterPosition();
                    int to = target.getAdapterPosition();
                    Log.d(TAG, "move(" + from + ", " + to + ") in memory");
                    if(from >= queue.size() || to >= queue.size()) {
                        return false;
                    }
                    queue.add(to, queue.remove(from));
                    recyclerAdapter.notifyItemMoved(from, to);
                    return true;
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    if(disposable != null) {
                        disposable.dispose();
                    }
                    final int position = viewHolder.getAdapterPosition();
                    Log.d(TAG, "remove(" + position + ")");
                    final FeedItem item = queue.get(position);
                    final boolean isRead = item.isPlayed();
                    DBWriter.markItemPlayed(FeedItem.PLAYED, false, item.getId());
                    DBWriter.removeQueueItem(getActivity(), true, item);
                    Snackbar snackbar = Snackbar.make(root, getString(R.string.marked_as_read_label), Snackbar.LENGTH_LONG);
                    snackbar.setAction(getString(R.string.undo), v -> {
                        DBWriter.addQueueItemAt(getActivity(), item.getId(), position, false);
                        if(!isRead) {
                            DBWriter.markItemPlayed(FeedItem.UNPLAYED, item.getId());
                        }
                    });
                    snackbar.show();
                }

                @Override
                public boolean isLongPressDragEnabled() {
                    return !UserPreferences.isQueueLocked();
                }

                @Override
                public boolean isItemViewSwipeEnabled() {
                    return !UserPreferences.isQueueLocked();
                }

                @Override
                public void onSelectedChanged(RecyclerView.ViewHolder viewHolder,
                                              int actionState) {
                    // We only want the active item
                    if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                        if (viewHolder instanceof QueueRecyclerAdapter.ItemTouchHelperViewHolder) {
                            QueueRecyclerAdapter.ItemTouchHelperViewHolder itemViewHolder =
                                    (QueueRecyclerAdapter.ItemTouchHelperViewHolder) viewHolder;
                            itemViewHolder.onItemSelected();
                        }
                    }

                    super.onSelectedChanged(viewHolder, actionState);
                }
                @Override
                public void clearView(RecyclerView recyclerView,
                                      RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);

                    // Check if drag finished
                    if(dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                        reallyMoved(dragFrom, dragTo);
                    }

                    dragFrom = dragTo = -1;

                    if (viewHolder instanceof QueueRecyclerAdapter.ItemTouchHelperViewHolder) {
                        QueueRecyclerAdapter.ItemTouchHelperViewHolder itemViewHolder =
                                (QueueRecyclerAdapter.ItemTouchHelperViewHolder) viewHolder;
                        itemViewHolder.onItemClear();
                    }
                }

                private void reallyMoved(int from, int to) {
                    // Write drag operation to database
                    Log.d(TAG, "Write to database move(" + from + ", " + to + ")");
                    DBWriter.moveQueueItem(from, to, true);
                }
            }
        );
        itemTouchHelper.attachToRecyclerView(recyclerView);

        emptyView = new EmptyViewHandler(getContext());
        emptyView.attachToRecyclerView(recyclerView);
        emptyView.setTitle(R.string.no_items_header_label);
        emptyView.setMessage(R.string.no_items_label);

        progLoading = root.findViewById(R.id.progLoading);
        progLoading.setVisibility(View.VISIBLE);

        return root;
    }

    private void onFragmentLoaded(final boolean restoreScrollPosition) {
        if (queue != null && queue.size() > 0) {
            if (recyclerAdapter == null) {
                MainActivity activity = (MainActivity) getActivity();
                recyclerAdapter = new QueueRecyclerAdapter(activity, itemAccess,
                        new DefaultActionButtonCallback(activity), itemTouchHelper);
                recyclerAdapter.setHasStableIds(true);
                recyclerView.setAdapter(recyclerAdapter);
                emptyView.updateAdapter(recyclerAdapter);
            }
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            recyclerAdapter = null;
            recyclerView.setVisibility(View.GONE);
            emptyView.updateAdapter(recyclerAdapter);
        }

        if (restoreScrollPosition) {
            restoreScrollPosition();
        }

        // we need to refresh the options menu because it sometimes
        // needs data that may have just been loaded.
        getActivity().supportInvalidateOptionsMenu();

        refreshInfoBar();
    }

    private void refreshInfoBar() {
        String info = queue.size() + getString(R.string.episodes_suffix);
        if(queue.size() > 0) {
            long timeLeft = 0;
            float playbackSpeed = Float.valueOf(UserPreferences.getPlaybackSpeed());
            for(FeedItem item : queue) {
                if(item.getMedia() != null) {
                    timeLeft +=
                            (long) ((item.getMedia().getDuration() - item.getMedia().getPosition())
                                    / playbackSpeed);
                }
            }
            info += " \u2022 ";
            info += getString(R.string.time_left_label);
            info += Converter.getDurationStringLocalized(getActivity(), timeLeft);
        }
        infoBar.setText(info);
    }

    private final QueueRecyclerAdapter.ItemAccess itemAccess = new QueueRecyclerAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return queue != null ? queue.size() : 0;
        }

        @Override
        public FeedItem getItem(int position) {
            if (queue != null && 0 <= position && position < queue.size()) {
                return queue.get(position);
            }
            return null;
        }

        @Override
        public long getItemDownloadedBytes(FeedItem item) {
            if (downloaderList != null) {
                for (Downloader downloader : downloaderList) {
                    if (downloader.getDownloadRequest().getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                            && downloader.getDownloadRequest().getFeedfileId() == item.getMedia().getId()) {
                        Log.d(TAG, "downloaded bytes: " + downloader.getDownloadRequest().getSoFar());
                        return downloader.getDownloadRequest().getSoFar();
                    }
                }
            }
            return 0;
        }

        @Override
        public long getItemDownloadSize(FeedItem item) {
            if (downloaderList != null) {
                for (Downloader downloader : downloaderList) {
                    if (downloader.getDownloadRequest().getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                            && downloader.getDownloadRequest().getFeedfileId() == item.getMedia().getId()) {
                        Log.d(TAG, "downloaded size: " + downloader.getDownloadRequest().getSize());
                        return downloader.getDownloadRequest().getSize();
                    }
                }
            }
            return 0;
        }
        @Override
        public int getItemDownloadProgressPercent(FeedItem item) {
            if (downloaderList != null) {
                for (Downloader downloader : downloaderList) {
                    if (downloader.getDownloadRequest().getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                            && downloader.getDownloadRequest().getFeedfileId() == item.getMedia().getId()) {
                        return downloader.getDownloadRequest().getProgressPercent();
                    }
                }
            }
            return 0;
        }

        @Override
        public LongList getQueueIds() {
            return queue != null ? LongList.of(FeedItemUtil.getIds(queue)) : new LongList(0);
        }
    };

    private final EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EVENTS) != 0) {
                Log.d(TAG, "arg: " + arg);
                loadItems(false);
                if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing()) {
                    getActivity().supportInvalidateOptionsMenu();
                }
            }
        }
    };

    private void loadItems(final boolean restoreScrollPosition) {
        Log.d(TAG, "loadItems()");
        if(disposable != null) {
            disposable.dispose();
        }
        if (queue == null) {
            recyclerView.setVisibility(View.GONE);
            emptyView.hide();
            progLoading.setVisibility(View.VISIBLE);
        }
        disposable = Observable.fromCallable(DBReader::getQueue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(items -> {
                    progLoading.setVisibility(View.GONE);
                    queue = items;
                    onFragmentLoaded(restoreScrollPosition);
                    if(recyclerAdapter != null) {
                        recyclerAdapter.notifyDataSetChanged();
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

}
