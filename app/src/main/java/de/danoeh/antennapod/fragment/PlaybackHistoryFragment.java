package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.adapter.FeedItemlistAdapter;
import de.danoeh.antennapod.core.asynctask.DownloadObserver;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.QueueAccess;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.menuhandler.NavDrawerActivity;

public class PlaybackHistoryFragment extends ListFragment {
    private static final String TAG = "PlaybackHistoryFragment";

    private List<FeedItem> playbackHistory;
    private QueueAccess queue;
    private FeedItemlistAdapter adapter;

    private boolean itemsLoaded = false;
    private boolean viewsCreated = false;

    private AtomicReference<Activity> activity = new AtomicReference<Activity>();

    private DownloadObserver downloadObserver;
    private List<Downloader> downloaderList;

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
        activity.set(null);
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
    public void onDestroyView() {
        super.onDestroyView();
        adapter = null;
        viewsCreated = false;
        if (downloadObserver != null) {
            downloadObserver.onPause();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);

        viewsCreated = true;
        if (itemsLoaded) {
            onFragmentLoaded();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        FeedItem item = adapter.getItem(position - l.getHeaderViewsCount());
        if (item != null) {
            ((MainActivity) getActivity()).loadChildFragment(ItemFragment.newInstance(item.getId()));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (itemsLoaded && !MenuItemUtils.isActivityDrawerOpen((NavDrawerActivity) getActivity())) {
            MenuItem clearHistory = menu.add(Menu.NONE, R.id.clear_history_item, Menu.CATEGORY_CONTAINER, R.string.clear_history_label);
            MenuItemCompat.setShowAsAction(clearHistory, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            TypedArray drawables = getActivity().obtainStyledAttributes(new int[]{R.attr.content_discard});
            clearHistory.setIcon(drawables.getDrawable(0));
            drawables.recycle();
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (itemsLoaded && !MenuItemUtils.isActivityDrawerOpen((NavDrawerActivity) getActivity())) {
            menu.findItem(R.id.clear_history_item).setVisible(playbackHistory != null && !playbackHistory.isEmpty());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            switch (item.getItemId()) {
                case R.id.clear_history_item:
                    DBWriter.clearPlaybackHistory(getActivity());
                    return true;
                default:
                    return false;
            }
        } else {
            return true;
        }
    }

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EventDistributor.PLAYBACK_HISTORY_UPDATE) != 0) {
                startItemLoader();
                getActivity().supportInvalidateOptionsMenu();
            }
        }
    };

    private void onFragmentLoaded() {
        if (adapter == null) {
            adapter = new FeedItemlistAdapter(getActivity(), itemAccess, new DefaultActionButtonCallback(activity.get()), true);
            setListAdapter(adapter);
            downloadObserver = new DownloadObserver(activity.get(), new Handler(), downloadObserverCallback);
            downloadObserver.onResume();
        }
        setListShown(true);
        adapter.notifyDataSetChanged();
        getActivity().supportInvalidateOptionsMenu();
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

    private FeedItemlistAdapter.ItemAccess itemAccess = new FeedItemlistAdapter.ItemAccess() {
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
