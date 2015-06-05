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
import android.support.v4.view.MenuItemCompat;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.IconTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.joanzapata.android.iconify.Iconify;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.Validate;

import java.util.List;

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
import de.danoeh.antennapod.core.feed.FeedEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedItemFilter;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.QueueEvent;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.gui.MoreContentListFooterUtil;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.greenrobot.event.EventBus;

/**
 * Displays a list of FeedItems.
 */
@SuppressLint("ValidFragment")
public class ItemlistFragment extends ListFragment {
    private static final String TAG = "ItemlistFragment";

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED
            | EventDistributor.DOWNLOAD_QUEUED
            | EventDistributor.UNREAD_ITEMS_UPDATE
            | EventDistributor.PLAYER_STATUS_UPDATE;

    public static final String EXTRA_SELECTED_FEEDITEM = "extra.de.danoeh.antennapod.activity.selected_feeditem";
    public static final String ARGUMENT_FEED_ID = "argument.de.danoeh.antennapod.feed_id";

    protected FeedItemlistAdapter adapter;
    private ContextMenu contextMenu;
    private AdapterView.AdapterContextMenuInfo lastMenuInfo = null;

    private long feedID;
    private Feed feed;
    private LongList queuedItemsIds;
    private LongList newItemsIds;


    private boolean itemsLoaded = false;
    private boolean viewsCreated = false;

    private DownloadObserver downloadObserver;
    private List<Downloader> downloaderList;

    private MoreContentListFooterUtil listFooter;

    private boolean isUpdatingFeed;
    
    private IconTextView txtvFailure;

    private TextView txtvInformation;

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
        EventBus.getDefault().register(this);
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
        EventBus.getDefault().unregister(this);
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

        if (itemsLoaded) {
            FeedMenuHandler.onCreateOptionsMenu(inflater, menu);

            MenuItem searchItem = menu.findItem(R.id.action_search);
            final SearchView sv = (SearchView) MenuItemCompat.getActionView(searchItem);
            MenuItemUtils.adjustTextColor(getActivity(), sv);
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
            if(feed == null || feed.getLink() == null) {
                menu.findItem(R.id.share_link_item).setVisible(false);
                menu.findItem(R.id.visit_website_item).setVisible(false);
            }

            isUpdatingFeed = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (itemsLoaded) {
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
                                    ((MainActivity) getActivity()).loadFragment(NewEpisodesFragment.TAG, null);
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

        // because of addHeaderView(), positions are increased by 1!
        FeedItem item = itemAccess.getItem(adapterInfo.position-1);

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.feeditemlist_context, menu);

        if (item != null) {
            menu.setHeaderTitle(item.getTitle());
        }

        contextMenu = menu;
        lastMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
        FeedItemMenuHandler.onPrepareMenu(getActivity(), contextMenuInterface, item, true, queuedItemsIds);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(menuInfo == null) {
            menuInfo = lastMenuInfo;
        }
        // because of addHeaderView(), positions are increased by 1!
        FeedItem selectedItem = itemAccess.getItem(menuInfo.position-1);

        if (selectedItem == null) {
            Log.i(TAG, "Selected item at position " + menuInfo.position + " was null, ignoring selection");
            return super.onContextItemSelected(item);
        }

        try {
            return FeedItemMenuHandler.onMenuItemClicked(getActivity(), item.getItemId(), selectedItem);
        } catch (DownloadRequestException e) {
            // context menu doesn't contain download functionality
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

        registerForContextMenu(getListView());

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

    public void onEvent(QueueEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        startItemLoader();
    }

    public void onEvent(FeedEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        if(event.feedId == feedID) {
            startItemLoader();
        }
    }

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {

        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((EVENTS & arg) != 0) {
                Log.d(TAG, "Received contentUpdate Intent. arg " + arg);
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
        refreshHeaderView();
        setListShown(true);
        adapter.notifyDataSetChanged();

        getActivity().supportInvalidateOptionsMenu();

        if (feed != null && feed.getNextPageLink() == null && listFooter != null) {
            getListView().removeFooterView(listFooter.getRoot());
        }

        insideOnFragmentLoaded = false;

    }

    private void refreshHeaderView() {
        if(feed.hasLastUpdateFailed()) {
            txtvFailure.setVisibility(View.VISIBLE);
        } else {
            txtvFailure.setVisibility(View.GONE);
        }
        if(feed.getItemFilter() != null) {
            FeedItemFilter filter = feed.getItemFilter();
            if(filter.getValues().length > 0) {
                if(feed.hasLastUpdateFailed()) {
                    RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) txtvInformation.getLayoutParams();
                    p.addRule(RelativeLayout.BELOW, R.id.txtvFailure);
                }
                txtvInformation.setText("{fa-info-circle} " + this.getString(R.string.filtered_label));
                Iconify.addIcons(txtvInformation);
                txtvInformation.setVisibility(View.VISIBLE);
            } else {
                txtvInformation.setVisibility(View.GONE);
            }
        } else {

            txtvInformation.setVisibility(View.GONE);
        }
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
        txtvInformation = (TextView) header.findViewById(R.id.txtvInformation);
        txtvFailure = (IconTextView) header.findViewById(R.id.txtvFailure);

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
            return (feed != null) ? feed.getItemAtIndex(position) : null;
        }

        @Override
        public int getCount() {
            return (feed != null) ? feed.getNumOfItems() : 0;
        }

        @Override
        public boolean isInQueue(FeedItem item) {
            return (queuedItemsIds != null) && queuedItemsIds.contains(item.getId());
        }

        @Override
        public boolean isNew(FeedItem item) {
            return (newItemsIds != null) && newItemsIds.contains(item.getId());
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
                Feed feed = DBReader.getFeed(context, feedID);
                if(feed.getItemFilter() != null) {
                    FeedItemFilter filter = feed.getItemFilter();
                    feed.setItems(filter.filter(context, feed.getItems()));
                }
                LongList queuedItemsIds = DBReader.getQueueIDList(context);
                LongList newItemsIds = DBReader.getNewItemIds(context);
                return new Object[] { feed, queuedItemsIds, newItemsIds };
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object[] res) {
            super.onPostExecute(res);
            if (res != null) {
                feed = (Feed) res[0];
                queuedItemsIds = (LongList) res[1];
                newItemsIds = res[2] == null ? null : (LongList) res[2];
                itemsLoaded = true;
                if (viewsCreated) {
                    onFragmentLoaded();
                }
            }
        }
    }
}
