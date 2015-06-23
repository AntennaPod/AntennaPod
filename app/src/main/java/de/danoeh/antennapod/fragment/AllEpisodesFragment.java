package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mobeta.android.dslv.DragSortListView;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.AllEpisodesListAdapter;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.core.asynctask.DownloadObserver;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
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
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;

/**
 * Shows unread or recently published episodes
 */
public class AllEpisodesFragment extends Fragment {

    public static final String TAG = "AllEpisodesFragment";

    private static final int EVENTS = EventDistributor.DOWNLOAD_HANDLED |
            EventDistributor.DOWNLOAD_QUEUED |
            EventDistributor.UNREAD_ITEMS_UPDATE |
            EventDistributor.PLAYER_STATUS_UPDATE;

    private static final int RECENT_EPISODES_LIMIT = 150;
    private static final String DEFAULT_PREF_NAME = "PrefAllEpisodesFragment";
    private static final String PREF_KEY_LIST_TOP = "list_top";
    private static final String PREF_KEY_LIST_SELECTION = "list_selection";

    private String prefName;
    protected DragSortListView listView;
    private AllEpisodesListAdapter listAdapter;
    private TextView txtvEmpty;
    private ProgressBar progLoading;
    private ContextMenu contextMenu;
    private AdapterView.AdapterContextMenuInfo lastMenuInfo = null;

    private List<FeedItem> episodes;
    private LongList queuedItemsIds;
    private List<Downloader> downloaderList;

    private boolean itemsLoaded = false;
    private boolean viewsCreated = false;
    private final boolean showOnlyNewEpisodes;

    private AtomicReference<MainActivity> activity = new AtomicReference<MainActivity>();

    private DownloadObserver downloadObserver = null;

    private boolean isUpdatingFeeds;

    public AllEpisodesFragment() {
        // by default we show all the episodes
        this(false, DEFAULT_PREF_NAME);
    }

    // this is only going to be called by our sub-class.
    // The Android docs say to avoid non-default constructors
    // but I think this will be OK since it will only be invoked
    // from a fragment via a default constructor
    protected AllEpisodesFragment(boolean showOnlyNewEpisodes, String prefName) {
        this.showOnlyNewEpisodes = showOnlyNewEpisodes;
        this.prefName = prefName;
    }

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
    public void onPause() {
        super.onPause();
        saveScrollPosition();
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

    private void saveScrollPosition() {
        SharedPreferences prefs = getActivity().getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
        editor.putInt(PREF_KEY_LIST_SELECTION, listView.getFirstVisiblePosition());
        editor.putInt(PREF_KEY_LIST_TOP, top);
        editor.commit();
    }

    private void restoreScrollPosition() {
        SharedPreferences prefs = getActivity().getSharedPreferences(prefName, Context.MODE_PRIVATE);
        int listSelection = prefs.getInt(PREF_KEY_LIST_SELECTION, 0);
        int top = prefs.getInt(PREF_KEY_LIST_TOP, 0);
        if (listSelection > 0 || top > 0) {
            listView.setSelectionFromTop(listSelection, top);
            // restore once, then forget
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(PREF_KEY_LIST_SELECTION, 0);
            editor.putInt(PREF_KEY_LIST_TOP, 0);
            editor.commit();
        }
    }

    protected void resetViewState() {
        listAdapter = null;
        activity.set(null);
        viewsCreated = false;
        if (downloadObserver != null) {
            downloadObserver.onPause();
        }
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
                            DBWriter.markAllItemsRead(getActivity());
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return onCreateViewHelper(inflater, container, savedInstanceState,
                R.layout.all_episodes_fragment, R.string.all_episodes_label);
    }

    protected View onCreateViewHelper(LayoutInflater inflater,
                                      ViewGroup container,
                                      Bundle savedInstanceState,
                                      int fragmentResource,
                                      int titleString) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(titleString);

        View root = inflater.inflate(fragmentResource, container, false);

        listView = (DragSortListView) root.findViewById(android.R.id.list);
        txtvEmpty = (TextView) root.findViewById(android.R.id.empty);
        progLoading = (ProgressBar) root.findViewById(R.id.progLoading);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FeedItem item = (FeedItem) listAdapter.getItem(position - listView.getHeaderViewsCount());
                if (item != null) {
                    ((MainActivity) getActivity()).loadChildFragment(ItemFragment.newInstance(item.getId()));
                }

            }
        });

        registerForContextMenu(listView);

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
        FeedItem item = itemAccess.getItem(adapterInfo.position);

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.allepisodes_context, menu);

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
        FeedItem selectedItem = itemAccess.getItem(menuInfo.position);

        if (selectedItem == null) {
            Log.i(TAG, "Selected item at position " + menuInfo.position + " was null, ignoring selection");
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

    private void onFragmentLoaded() {
        if (listAdapter == null) {
            listAdapter = new AllEpisodesListAdapter(activity.get(), itemAccess,
                    new DefaultActionButtonCallback(activity.get()), showOnlyNewEpisodes);
            listView.setAdapter(listAdapter);
            listView.setEmptyView(txtvEmpty);
            downloadObserver = new DownloadObserver(activity.get(), new Handler(), downloadObserverCallback);
            downloadObserver.onResume();
        }
        listAdapter.notifyDataSetChanged();
        restoreScrollPosition();
        getActivity().supportInvalidateOptionsMenu();
        updateShowOnlyEpisodesListViewState();
    }

    private DownloadObserver.Callback downloadObserverCallback = new DownloadObserver.Callback() {
        @Override
        public void onContentChanged(List<Downloader> downloaderList) {
            AllEpisodesFragment.this.downloaderList = downloaderList;
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
    };

    private AllEpisodesListAdapter.ItemAccess itemAccess = new AllEpisodesListAdapter.ItemAccess() {

        @Override
        public int getCount() {
            if (itemsLoaded) {
                return episodes.size();
            }
            return 0;
        }

        @Override
        public FeedItem getItem(int position) {
            if (itemsLoaded) {
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
            if (itemsLoaded) {
                return queuedItemsIds.contains(item.getId());
            } else {
                return false;
            }
        }
    };

    private EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EVENTS) != 0) {
                startItemLoader();
                if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing()) {
                    getActivity().supportInvalidateOptionsMenu();
                }
            }
        }
    };

    private void updateShowOnlyEpisodesListViewState() {
        if (showOnlyNewEpisodes) {
            listView.setEmptyView(null);
            txtvEmpty.setVisibility(View.GONE);
        } else {
            listView.setEmptyView(txtvEmpty);
        }
    }

    private ItemLoader itemLoader;

    protected void startItemLoader() {
        if (itemLoader != null) {
            itemLoader.cancel(true);
        }
        itemLoader = new ItemLoader();
        itemLoader.execute();
    }

    protected void stopItemLoader() {
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
                if(showOnlyNewEpisodes) {
                    return new Object[] {
                            DBReader.getNewItemsList(context),
                            DBReader.getQueueIDList(context),
                            null // see ItemAccess.isNew
                    };
                } else {
                    return new Object[]{
                            DBReader.getRecentlyPublishedEpisodes(context, RECENT_EPISODES_LIMIT),
                            DBReader.getQueueIDList(context)
                    };
                }
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
                episodes = (List<FeedItem>) lists[0];
                queuedItemsIds = (LongList) lists[1];
                itemsLoaded = true;
                if (viewsCreated && activity.get() != null) {
                    onFragmentLoaded();
                }
            }
        }
    }
}
