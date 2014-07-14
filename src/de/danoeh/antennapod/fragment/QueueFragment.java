package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
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

import com.mobeta.android.dslv.DragSortListView;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.adapter.QueueListAdapter;
import de.danoeh.antennapod.asynctask.DownloadObserver;
import de.danoeh.antennapod.dialog.FeedItemDialog;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.util.menuhandler.NavDrawerActivity;

/**
 * Shows all items in the queue
 */
public class QueueFragment extends Fragment {
    private static final String TAG = "QueueFragment";
    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED |
            EventDistributor.DOWNLOAD_QUEUED |
            EventDistributor.QUEUE_UPDATE;

    private DragSortListView listView;
    private QueueListAdapter listAdapter;
    private TextView txtvEmpty;
    private ProgressBar progLoading;

    private List<FeedItem> queue;
    private List<Downloader> downloaderList;

    private boolean itemsLoaded = false;
    private boolean viewsCreated = false;

    private AtomicReference<Activity> activity = new AtomicReference<Activity>();

    private DownloadObserver downloadObserver = null;

    private FeedItemDialog feedItemDialog;
    private FeedItemDialog.FeedItemDialogSavedInstance feedItemDialogSavedInstance;

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
    public void onStop() {
        super.onStop();
        EventDistributor.getInstance().unregister(contentUpdate);
        stopItemLoader();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity.set((MainActivity) activity);
    }

    private void resetViewState() {
        unregisterForContextMenu(listView);
        listAdapter = null;
        activity.set(null);
        viewsCreated = false;
        blockDownloadObserverUpdate = false;
        if (downloadObserver != null) {
            downloadObserver.onPause();
        }
        if (feedItemDialog != null) {
            feedItemDialogSavedInstance = feedItemDialog.save();
        }
        feedItemDialog = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        resetViewState();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!MenuItemUtils.isActivityDrawerOpen((NavDrawerActivity) getActivity())) {
            final SearchView sv = new SearchView(getActivity());
            MenuItemUtils.addSearchItem(menu, sv);
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
        }
    }

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

        menu.findItem(R.id.move_to_top_item).setEnabled(!queue.isEmpty() && queue.get(0) != item);
        menu.findItem(R.id.move_to_bottom_item).setEnabled(!queue.isEmpty() && queue.get(queue.size() - 1) != item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        FeedItem selectedItem = itemAccess.getItem(menuInfo.position);

        if (selectedItem == null) {
            Log.i(TAG, "Selected item at position " + menuInfo.position + " was null, ignoring selection");
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.move_to_top_item:
                DBWriter.moveQueueItemToTop(getActivity(), selectedItem.getId(), true);
                return true;
            case R.id.move_to_bottom_item:
                DBWriter.moveQueueItemToBottom(getActivity(), selectedItem.getId(), true);
                return true;
            case R.id.remove_from_queue_item:
                DBWriter.removeQueueItem(getActivity(), selectedItem.getId(), false);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.queue_label);

        View root = inflater.inflate(R.layout.queue_fragment, container, false);
        listView = (DragSortListView) root.findViewById(android.R.id.list);
        txtvEmpty = (TextView) root.findViewById(android.R.id.empty);
        progLoading = (ProgressBar) root.findViewById(R.id.progLoading);
        listView.setEmptyView(txtvEmpty);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FeedItem item = (FeedItem) listAdapter.getItem(position - listView.getHeaderViewsCount());
                if (item != null) {
                    feedItemDialog = FeedItemDialog.newInstance(activity.get(), item, QueueAccess.ItemListAccess(queue));
                    feedItemDialog.show();
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
        if (feedItemDialog != null) {
            feedItemDialog.updateContent(QueueAccess.ItemListAccess(queue), queue);
        } else if (feedItemDialogSavedInstance != null) {
            feedItemDialog = FeedItemDialog.newInstance(activity.get(), feedItemDialogSavedInstance);
        }
    }

    private DownloadObserver.Callback downloadObserverCallback = new DownloadObserver.Callback() {
        @Override
        public void onContentChanged() {
            if (listAdapter != null && !blockDownloadObserverUpdate) {
                listAdapter.notifyDataSetChanged();
            }
            if (feedItemDialog != null && feedItemDialog.isShowing()) {
                feedItemDialog.updateMenuAppearance();
            }
        }

        @Override
        public void onDownloadDataAvailable(List<Downloader> downloaderList) {
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
