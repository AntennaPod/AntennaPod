package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.AllEpisodesRecycleAdapter;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Shows unread or recently published episodes
 */
public class AllEpisodesFragment extends Fragment {

    public static final String TAG = "AllEpisodesFragment";

    private static final int EVENTS = EventDistributor.FEED_LIST_UPDATE |
            EventDistributor.UNREAD_ITEMS_UPDATE |
            EventDistributor.PLAYER_STATUS_UPDATE;

    private static final int RECENT_EPISODES_LIMIT = 150;
    private static final String DEFAULT_PREF_NAME = "PrefAllEpisodesFragment";
    private static final String PREF_SCROLL_POSITION = "scroll_position";
    private static final String PREF_SCROLL_OFFSET = "scroll_offset";

    protected RecyclerView recyclerView;
    private AllEpisodesRecycleAdapter listAdapter;
    private ProgressBar progLoading;

    private List<FeedItem> episodes;
    private List<Downloader> downloaderList;

    private boolean itemsLoaded = false;
    private boolean viewsCreated = false;

    private AtomicReference<MainActivity> activity = new AtomicReference<MainActivity>();

    private boolean isUpdatingFeeds;

    protected Subscription subscription;
    private LinearLayoutManager layoutManager;

    protected boolean showOnlyNewEpisodes() { return false; }
    protected String getPrefName() { return DEFAULT_PREF_NAME; }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDistributor.getInstance().register(contentUpdate);
        this.activity.set((MainActivity) getActivity());
        if (viewsCreated && itemsLoaded) {
            onFragmentLoaded();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
        loadItems();
        registerForContextMenu(recyclerView);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        saveScrollPosition();
        unregisterForContextMenu(recyclerView);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventDistributor.getInstance().unregister(contentUpdate);
        if(subscription != null) {
            subscription.unsubscribe();
        }
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

    private void saveScrollPosition() {
        int firstItem = layoutManager.findFirstVisibleItemPosition();
        View firstItemView = layoutManager.findViewByPosition(firstItem);
        float topOffset;
        if(firstItemView == null) {
            topOffset = 0;
        } else {
            topOffset = firstItemView.getTop();
        }

        SharedPreferences prefs = getActivity().getSharedPreferences(getPrefName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_SCROLL_POSITION, firstItem);
        editor.putFloat(PREF_SCROLL_OFFSET, topOffset);
        editor.commit();
    }

    private void restoreScrollPosition() {
        SharedPreferences prefs = getActivity().getSharedPreferences(getPrefName(), Context.MODE_PRIVATE);
        int position = prefs.getInt(PREF_SCROLL_POSITION, 0);
        float offset = prefs.getFloat(PREF_SCROLL_OFFSET, 0.0f);
        if (position > 0 || offset > 0) {
            layoutManager.scrollToPositionWithOffset(position, (int) offset);
            // restore once, then forget
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_SCROLL_POSITION, 0);
            editor.putFloat(PREF_SCROLL_OFFSET, 0.0f);
            editor.commit();
        }
    }

