package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.ListFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.widget.IconTextView;

import org.apache.commons.lang3.Validate;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.FeedItemlistAdapter;
import de.danoeh.antennapod.core.asynctask.FeedRemover;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.event.FeedItemEvent;

import de.danoeh.antennapod.core.event.FeedListUpdateEvent;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.event.PlayerStatusEvent;
import de.danoeh.antennapod.core.event.UnreadItemsUpdateEvent;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedItemFilter;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.glide.FastBlurTransformation;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.FeedItemPermutors;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.Optional;
import de.danoeh.antennapod.core.util.gui.MoreContentListFooterUtil;
import de.danoeh.antennapod.dialog.EpisodesApplyActionFragment;
import de.danoeh.antennapod.dialog.RenameFeedDialog;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays a list of FeedItems.
 */
@SuppressLint("ValidFragment")
public class FeedItemlistFragment extends ListFragment {
    private static final String TAG = "ItemlistFragment";
    private static final String ARGUMENT_FEED_ID = "argument.de.danoeh.antennapod.feed_id";

    private FeedItemlistAdapter adapter;
    private ContextMenu contextMenu;
    private AdapterView.AdapterContextMenuInfo lastMenuInfo = null;

    private long feedID;
    private Feed feed;

    private boolean headerCreated = false;

    private List<Downloader> downloaderList;

    private MoreContentListFooterUtil listFooter;

    private boolean isUpdatingFeed;

    private TextView txtvTitle;
    private IconTextView txtvFailure;
    private ImageView imgvBackground;
    private ImageView imgvCover;

    private TextView txtvInformation;

    private Disposable disposable;

