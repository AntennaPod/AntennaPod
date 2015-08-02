package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mobeta.android.dslv.DragSortListView;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.adapter.QueueListAdapter;
import de.danoeh.antennapod.core.asynctask.DownloadObserver;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.QueueEvent;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.QueueSorter;
import de.danoeh.antennapod.core.util.gui.FeedItemUndoToken;
import de.danoeh.antennapod.core.util.gui.UndoBarController;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.greenrobot.event.EventBus;

/**
 * Shows all items in the queue
 */
public class QueueFragment extends Fragment {

    public static final String TAG = "QueueFragment";

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED |
            EventDistributor.DOWNLOAD_QUEUED |
            EventDistributor.PLAYER_STATUS_UPDATE;

    private TextView infoBar;
    private DragSortListView listView;
    private QueueListAdapter listAdapter;
    private TextView txtvEmpty;
    private ProgressBar progLoading;

    private ContextMenu contextMenu;
    private AdapterView.AdapterContextMenuInfo lastMenuInfo = null;

    private UndoBarController<FeedItemUndoToken> undoBarController;

    private List<FeedItem> queue;
    private List<Downloader> downloaderList;

    private boolean itemsLoaded = false;
    private boolean viewsCreated = false;
    private boolean isUpdatingFeeds = false;

    private static final String PREFS = "QueueFragment";
    private static final String PREF_KEY_LIST_TOP = "list_top";
    private static final String PREF_KEY_LIST_SELECTION = "list_selection";

    private AtomicReference<Activity> activity = new AtomicReference<Activity>();

    private DownloadObserver downloadObserver = null;

    /**
     * Download observer updates won't result in an upate of the list adapter if this is true.
     */
    private boolean blockDownloadObserverUpdate = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        startItemLoader();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDistributor.getInstance().register(contentUpdate);
        EventBus.getDefault().register(this);
        this.activity.set((MainActivity) getActivity());
        if (downloadObserver != null) {
            downloadObserver.setActivity(getActivity());
            downloadObserver.onResume();
        }
        if (viewsCreated && itemsLoaded) {
            onFragmentLoaded();
        }
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
        stopItemLoader();
        if(undoBarController.isShowing()) {
            undoBarController.close();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity.set((MainActivity) activity);
    }

    public void onEventMainThread(QueueEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        if(event.action == QueueEvent.Action.REMOVED) {
            undoBarController.showUndoBar(false, getString(R.string.removed_from_queue),
                    new FeedItemUndoToken(event.item, event.position));
        }
        startItemLoader();
    }

