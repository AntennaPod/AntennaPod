package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import de.danoeh.antennapod.adapter.ActionButtonCallback;
import de.danoeh.antennapod.adapter.InternalFeedItemlistAdapter;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
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
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity.set(activity);
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
            adapter = new InternalFeedItemlistAdapter(getActivity(), itemAccess, actionButtonCallback, true);
            setListAdapter(adapter);
        }
        setListShown(true);
        adapter.notifyDataSetChanged();

    }

    private InternalFeedItemlistAdapter.ItemAccess itemAccess = new InternalFeedItemlistAdapter.ItemAccess() {
        @Override
        public boolean isInQueue(FeedItem item) {
            return (queue != null) ? queue.contains(item.getId()) : false;
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

    private ActionButtonCallback actionButtonCallback = new ActionButtonCallback() {
        @Override
        public void onActionButtonPressed(FeedItem item) {

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
