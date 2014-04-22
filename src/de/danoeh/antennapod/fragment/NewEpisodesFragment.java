package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.mobeta.android.dslv.DragSortListView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.adapter.NewEpisodesListAdapter;
import de.danoeh.antennapod.asynctask.DownloadObserver;
import de.danoeh.antennapod.dialog.FeedItemDialog;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.service.download.DownloadService;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.menuhandler.MenuItemUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shows unread or recently published episodes
 */
public class NewEpisodesFragment extends Fragment {
    private static final String TAG = "NewEpisodesFragment";
    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED |
            EventDistributor.DOWNLOAD_QUEUED |
            EventDistributor.QUEUE_UPDATE |
            EventDistributor.UNREAD_ITEMS_UPDATE;

    private static final int RECENT_EPISODES_LIMIT = 150;

    private DragSortListView listView;
    private NewEpisodesListAdapter listAdapter;
    private TextView txtvEmpty;
    private ProgressBar progLoading;

    private List<FeedItem> unreadItems;
    private List<FeedItem> recentItems;
    private QueueAccess queueAccess;
    private List<Downloader> downloaderList;

    private boolean itemsLoaded = false;
    private boolean viewsCreated = false;

    private AtomicReference<MainActivity> activity = new AtomicReference<MainActivity>();

    private DownloadObserver downloadObserver = null;

    private FeedItemDialog feedItemDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        startItemLoader();
    }

    @Override
    public void onResume() {
        super.onResume();
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
        this.activity.set((MainActivity) getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        resetViewState();
    }

    private void resetViewState() {
        listAdapter = null;
        activity.set(null);
        viewsCreated = false;
        if (downloadObserver != null) {
            downloadObserver.onPause();
        }
        feedItemDialog = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.new_episodes, menu);

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

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.mark_all_read_item).setVisible(unreadItems != null && !unreadItems.isEmpty());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            switch (item.getItemId()) {
                case R.id.refresh_item:
                    List<Feed> feeds = ((MainActivity) getActivity()).getFeeds();
                    if (feeds != null) {
                        DBTasks.refreshAllFeeds(getActivity(), feeds);
                    }
                    return true;
                case R.id.mark_all_read_item:
                    DBWriter.markAllItemsRead(getActivity());
                    Toast.makeText(getActivity(), R.string.mark_all_read_msg, Toast.LENGTH_SHORT).show();
                    return true;
                default:
                    return false;
            }
        } else {
            return true;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.new_episodes_label);

        View root = inflater.inflate(R.layout.new_episodes_fragment, container, false);
        listView = (DragSortListView) root.findViewById(android.R.id.list);
        txtvEmpty = (TextView) root.findViewById(android.R.id.empty);
        progLoading = (ProgressBar) root.findViewById(R.id.progLoading);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FeedItem item = (FeedItem) listAdapter.getItem(position - listView.getHeaderViewsCount());
                if (item != null) {
                    feedItemDialog = FeedItemDialog.newInstace(activity.get(), item, queueAccess);
                    feedItemDialog.show();
                }

            }
        });

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
            listAdapter = new NewEpisodesListAdapter(activity.get(), itemAccess, new DefaultActionButtonCallback(activity.get()));
            listView.setAdapter(listAdapter);
            listView.setEmptyView(txtvEmpty);
            downloadObserver = new DownloadObserver(activity.get(), new Handler(), downloadObserverCallback);
            downloadObserver.onResume();
        }
        if (feedItemDialog != null && feedItemDialog.isShowing()) {
            feedItemDialog.setQueue(queueAccess);
            feedItemDialog.setItemFromCollection(unreadItems);
            feedItemDialog.setItemFromCollection(recentItems);
            feedItemDialog.updateMenuAppearance();
        }
        listAdapter.notifyDataSetChanged();
        getActivity().supportInvalidateOptionsMenu();
        updateProgressBarVisibility();
    }

    private DownloadObserver.Callback downloadObserverCallback = new DownloadObserver.Callback() {
        @Override
        public void onContentChanged() {
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            if (feedItemDialog != null && feedItemDialog.isShowing()) {
                feedItemDialog.updateMenuAppearance();
            }
        }

        @Override
        public void onDownloadDataAvailable(List<Downloader> downloaderList) {
            NewEpisodesFragment.this.downloaderList = downloaderList;
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
    };

    private NewEpisodesListAdapter.ItemAccess itemAccess = new NewEpisodesListAdapter.ItemAccess() {


        @Override
        public int getUnreadItemsCount() {
            return (itemsLoaded) ? unreadItems.size() : 0;
        }

        @Override
        public int getRecentItemsCount() {
            return (itemsLoaded) ? recentItems.size() : 0;
        }

        @Override
        public FeedItem getUnreadItem(int position) {
            return (itemsLoaded) ? unreadItems.get(position) : null;
        }

        @Override
        public FeedItem getRecentItem(int position) {
            return (itemsLoaded) ? recentItems.get(position) : null;
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
        public boolean isInQueue(FeedItem item) {
            if (itemsLoaded) {
                return queueAccess.contains(item.getId());
            } else {
                return false;
            }
        }


    };

    private void updateProgressBarVisibility() {
        if (DownloadService.isRunning
                && DownloadRequester.getInstance().isDownloadingFeeds()) {
            ((ActionBarActivity) getActivity())
                    .setSupportProgressBarIndeterminateVisibility(true);
        } else {
            ((ActionBarActivity) getActivity())
                    .setSupportProgressBarIndeterminateVisibility(false);
        }
        getActivity().supportInvalidateOptionsMenu();

    }

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EVENTS) != 0) {
                startItemLoader();
                updateProgressBarVisibility();
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

    private class ItemLoader extends AsyncTask<Void, Void, Object[]> {

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
        protected Object[] doInBackground(Void... params) {
            Context context = activity.get();
            if (context != null) {
                return new Object[]{DBReader.getUnreadItemsList(context),
                        DBReader.getRecentlyPublishedEpisodes(context, RECENT_EPISODES_LIMIT),
                        QueueAccess.IDListAccess(DBReader.getQueueIDList(context))};
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object[] lists) {
            super.onPostExecute(lists);
            listView.setVisibility(View.VISIBLE);
            progLoading.setVisibility(View.GONE);

            if (lists != null) {
                unreadItems = (List<FeedItem>) lists[0];
                recentItems = (List<FeedItem>) lists[1];
                queueAccess = (QueueAccess) lists[2];
                itemsLoaded = true;
                if (viewsCreated && activity.get() != null) {
                    onFragmentLoaded();
                }
            }
        }
    }
}