    private void saveScrollPosition() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
        editor.putInt(PREF_KEY_LIST_SELECTION, listView.getFirstVisiblePosition());
        editor.putInt(PREF_KEY_LIST_TOP, top);
        editor.commit();
    }

    private void restoreScrollPosition() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int listSelection = prefs.getInt(PREF_KEY_LIST_SELECTION, 0);
        int top = prefs.getInt(PREF_KEY_LIST_TOP, 0);
        if(listSelection > 0 || top > 0) {
            listView.setSelectionFromTop(listSelection, top);
            // restore once, then forget
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_KEY_LIST_SELECTION, 0);
            editor.putInt(PREF_KEY_LIST_TOP, 0);
            editor.commit();
        }
    }

    private void resetViewState() {
        unregisterForContextMenu(listView);
        listAdapter = null;
        activity.set(null);
        undoBarController = null;
        viewsCreated = false;
        blockDownloadObserverUpdate = false;
        if (downloadObserver != null) {
            downloadObserver.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        resetViewState();
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker = new MenuItemUtils.UpdateRefreshMenuItemChecker() {
        @Override
        public boolean isRefreshing() {
            return DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds();
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (itemsLoaded) {
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
                    boolean locked = !UserPreferences.isQueueLocked();
                    if(locked) {
                        listView.setDragEnabled(false);
                    } else {
                        listView.setDragEnabled(true);
                    }
                    UserPreferences.setQueueLocked(locked);
                    getActivity().supportInvalidateOptionsMenu();
                    listAdapter.setLocked(locked);
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
                            DBWriter.clearQueue(getActivity());
                        }
                    };
                    conDialog.createNewDialog().show();
                    return true;
                case R.id.queue_sort_alpha_asc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.ALPHA_ASC, true);
                    return true;
                case R.id.queue_sort_alpha_desc:
                    QueueSorter.sort(getActivity(), QueueSorter.Rule.ALPHA_DESC, true);
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
                default:
                    return false;
            }
        } else {
            return true;
        }

    }

    private final FeedItemMenuHandler.MenuInterface contextMenuInterface = new FeedItemMenuHandler.MenuInterface() {
        @Override
        public void setItemVisibility(int id, boolean visible) {
            if(contextMenu == null) {
                return;
            }
            MenuItem item = contextMenu.findItem(id);
            if (item != null) {
                item.setVisible(visible);
            }
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo adapterInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        FeedItem item = itemAccess.getItem(adapterInfo.position);

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.queue_context, menu);

        if (item != null) {
            menu.setHeaderTitle(item.getTitle());
        }

        contextMenu = menu;
        lastMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        LongList queueIds = new LongList(queue.size());
        for(FeedItem queueItem : queue) {
            queueIds.add(queueItem.getId());
        }
        FeedItemMenuHandler.onPrepareMenu(getActivity(), contextMenuInterface, item, true, queueIds);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(menuInfo == null) {
            menuInfo = lastMenuInfo;
        }
        FeedItem selectedItem = itemAccess.getItem(menuInfo.position);

        if (selectedItem == null) {
            Log.i(TAG, "Selected item at position " + menuInfo.position + " was null, ignoring selection");
            return super.onContextItemSelected(item);
        }

        try {
            return FeedItemMenuHandler.onMenuItemClicked(getActivity(), item.getItemId(), selectedItem);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
            return true;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.queue_label);

        View root = inflater.inflate(R.layout.queue_fragment, container, false);
        infoBar = (TextView) root.findViewById(R.id.info_bar);
        listView = (DragSortListView) root.findViewById(android.R.id.list);
        txtvEmpty = (TextView) root.findViewById(android.R.id.empty);
        progLoading = (ProgressBar) root.findViewById(R.id.progLoading);
        listView.setEmptyView(txtvEmpty);

        if(UserPreferences.isQueueLocked()) {
            listView.setDragEnabled(false);
        } else {
            listView.setDragEnabled(true);
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FeedItem item = (FeedItem) listAdapter.getItem(position - listView.getHeaderViewsCount());
                if (item != null) {
                    ((MainActivity) getActivity()).loadChildFragment(ItemFragment.newInstance(item.getId()));
                }
            }
        });

        listView.setDragSortListener(new DragSortListView.DragSortListener() {
            @Override
            public void drag(int from, int to) {
                Log.d(TAG, "drag");
                blockDownloadObserverUpdate = true;
            }

            @Override
            public void drop(int from, int to) {
                Log.d(TAG, "drop");
                blockDownloadObserverUpdate = false;
                stopItemLoader();
                final FeedItem item = queue.remove(from);
                queue.add(to, item);
                listAdapter.notifyDataSetChanged();
                DBWriter.moveQueueItem(getActivity(), from, to, true);
            }

            @Override
            public void remove(int which) {
                Log.d(TAG, "remove(" + which + ")");
                stopItemLoader();
                FeedItem item = (FeedItem) listView.getAdapter().getItem(which);
                DBWriter.removeQueueItem(getActivity(), item, true);
            }
        });

        undoBarController = new UndoBarController<FeedItemUndoToken>(root.findViewById(R.id.undobar),
                new UndoBarController.UndoListener<FeedItemUndoToken>() {

            private final Context context = getActivity();

            @Override
            public void onUndo(FeedItemUndoToken token) {
                if (token != null) {
                    long itemId = token.getFeedItemId();
                    int position = token.getPosition();
                    DBWriter.addQueueItemAt(context, itemId, position, false);
                }
            }

            @Override
            public void onHide(FeedItemUndoToken token) {
                if (token != null && context != null) {
                    long itemId = token.getFeedItemId();
                    FeedItem item = DBReader.getFeedItem(context, itemId);
                    FeedMedia media = item.getMedia();
                    if(media != null && media.hasAlmostEnded() && item.getFeed().getPreferences().getCurrentAutoDelete()) {
                        DBWriter.deleteFeedMediaOfItem(context, media.getId());
                    }
                }
            }

        });

        registerForContextMenu(listView);

        if (!itemsLoaded) {
            progLoading.setVisibility(View.VISIBLE);
            txtvEmpty.setVisibility(View.GONE);
        }

        viewsCreated = true;

        if (itemsLoaded && activity.get() != null) {
            onFragmentLoaded();
        }

        return root;
    }

    private void onFragmentLoaded() {
        if (listAdapter == null) {
            listAdapter = new QueueListAdapter(activity.get(), itemAccess, new DefaultActionButtonCallback(activity.get()));
            listView.setAdapter(listAdapter);
            downloadObserver = new DownloadObserver(activity.get(), new Handler(), downloadObserverCallback);
            downloadObserver.onResume();
        }
        listAdapter.notifyDataSetChanged();

        restoreScrollPosition();

        // we need to refresh the options menu because it sometimes
        // needs data that may have just been loaded.
        getActivity().supportInvalidateOptionsMenu();

        // refresh information bar
        String info = queue.size() + getString(R.string.episodes_suffix);
        if(queue.size() > 0) {
            long duration = 0;
            for(FeedItem item : queue) {
                if(item.getMedia() != null) {
                    duration += item.getMedia().getDuration();
                }
            }
            info += " \u2022 ";
            info += Converter.getDurationStringLocalized(getActivity(), duration);
        }
        infoBar.setText(info);
    }

    private DownloadObserver.Callback downloadObserverCallback = new DownloadObserver.Callback() {
        @Override
        public void onContentChanged(List<Downloader> downloaderList) {
            QueueFragment.this.downloaderList = downloaderList;
            if (listAdapter != null && !blockDownloadObserverUpdate) {
                listAdapter.notifyDataSetChanged();
            }
        }
    };

    private QueueListAdapter.ItemAccess itemAccess = new QueueListAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return (itemsLoaded) ? queue.size() : 0;
        }

        @Override
        public FeedItem getItem(int position) {
            return (itemsLoaded) ? queue.get(position) : null;
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
    };

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EVENTS) != 0) {
                startItemLoader();
                if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing()) {
                    getActivity().supportInvalidateOptionsMenu();
                }
            }
        }
    };

    private ItemLoader itemLoader;

    private void startItemLoader() {
        if (itemLoader != null) {
            itemLoader.cancel(true);
        }
        itemLoader = new ItemLoader();
        itemLoader.execute();
    }

    private void stopItemLoader() {
        if (itemLoader != null) {
            itemLoader.cancel(true);
        }
    }

    private class ItemLoader extends AsyncTask<Void, Void, List<FeedItem>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (viewsCreated && !itemsLoaded) {
                listView.setVisibility(View.GONE);
                txtvEmpty.setVisibility(View.GONE);
                progLoading.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(List<FeedItem> feedItems) {
            super.onPostExecute(feedItems);
            listView.setVisibility(View.VISIBLE);
            progLoading.setVisibility(View.GONE);

            if (feedItems != null) {
                queue = feedItems;
                itemsLoaded = true;
                if (viewsCreated && activity.get() != null) {
                    onFragmentLoaded();
                }
            }
        }

        @Override
        protected List<FeedItem> doInBackground(Void... params) {
            Context context = activity.get();
            if (context != null) {
                return DBReader.getQueue(context);
            }
            return null;
        }
    }
}