    /**
     * Creates new ItemlistFragment which shows the Feeditems of a specific
     * feed. Sets 'showFeedtitle' to false
     *
     * @param feedId The id of the feed to show
     * @return the newly created instance of an ItemlistFragment
     */
    public static FeedItemlistFragment newInstance(long feedId) {
        FeedItemlistFragment i = new FeedItemlistFragment();
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
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getActivity() != null) {
            ((MainActivity) getActivity()).getSupportActionBar().setTitle("");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        registerForContextMenu(getListView());

        EventBus.getDefault().register(this);
        loadItems();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
        adapter = null;
        listFooter = null;
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker = new MenuItemUtils.UpdateRefreshMenuItemChecker() {
        @Override
        public boolean isRefreshing() {
            return feed != null && DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFile(feed);
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);

        FeedMenuHandler.onCreateOptionsMenu(inflater, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView sv = (SearchView) MenuItemCompat.getActionView(searchItem);
        sv.setQueryHint(getString(R.string.search_label));
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
             @Override
             public boolean onMenuItemActionExpand(MenuItem item) {
                 menu.findItem(R.id.sort_items).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                 menu.findItem(R.id.filter_items).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                 menu.findItem(R.id.episode_actions).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                 menu.findItem(R.id.refresh_item).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                 return true;
             }

             @Override
             public boolean onMenuItemActionCollapse(MenuItem item) {
                 getActivity().invalidateOptionsMenu();
                 return true;
             }
         });
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                sv.clearFocus();
                if (feed != null) {
                    ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance(s, feed.getId()));
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        if (feed == null || feed.getLink() == null) {
            menu.findItem(R.id.share_link_item).setVisible(false);
            menu.findItem(R.id.visit_website_item).setVisible(false);
        }

        isUpdatingFeed = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (feed != null) {
            FeedMenuHandler.onPrepareOptionsMenu(menu, feed);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            if (feed == null) {
                Toast.makeText(getContext(), R.string.please_wait_for_data, Toast.LENGTH_LONG).show();
                return true;
            }
            try {
                if (!FeedMenuHandler.onOptionsItemClicked(getActivity(), item, feed)) {
                    switch (item.getItemId()) {
                        case R.id.episode_actions:
                            EpisodesApplyActionFragment fragment = EpisodesApplyActionFragment
                                    .newInstance(feed.getItems());
                            ((MainActivity)getActivity()).loadChildFragment(fragment);
                            return true;
                        case R.id.rename_item:
                            new RenameFeedDialog(getActivity(), feed).show();
                            return true;
                        case R.id.remove_item:
                            final FeedRemover remover = new FeedRemover(
                                    getActivity(), feed) {
                                @Override
                                protected void onPostExecute(Void result) {
                                    super.onPostExecute(result);
                                    ((MainActivity) getActivity()).loadFragment(EpisodesFragment.TAG, null);
                                }
                            };
                            ConfirmationDialog conDialog = new ConfirmationDialog(getActivity(),
                                    R.string.remove_feed_label,
                                    getString(R.string.feed_delete_confirmation_msg, feed.getTitle())) {

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
        FeedItemMenuHandler.onPrepareMenu(menu, item);
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

        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedItem);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if(adapter == null) {
            return;
        }
        position -= l.getHeaderViewsCount();
        MainActivity activity = (MainActivity) getActivity();
        long[] ids = FeedItemUtil.getIds(feed.getItems());
        activity.loadChildFragment(ItemPagerFragment.newInstance(ids, position));
        activity.getSupportActionBar().setTitle(feed.getTitle());
    }

    @Subscribe
    public void onEvent(FeedEvent event) {
        Log.d(TAG, "onEvent() called with: " + "event = [" + event + "]");
        if(event.feedId == feedID) {
            loadItems();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if(feed == null || feed.getItems() == null || adapter == null) {
            return;
        }
        for(FeedItem item : event.items) {
            int pos = FeedItemUtil.indexOfItemWithId(feed.getItems(), item.getId());
            if(pos >= 0) {
                loadItems();
                return;
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        downloaderList = update.downloaders;
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeed)) {
            updateProgressBarVisibility();
        }
        if (adapter != null && update.mediaIds.length > 0) {
            adapter.notifyDataSetChanged();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (adapter != null) {
            adapter.notifyCurrentlyPlayingItemChanged(event, getListView());
        }
    }

    private void updateUi() {
        refreshHeaderView();
        loadItems();
        updateProgressBarVisibility();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        updateUi();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        updateUi();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        if (event.contains(feed)) {
            updateUi();
        }
    }

    private void updateProgressBarVisibility() {
        if (isUpdatingFeed != updateRefreshMenuItemChecker.isRefreshing()) {
            getActivity().supportInvalidateOptionsMenu();
        }
        if (listFooter != null) {
            listFooter.setLoadingState(DownloadRequester.getInstance().isDownloadingFeeds());
        }

    }

    private void displayList() {
        if (getView() == null) {
            Log.e(TAG, "Required root view is not yet created. Stop binding data to UI.");
            return;
        }
        if (adapter == null) {
            setListAdapter(null);
            setupHeaderView();
            setupFooterView();
            adapter = new FeedItemlistAdapter(getActivity(), itemAccess, false, true);
            setListAdapter(adapter);
        }
        refreshHeaderView();
        setListShown(true);
        adapter.notifyDataSetChanged();

        getActivity().supportInvalidateOptionsMenu();

        if (feed != null && feed.getNextPageLink() == null && listFooter != null) {
            getListView().removeFooterView(listFooter.getRoot());
        }
    }

    private void refreshHeaderView() {
        if (getListView() == null || feed == null || !headerCreated) {
            Log.e(TAG, "Unable to refresh header view");
            return;
        }
        loadFeedImage();
        if(feed.hasLastUpdateFailed()) {
            txtvFailure.setVisibility(View.VISIBLE);
        } else {
            txtvFailure.setVisibility(View.GONE);
        }
        txtvTitle.setText(feed.getTitle());
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

    private void setupHeaderView() {
        if (getListView() == null || feed == null) {
            Log.e(TAG, "Unable to setup listview: recyclerView = null or feed = null");
            return;
        }
        ListView lv = getListView();
        LayoutInflater inflater = (LayoutInflater)
                getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View header = inflater.inflate(R.layout.feeditemlist_header, lv, false);
        lv.addHeaderView(header);

        txtvTitle = header.findViewById(R.id.txtvTitle);
        TextView txtvAuthor = header.findViewById(R.id.txtvAuthor);
        imgvBackground = header.findViewById(R.id.imgvBackground);
        imgvCover = header.findViewById(R.id.imgvCover);
        ImageButton butShowInfo = header.findViewById(R.id.butShowInfo);
        ImageButton butShowSettings = header.findViewById(R.id.butShowSettings);
        txtvInformation = header.findViewById(R.id.txtvInformation);
        txtvFailure = header.findViewById(R.id.txtvFailure);

        txtvTitle.setText(feed.getTitle());
        txtvAuthor.setText(feed.getAuthor());


        // https://github.com/bumptech/glide/issues/529
        imgvBackground.setColorFilter(new LightingColorFilter(0xff828282, 0x000000));

        loadFeedImage();

        butShowInfo.setOnClickListener(v -> showFeedInfo());
        imgvCover.setOnClickListener(v -> showFeedInfo());
        butShowSettings.setOnClickListener(v -> {
            if (feed != null) {
                FeedSettingsFragment fragment = FeedSettingsFragment.newInstance(feed);
                ((MainActivity) getActivity()).loadChildFragment(fragment, TransitionEffect.SLIDE);
            }
        });
        headerCreated = true;
    }

    private void showFeedInfo() {
        if (feed != null) {
            FeedInfoFragment fragment = FeedInfoFragment.newInstance(feed);
            ((MainActivity) getActivity()).loadChildFragment(fragment, TransitionEffect.SLIDE);
        }
    }

    private void loadFeedImage() {
        Glide.with(getActivity())
                .load(feed.getImageLocation())
                .apply(new RequestOptions()
                    .placeholder(R.color.image_readability_tint)
                    .error(R.color.image_readability_tint)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .transform(new FastBlurTransformation())
                    .dontAnimate())
                .into(imgvBackground);

        Glide.with(getActivity())
                .load(feed.getImageLocation())
                .apply(new RequestOptions()
                    .placeholder(R.color.light_gray)
                    .error(R.color.light_gray)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .fitCenter()
                    .dontAnimate())
                .into(imgvCover);
    }


    private void setupFooterView() {
        if (getListView() == null || feed == null) {
            Log.e(TAG, "Unable to setup listview: recyclerView = null or feed = null");
            return;
        }
        if (feed.isPaged() && feed.getNextPageLink() != null) {
            ListView lv = getListView();
            LayoutInflater inflater = (LayoutInflater)
                    getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View header = inflater.inflate(R.layout.more_content_list_footer, lv, false);
            lv.addFooterView(header);
            listFooter = new MoreContentListFooterUtil(header);
            listFooter.setClickListener(() -> {
                if (feed != null) {
                    try {
                        DBTasks.loadNextPageOfFeed(getActivity(), feed, false);
                    } catch (DownloadRequestException e) {
                        e.printStackTrace();
                        DownloadRequestErrorDialogCreator.newRequestErrorDialog(getActivity(), e.getMessage());
                    }
                }
            });
        }
    }

    private final FeedItemlistAdapter.ItemAccess itemAccess = new FeedItemlistAdapter.ItemAccess() {

        @Override
        public FeedItem getItem(int position) {
            if (feed != null && 0 <= position && position < feed.getNumOfItems()) {
                return feed.getItemAtIndex(position);
            } else {
                return null;
            }
        }

        @Override
        public LongList getQueueIds() {
            LongList queueIds = new LongList();
            if(feed == null) {
                return queueIds;
            }
            for(FeedItem item : feed.getItems()) {
                if(item.isTagged(FeedItem.TAG_QUEUE)) {
                    queueIds.add(item.getId());
                }
            }
            return queueIds;
        }

        @Override
        public int getCount() {
            return (feed != null) ? feed.getNumOfItems() : 0;
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


    private void loadItems() {
        if(disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(this::loadData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    feed = result.orElse(null);
                    displayList();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    private Optional<Feed> loadData() {
        Feed feed = DBReader.getFeed(feedID);
        if (feed != null && feed.getItemFilter() != null) {
            DBReader.loadAdditionalFeedItemListData(feed.getItems());
            FeedItemFilter filter = feed.getItemFilter();
            feed.setItems(filter.filter(feed.getItems()));
        }
        if (feed != null && feed.getSortOrder() != null) {
            List<FeedItem> feedItems = feed.getItems();
            FeedItemPermutors.getPermutor(feed.getSortOrder()).reorder(feedItems);
            feed.setItems(feedItems);
        }
        return Optional.ofNullable(feed);
    }

}