    protected void resetViewState() {
        listAdapter = null;
        activity.set(null);
        viewsCreated = false;
    }


    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker = new MenuItemUtils.UpdateRefreshMenuItemChecker() {
        @Override
        public boolean isRefreshing() {
            return DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds();
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (itemsLoaded) {
            inflater.inflate(R.menu.new_episodes, menu);

            MenuItem searchItem = menu.findItem(R.id.action_search);
            final SearchView sv = (SearchView) MenuItemCompat.getActionView(searchItem);
            MenuItemUtils.adjustTextColor(getActivity(), sv);
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
            isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (itemsLoaded) {
            MenuItem menuItem = menu.findItem(R.id.mark_all_read_item);
            if (menuItem != null) {
                menuItem.setVisible(episodes != null && !episodes.isEmpty());
            }
        }
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
                    ConfirmationDialog conDialog = new ConfirmationDialog(getActivity(),
                            R.string.mark_all_read_label,
                            R.string.mark_all_read_confirmation_msg) {

                        @Override
                        public void onConfirmButtonPressed(
                                DialogInterface dialog) {
                            dialog.dismiss();
                            DBWriter.markAllItemsRead();
                            Toast.makeText(getActivity(), R.string.mark_all_read_msg, Toast.LENGTH_SHORT).show();
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

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d(TAG, "onContextItemSelected() called with: " + "item = [" + item + "]");
        if(!isVisible()) {
            return false;
        }
        if(item.getItemId() == R.id.share_item) {
            return true; // avoids that the position is reset when we need it in the submenu
        }
        int pos = listAdapter.getPosition();
        if(pos < 0) {
            return false;
        }
        FeedItem selectedItem = itemAccess.getItem(pos);

        if (selectedItem == null) {
            Log.i(TAG, "Selected item at position " + pos + " was null, ignoring selection");
            return super.onContextItemSelected(item);
        }

        try {
            return FeedItemMenuHandler.onMenuItemClicked(getActivity(), item.getItemId(), selectedItem);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
            return true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return onCreateViewHelper(inflater, container, savedInstanceState,
                R.layout.all_episodes_fragment);
    }

    protected View onCreateViewHelper(LayoutInflater inflater,
                                      ViewGroup container,
                                      Bundle savedInstanceState,
                                      int fragmentResource) {
        super.onCreateView(inflater, container, savedInstanceState);

        View root = inflater.inflate(fragmentResource, container, false);

        recyclerView = (RecyclerView) root.findViewById(android.R.id.list);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity()).build());

        progLoading = (ProgressBar) root.findViewById(R.id.progLoading);

        if (!itemsLoaded) {
            progLoading.setVisibility(View.VISIBLE);
        }

        viewsCreated = true;

        if (itemsLoaded && activity.get() != null) {
            onFragmentLoaded();
        }

        return root;
    }

    private void onFragmentLoaded() {
        if (listAdapter == null) {
            MainActivity mainActivity = activity.get();
            listAdapter = new AllEpisodesRecycleAdapter(mainActivity, itemAccess,
                    new DefaultActionButtonCallback(mainActivity), showOnlyNewEpisodes());
            recyclerView.setAdapter(listAdapter);
        }
        listAdapter.notifyDataSetChanged();
        restoreScrollPosition();
        getActivity().supportInvalidateOptionsMenu();
        updateShowOnlyEpisodesListViewState();
    }

    protected AllEpisodesRecycleAdapter.ItemAccess itemAccess = new AllEpisodesRecycleAdapter.ItemAccess() {

        @Override
        public int getCount() {
            if (episodes != null) {
                return episodes.size();
            }
            return 0;
        }

        @Override
        public FeedItem getItem(int position) {
            if (episodes != null && 0 <= position && position < episodes.size()) {
                return episodes.get(position);
            }
            return null;
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
            if (item != null) {
                return item.isTagged(FeedItem.TAG_QUEUE);
            }
            return false;
        }
    };

    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if(episodes == null || listAdapter == null) {
            return;
        }
        for(int i=0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemUtil.indexOfItemWithId(episodes, item.getId());
            if(pos >= 0) {
                episodes.remove(pos);
                episodes.add(pos, item);
                listAdapter.notifyItemChanged(pos);
            }
        }
    }


    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        downloaderList = update.downloaders;
        if (isUpdatingFeeds != update.feedIds.length > 0) {
                getActivity().supportInvalidateOptionsMenu();
        }
        if(listAdapter != null && update.mediaIds.length > 0) {
            for(long mediaId : update.mediaIds) {
                int pos = FeedItemUtil.indexOfItemWithMediaId(episodes, mediaId);
                if(pos >= 0) {
                    listAdapter.notifyItemChanged(pos);
                }
            }
        }
    }

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EVENTS) != 0) {
                loadItems();
                if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing()) {
                    getActivity().supportInvalidateOptionsMenu();
                }
            }
        }
    };

    private void updateShowOnlyEpisodesListViewState() {
    }

    protected void loadItems() {
        if(subscription != null) {
            subscription.unsubscribe();
        }
        if (viewsCreated && !itemsLoaded) {
            recyclerView.setVisibility(View.GONE);
            progLoading.setVisibility(View.VISIBLE);
        }
        subscription = Observable.fromCallable(() -> loadData())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    recyclerView.setVisibility(View.VISIBLE);
                    progLoading.setVisibility(View.GONE);
                    if (data != null) {
                        episodes = data;
                        itemsLoaded = true;
                        if (viewsCreated && activity.get() != null) {
                            onFragmentLoaded();
                        }
                    }
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                });
    }

    protected List<FeedItem> loadData() {
        return DBReader.getRecentlyPublishedEpisodes(RECENT_EPISODES_LIMIT);
    }

}
