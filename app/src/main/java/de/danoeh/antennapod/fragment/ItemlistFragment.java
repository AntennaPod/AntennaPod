package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.Validate;

import java.util.List;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.FeedInfoActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.adapter.FeedItemlistAdapter;
import de.danoeh.antennapod.core.asynctask.DownloadObserver;
import de.danoeh.antennapod.core.asynctask.FeedRemover;
import de.danoeh.antennapod.core.asynctask.PicassoProvider;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.QueueAccess;
import de.danoeh.antennapod.core.util.gui.MoreContentListFooterUtil;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.menuhandler.NavDrawerActivity;

/**
 * Displays a list of FeedItems.
 */
@SuppressLint("ValidFragment")
public class ItemlistFragment extends ListFragment {
    private static final String TAG = "ItemlistFragment";

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED
            | EventDistributor.DOWNLOAD_QUEUED
            | EventDistributor.QUEUE_UPDATE
            | EventDistributor.UNREAD_ITEMS_UPDATE;

    public static final String EXTRA_SELECTED_FEEDITEM = "extra.de.danoeh.antennapod.activity.selected_feeditem";
    public static final String ARGUMENT_FEED_ID = "argument.de.danoeh.antennapod.feed_id";

    protected FeedItemlistAdapter adapter;

    private long feedID;
    private Feed feed;
    protected QueueAccess queue;

    private boolean itemsLoaded = false;
    private boolean viewsCreated = false;

    private DownloadObserver downloadObserver;
    private List<Downloader> downloaderList;

    private MoreContentListFooterUtil listFooter;

    private boolean isUpdatingFeed;

    /**
     * Creates new ItemlistFragment which shows the Feeditems of a specific
     * feed. Sets 'showFeedtitle' to false
     *
     * @param feedId The id of the feed to show
     * @return the newly created instance of an ItemlistFragment
     */
    public static ItemlistFragment newInstance(long feedId) {
        ItemlistFragment i = new ItemlistFragment();
        Bundle b = new Bundle();
        b.putLong(ARGUMENT_FEED_ID, feedId);
        i.setArguments(b);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        Validate.notNull(args);
        feedID = args.getLong(ARGUMENT_FEED_ID);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDistributor.getInstance().register(contentUpdate);
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
    public void onResume() {
        super.onResume();
        updateProgressBarVisibility();
        startItemLoader();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stopItemLoader();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        resetViewState();
    }

    private void resetViewState() {
        adapter = null;
        viewsCreated = false;
        listFooter = null;
        if (downloadObserver != null) {
            downloadObserver.onPause();
        }
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker = new MenuItemUtils.UpdateRefreshMenuItemChecker() {
        @Override
        public boolean isRefreshing() {
            if (feed != null && DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFile(feed)) {
                return true;
            } else {
                return false;
            }
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (itemsLoaded && !MenuItemUtils.isActivityDrawerOpen((NavDrawerActivity) getActivity())) {
            FeedMenuHandler.onCreateOptionsMenu(inflater, menu);

            final SearchView sv = new SearchView(getActivity());
            MenuItemUtils.addSearchItem(menu, sv);
            sv.setQueryHint(getString(R.string.search_hint));
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    sv.clearFocus();
                    if (itemsLoaded) {
                        ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance(s, feed.getId()));
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    return false;
                }
            });
            isUpdatingFeed = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (itemsLoaded && !MenuItemUtils.isActivityDrawerOpen((NavDrawerActivity) getActivity())) {
            FeedMenuHandler.onPrepareOptionsMenu(menu, feed);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            try {
                if (!FeedMenuHandler.onOptionsItemClicked(getActivity(), item, feed)) {
                    switch (item.getItemId()) {
                        case R.id.remove_item:
                            final FeedRemover remover = new FeedRemover(
                                    getActivity(), feed) {
                                @Override
                                protected void onPostExecute(Void result) {
                                    super.onPostExecute(result);
                                    ((MainActivity) getActivity()).loadNavFragment(MainActivity.POS_NEW, null);
                                }
                            };
                            ConfirmationDialog conDialog = new ConfirmationDialog(getActivity(),
                                    R.string.remove_feed_label,
                                    R.string.feed_delete_confirmation_msg) {

                                @Override
                                public void onConfirmButtonPressed(
                                        DialogInterface dialog) {
                                    dialog.dismiss();
                                    remover.executeAsync();
                                }
                            };
                            conDialog.createNewDialog().show();
                            return true;
                        default:
                            return false;

                    }
                } else {
                    return true;
                }
            } catch (DownloadRequestException e) {
                e.printStackTrace();
                DownloadRequestErrorDialogCreator.newRequestErrorDialog(getActivity(), e.getMessage());
                return true;
            }
        } else {
            return true;
        }

    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        // This workaround prevents the ListFragment from setting a list adapter when its state is restored.
        // This is only necessary on API 10 because addFooterView throws an internal exception in this case.
        if (Build.VERSION.SDK_INT > 10 || insideOnFragmentLoaded) {
            super.setListAdapter(adapter);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle("");

        viewsCreated = true;
        if (itemsLoaded) {
            onFragmentLoaded();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        FeedItem selection = adapter.getItem(position - l.getHeaderViewsCount());
        if (selection != null) {
            ((MainActivity) getActivity()).loadChildFragment(ItemFragment.newInstance(selection.getId()));
        }
    }

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((EVENTS & arg) != 0) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Received contentUpdate Intent.");
                if ((EventDistributor.DOWNLOAD_QUEUED & arg) != 0) {
                    updateProgressBarVisibility();
                } else {
                    startItemLoader();
                    updateProgressBarVisibility();
                }
            }
        }
    };

