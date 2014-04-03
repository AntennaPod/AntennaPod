package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.mobeta.android.dslv.DragSortListView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.NewEpisodesListAdapter;
import de.danoeh.antennapod.asynctask.DownloadObserver;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.util.QueueAccess;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        startItemLoader();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDistributor.getInstance().register(contentUpdate);
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
        if (downloadObserver != null) {
            downloadObserver.setActivity(activity);
            downloadObserver.onResume();
        }
        if (viewsCreated && itemsLoaded) {
            onFragmentLoaded();
        }


    }

    @Override
    public void onDetach() {
        super.onDetach();
        listAdapter = null;
        activity.set(null);
        viewsCreated = false;
        if (downloadObserver != null) {
            downloadObserver.onPause();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.new_episodes_fragment, container, false);
        listView = (DragSortListView) root.findViewById(android.R.id.list);
        txtvEmpty = (TextView) root.findViewById(android.R.id.empty);
        progLoading = (ProgressBar) root.findViewById(R.id.progLoading);


        viewsCreated = true;

        if (itemsLoaded && activity.get() != null) {
            onFragmentLoaded();
        }

        return root;
    }

    private void onFragmentLoaded() {
        if (listAdapter == null) {
            listAdapter = new NewEpisodesListAdapter(activity.get(), itemAccess);
            listView.setAdapter(listAdapter);
            listView.setEmptyView(txtvEmpty);
            downloadObserver = new DownloadObserver(activity.get(), new Handler(), downloadObserverCallback);
            downloadObserver.onResume();
        }
        listAdapter.notifyDataSetChanged();
    }

    private DownloadObserver.Callback downloadObserverCallback = new DownloadObserver.Callback() {
        @Override
        public void onContentChanged() {
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
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

        @Override
        public void onFeedItemSecondaryAction(FeedItem item) {
            Log.i(TAG, item.getTitle());
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
