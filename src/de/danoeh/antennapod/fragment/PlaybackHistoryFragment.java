package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.View;
import de.danoeh.antennapod.adapter.ActionButtonCallback;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.adapter.InternalFeedItemlistAdapter;
import de.danoeh.antennapod.asynctask.DownloadObserver;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.service.download.Downloader;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.util.QueueAccess;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PlaybackHistoryFragment extends ListFragment {
    private static final String TAG = "PlaybackHistoryFragment";

    private List<FeedItem> playbackHistory;
    private QueueAccess queue;
    private InternalFeedItemlistAdapter adapter;

    private boolean itemsLoaded = false;
    private boolean viewsCreated = false;

    private AtomicReference<Activity> activity = new AtomicReference<Activity>();

    private DownloadObserver downloadObserver;
    private List<Downloader> downloaderList;

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
    public void onDetach() {
        super.onDetach();
        stopItemLoader();
        adapter = null;
        viewsCreated = false;
        activity.set(null);
        if (downloadObserver != null) {
            downloadObserver.onPause();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity.set(activity);
        if (downloadObserver != null) {
            downloadObserver.setActivity(activity);
            downloadObserver.onResume();
        }
        if (viewsCreated && itemsLoaded) {
            onFragmentLoaded();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewsCreated = true;
        if (itemsLoaded) {
            onFragmentLoaded();
        }
    }

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EventDistributor.PLAYBACK_HISTORY_UPDATE) != 0) {
                startItemLoader();
            }
        }
    };

    private void onFragmentLoaded() {
        if (adapter == null) {
            adapter = new InternalFeedItemlistAdapter(getActivity(), itemAccess, new DefaultActionButtonCallback(activity.get()), true);
            setListAdapter(adapter);
            downloadObserver = new DownloadObserver(activity.get(), new Handler(), downloadObserverCallback);
            downloadObserver.onResume();
        }
        setListShown(true);
        adapter.notifyDataSetChanged();
    }

    private DownloadObserver.Callback downloadObserverCallback = new DownloadObserver.Callback() {
        @Override
        public void onContentChanged() {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onDownloadDataAvailable(List<Downloader> downloaderList) {
            PlaybackHistoryFragment.this.downloaderList = downloaderList;
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };

    private InternalFeedItemlistAdapter.ItemAccess itemAccess = new InternalFeedItemlistAdapter.ItemAccess() {
        @Override
        public boolean isInQueue(FeedItem item) {
            return (queue != null) ? queue.contains(item.getId()) : false;
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
        public int getCount() {
            return (playbackHistory != null) ? playbackHistory.size() : 0;
        }

        @Override
        public FeedItem getItem(int position) {
            return (playbackHistory != null) ? playbackHistory.get(position) : null;
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
        protected Object[] doInBackground(Void... params) {
            Context context = activity.get();
            if (context != null) {
                List<FeedItem> ph = DBReader.getPlaybackHistory(context);
                DBReader.loadFeedDataOfFeedItemlist(context, ph);
                return new Object[]{ph,
                        QueueAccess.IDListAccess(DBReader.getQueueIDList(context))};
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object[] res) {
            super.onPostExecute(res);
            if (res != null) {
                playbackHistory = (List<FeedItem>) res[0];
                queue = (QueueAccess) res[1];
                itemsLoaded = true;
                if (viewsCreated) {
                    onFragmentLoaded();
                }
            }
        }
    }
}