    private void updateProgressBarVisibility() {
        if (isUpdatingFeed != updateRefreshMenuItemChecker.isRefreshing()) {
            getActivity().supportInvalidateOptionsMenu();
        }
        if (listFooter != null) {
            listFooter.setLoadingState(DownloadRequester.getInstance().isDownloadingFeeds());
        }

    }

    private boolean insideOnFragmentLoaded = false;

    private void onFragmentLoaded() {
        insideOnFragmentLoaded = true;
        if (adapter == null) {
            setListAdapter(null);
            setupHeaderView();
            setupFooterView();
            adapter = new FeedItemlistAdapter(getActivity(), itemAccess, new DefaultActionButtonCallback(getActivity()), false);
            setListAdapter(adapter);
            downloadObserver = new DownloadObserver(getActivity(), new Handler(), downloadObserverCallback);
            downloadObserver.onResume();
        }
        setListShown(true);
        adapter.notifyDataSetChanged();

        getActivity().supportInvalidateOptionsMenu();

        if (feed != null && feed.getNextPageLink() == null && listFooter != null) {
            getListView().removeFooterView(listFooter.getRoot());
        }

        insideOnFragmentLoaded = false;

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
            ItemlistFragment.this.downloaderList = downloaderList;
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };

    private void setupHeaderView() {
        if (getListView() == null || feed == null) {
            Log.e(TAG, "Unable to setup listview: listView = null or feed = null");
            return;
        }
        ListView lv = getListView();
        LayoutInflater inflater = (LayoutInflater)
                getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View header = inflater.inflate(R.layout.feeditemlist_header, lv, false);
        lv.addHeaderView(header);

        TextView txtvTitle = (TextView) header.findViewById(R.id.txtvTitle);
        TextView txtvAuthor = (TextView) header.findViewById(R.id.txtvAuthor);
        ImageView imgvBackground = (ImageView) header.findViewById(R.id.imgvBackground);
        ImageView imgvCover = (ImageView) header.findViewById(R.id.imgvCover);
        ImageButton butShowInfo = (ImageButton) header.findViewById(R.id.butShowInfo);

        txtvTitle.setText(feed.getTitle());
        txtvAuthor.setText(feed.getAuthor());

        Picasso.with(getActivity())
                .load(feed.getImageUri())
                .placeholder(R.color.image_readability_tint)
                .error(R.color.image_readability_tint)
                .transform(PicassoProvider.blurTransformation)
                .resize(PicassoProvider.BLUR_IMAGE_SIZE, PicassoProvider.BLUR_IMAGE_SIZE)
                .into(imgvBackground);

        Picasso.with(getActivity())
                .load(feed.getImageUri())
                .fit()
                .into(imgvCover);

        butShowInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewsCreated && itemsLoaded) {
                    Intent startIntent = new Intent(getActivity(), FeedInfoActivity.class);
                    startIntent.putExtra(FeedInfoActivity.EXTRA_FEED_ID,
                            feed.getId());
                    startActivity(startIntent);
                }
            }
        });
    }

    private void setupFooterView() {
        if (getListView() == null || feed == null) {
            Log.e(TAG, "Unable to setup listview: listView = null or feed = null");
            return;
        }
        if (feed.isPaged() && feed.getNextPageLink() != null) {
            ListView lv = getListView();
            LayoutInflater inflater = (LayoutInflater)
                    getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View header = inflater.inflate(R.layout.more_content_list_footer, lv, false);
            lv.addFooterView(header);
            listFooter = new MoreContentListFooterUtil(header);
            listFooter.setClickListener(new MoreContentListFooterUtil.Listener() {
                @Override
                public void onClick() {
                    if (feed != null) {
                        try {
                            DBTasks.loadNextPageOfFeed(getActivity(), feed, false);
                        } catch (DownloadRequestException e) {
                            e.printStackTrace();
                            DownloadRequestErrorDialogCreator.newRequestErrorDialog(getActivity(), e.getMessage());
                        }
                    }
                }
            });
        }
    }

    private FeedItemlistAdapter.ItemAccess itemAccess = new FeedItemlistAdapter.ItemAccess() {

        @Override
        public FeedItem getItem(int position) {
            return (feed != null) ? feed.getItemAtIndex(true, position) : null;
        }

        @Override
        public int getCount() {
            return (feed != null) ? feed.getNumOfItems(true) : 0;
        }

        @Override
        public boolean isInQueue(FeedItem item) {
            return (queue != null) && queue.contains(item.getId());
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

    private ItemLoader itemLoader;

    private void startItemLoader() {
        if (itemLoader != null) {
            itemLoader.cancel(true);
        }
        itemLoader = new ItemLoader();
        itemLoader.execute(feedID);
    }

    private void stopItemLoader() {
        if (itemLoader != null) {
            itemLoader.cancel(true);
        }
    }

    private class ItemLoader extends AsyncTask<Long, Void, Object[]> {
        @Override
        protected Object[] doInBackground(Long... params) {
            long feedID = params[0];
            Context context = getActivity();
            if (context != null) {
                return new Object[]{DBReader.getFeed(context, feedID),
                        QueueAccess.IDListAccess(DBReader.getQueueIDList(context))};
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object[] res) {
            super.onPostExecute(res);
            if (res != null) {
                feed = (Feed) res[0];
                queue = (QueueAccess) res[1];
                itemsLoaded = true;
                if (viewsCreated) {
                    onFragmentLoaded();
                }
            }
        }
    }
}
