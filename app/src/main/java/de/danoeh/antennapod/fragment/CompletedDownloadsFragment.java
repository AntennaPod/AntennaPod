package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;
import de.danoeh.antennapod.adapter.DownloadedEpisodesListAdapter;
import de.danoeh.antennapod.dialog.FeedItemDialog;
import de.danoeh.antennapod.feed.EventDistributor;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.storage.DBReader;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.util.QueueAccess;

import java.util.List;

/**
 * Displays all running downloads and provides a button to delete them
 */
public class CompletedDownloadsFragment extends ListFragment {
    private static final int EVENTS =
            EventDistributor.DOWNLOAD_HANDLED |
                    EventDistributor.DOWNLOADLOG_UPDATE |
                    EventDistributor.QUEUE_UPDATE |
                    EventDistributor.UNREAD_ITEMS_UPDATE;

    private List<FeedItem> items;
    private QueueAccess queue;
    private DownloadedEpisodesListAdapter listAdapter;

    private boolean viewCreated = false;
    private boolean itemsLoaded = false;

    private FeedItemDialog feedItemDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listAdapter = null;
        viewCreated = false;
        feedItemDialog = null;
        stopItemLoader();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (viewCreated && itemsLoaded) {
            onFragmentLoaded();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewCreated = true;
        if (itemsLoaded && getActivity() != null) {
            onFragmentLoaded();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        FeedItem item = listAdapter.getItem(position - l.getHeaderViewsCount());
        if (item != null) {
            feedItemDialog = FeedItemDialog.newInstance(getActivity(), item, queue);
            feedItemDialog.show();
        }

    }

    private void onFragmentLoaded() {
        if (listAdapter == null) {
            listAdapter = new DownloadedEpisodesListAdapter(getActivity(), itemAccess);
            setListAdapter(listAdapter);
        }
        setListShown(true);
        listAdapter.notifyDataSetChanged();
        if (feedItemDialog != null) {
            boolean res = feedItemDialog.updateContent(queue, items);
            if (!res && feedItemDialog.isShowing()) {
                feedItemDialog.dismiss();
            }
        }
    }

    private DownloadedEpisodesListAdapter.ItemAccess itemAccess = new DownloadedEpisodesListAdapter.ItemAccess() {
        @Override
        public int getCount() {
            return (items != null) ? items.size() : 0;
        }

        @Override
        public FeedItem getItem(int position) {
            return (items != null) ? items.get(position) : null;
        }

        @Override
        public void onFeedItemSecondaryAction(FeedItem item) {
            DBWriter.deleteFeedMediaOfItem(getActivity(), item.getMedia().getId());
        }
    };

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EventDistributor.DOWNLOAD_QUEUED) != 0) {
                if (feedItemDialog != null && feedItemDialog.isShowing()) {
                    feedItemDialog.updateMenuAppearance();
                }
            } else if ((arg & EVENTS) != 0) {
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
            if (!itemsLoaded && viewCreated) {
                setListShown(false);
            }
        }

        @Override
        protected void onPostExecute(Object[] results) {
            super.onPostExecute(results);
            if (results != null) {
                items = (List<FeedItem>) results[0];
                queue = (QueueAccess) results[1];
                itemsLoaded = true;
                if (viewCreated && getActivity() != null) {
                    onFragmentLoaded();
                }
            }
        }

        @Override
        protected Object[] doInBackground(Void... params) {
            Context context = getActivity();
            if (context != null) {
                return new Object[]{DBReader.getDownloadedItems(context),
                        QueueAccess.IDListAccess(DBReader.getQueueIDList(context))};
            }
            return null;
        }
    }
}
