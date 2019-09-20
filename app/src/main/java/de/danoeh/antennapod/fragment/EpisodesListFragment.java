package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
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
import android.widget.TextView;
import android.widget.Toast;
import com.joanzapata.iconify.Iconify;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.AllEpisodesRecycleAdapter;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.feed.EventDistributor;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedItemFilter;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.service.download.Downloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.dialog.FilterDialog;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Shows unread or recently published episodes
 */
public abstract class EpisodesListFragment extends Fragment {

    public static final String TAG = "EpisodesListFragment";

    private static final int EVENTS = EventDistributor.FEED_LIST_UPDATE |
            EventDistributor.UNREAD_ITEMS_UPDATE |
            EventDistributor.PLAYER_STATUS_UPDATE;

    private static final String DEFAULT_PREF_NAME = "PrefAllEpisodesFragment";
    private static final String PREF_SCROLL_POSITION = "scroll_position";
    private static final String PREF_SCROLL_OFFSET = "scroll_offset";

    RecyclerView recyclerView;
    AllEpisodesRecycleAdapter listAdapter;
    ProgressBar progLoading;
    EmptyViewHandler emptyView;

    @NonNull
    List<FeedItem> episodes = new ArrayList<>();
    @NonNull
    private List<Downloader> downloaderList = new ArrayList<>();

    private boolean isUpdatingFeeds;
    boolean isMenuInvalidationAllowed = false;

    protected Disposable disposable;
    private LinearLayoutManager layoutManager;
    protected TextView txtvInformation;

    boolean showOnlyNewEpisodes() {
        return false;
    }

    String getPrefName() {
        return DEFAULT_PREF_NAME;
    }

    @Override
    public void onStart() {
        super.onStart();
        setHasOptionsMenu(true);
        EventDistributor.getInstance().register(contentUpdate);
        EventBus.getDefault().register(this);
        loadItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerForContextMenu(recyclerView);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveScrollPosition();
        unregisterForContextMenu(recyclerView);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        EventDistributor.getInstance().unregister(contentUpdate);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void saveScrollPosition() {
        int firstItem = layoutManager.findFirstVisibleItemPosition();
        View firstItemView = layoutManager.findViewByPosition(firstItem);
        float topOffset;
        if (firstItemView == null) {
            topOffset = 0;
        } else {
            topOffset = firstItemView.getTop();
        }

        SharedPreferences prefs = getActivity().getSharedPreferences(getPrefName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_SCROLL_POSITION, firstItem);
        editor.putFloat(PREF_SCROLL_OFFSET, topOffset);
        editor.apply();
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
            editor.apply();
        }
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFeeds();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.episodes, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView sv = (SearchView) MenuItemCompat.getActionView(searchItem);
        MenuItemUtils.adjustTextColor(getActivity(), sv);
        sv.setQueryHint(getString(R.string.search_hint));
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                sv.clearFocus();
                ((MainActivity) requireActivity()).loadChildFragment(SearchFragment.newInstance(s));
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
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
                    ConfirmationDialog markAllReadConfirmationDialog = new ConfirmationDialog(getActivity(),
                            R.string.mark_all_read_label,
                            R.string.mark_all_read_confirmation_msg) {

                        @Override
                        public void onConfirmButtonPressed(DialogInterface dialog) {
                            dialog.dismiss();
                            DBWriter.markAllItemsRead();
                            Toast.makeText(getActivity(), R.string.mark_all_read_msg, Toast.LENGTH_SHORT).show();
                        }
                    };
                    markAllReadConfirmationDialog.createNewDialog().show();
                    return true;
                case R.id.remove_all_new_flags_item:
                    ConfirmationDialog removeAllNewFlagsConfirmationDialog = new ConfirmationDialog(getActivity(),
                            R.string.remove_all_new_flags_label,
                            R.string.remove_all_new_flags_confirmation_msg) {

                        @Override
                        public void onConfirmButtonPressed(DialogInterface dialog) {
                            dialog.dismiss();
                            DBWriter.removeAllNewFlags();
                            Toast.makeText(getActivity(), R.string.removed_all_new_flags_msg, Toast.LENGTH_SHORT).show();
                        }
                    };
                    removeAllNewFlagsConfirmationDialog.createNewDialog().show();
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
        if (!getUserVisibleHint()) {
            return false;
        }
        if (!isVisible()) {
            return false;
        }
        if (item.getItemId() == R.id.share_item) {
            return true; // avoids that the position is reset when we need it in the submenu
        }

        if (listAdapter.getSelectedItem() == null) {
            Log.i(TAG, "Selected item or listAdapter was null, ignoring selection");
            return super.onContextItemSelected(item);
        }
        FeedItem selectedItem = listAdapter.getSelectedItem();

        // Remove new flag contains UI logic specific to All/New/FavoriteSegments,
        // e.g., Undo with Snackbar,
        // and is handled by this class rather than the generic FeedItemMenuHandler
        // Undo is useful for Remove new flag, given there is no UI to undo it otherwise,
        // i.e., there is context menu item for Mark as new
        if (R.id.remove_new_flag_item == item.getItemId()) {
            removeNewFlagWithUndo(selectedItem);
            return true;
        }

        return FeedItemMenuHandler.onMenuItemClicked(getActivity(), item.getItemId(), selectedItem);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.all_episodes_fragment, container, false);
        txtvInformation = root.findViewById(R.id.txtvInformation);

        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView = root.findViewById(android.R.id.list);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity()).build());
        recyclerView.setVisibility(View.GONE);

        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        progLoading = root.findViewById(R.id.progLoading);
        progLoading.setVisibility(View.VISIBLE);

        emptyView = new EmptyViewHandler(getContext());
        emptyView.attachToRecyclerView(recyclerView);
        emptyView.setIcon(R.attr.feed);
        emptyView.setTitle(R.string.no_all_episodes_head_label);
        emptyView.setMessage(R.string.no_all_episodes_label);

        createRecycleAdapter(recyclerView, emptyView);
        emptyView.hide();

        return root;
    }

    protected void onFragmentLoaded(List<FeedItem> episodes) {
        listAdapter.notifyDataSetChanged();

        if (episodes.size() == 0) {
            createRecycleAdapter(recyclerView, emptyView);
        }

        restoreScrollPosition();
        requireActivity().invalidateOptionsMenu();
    }

    /**
     * Currently, we need to recreate the list adapter in order to be able to undo last item via the
     * snackbar. See #3084 for details.
     */
    private void createRecycleAdapter(RecyclerView recyclerView, EmptyViewHandler emptyViewHandler) {
        MainActivity mainActivity = (MainActivity) getActivity();
        listAdapter = new AllEpisodesRecycleAdapter(mainActivity, itemAccess, showOnlyNewEpisodes());
        listAdapter.setHasStableIds(true);
        recyclerView.setAdapter(listAdapter);
        emptyViewHandler.updateAdapter(listAdapter);
    }

    private final AllEpisodesRecycleAdapter.ItemAccess itemAccess = new AllEpisodesRecycleAdapter.ItemAccess() {

        @Override
        public int getCount() {
            return episodes.size();
        }

        @Override
        public FeedItem getItem(int position) {
            if (0 <= position && position < episodes.size()) {
                return episodes.get(position);
            }
            return null;
        }

        @Override
        public LongList getItemsIds() {
            LongList ids = new LongList(episodes.size());
            for (FeedItem episode : episodes) {
                ids.add(episode.getId());
            }
            return ids;
        }

        @Override
        public int getItemDownloadProgressPercent(FeedItem item) {
            for (Downloader downloader : downloaderList) {
                DownloadRequest downloadRequest = downloader.getDownloadRequest();
                if (downloadRequest.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                        && downloadRequest.getFeedfileId() == item.getMedia().getId()) {
                    return downloadRequest.getProgressPercent();
                }
            }
            return 0;
        }

        @Override
        public boolean isInQueue(FeedItem item) {
            return item != null && item.isTagged(FeedItem.TAG_QUEUE);
        }

        @Override
        public LongList getQueueIds() {
            LongList queueIds = new LongList();
            for (FeedItem item : episodes) {
                if (item.isTagged(FeedItem.TAG_QUEUE)) {
                    queueIds.add(item.getId());
                }
            }
            return queueIds;
        }

    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        for (FeedItem item : event.items) {
            int pos = FeedItemUtil.indexOfItemWithId(episodes, item.getId());
            if (pos >= 0) {
                episodes.remove(pos);
                if (shouldUpdatedItemRemainInList(item)) {
                    episodes.add(pos, item);
                    listAdapter.notifyItemChanged(pos);
                } else {
                    listAdapter.notifyItemRemoved(pos);
                }
            }
        }
    }

    protected boolean shouldUpdatedItemRemainInList(FeedItem item) {
        return true;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        downloaderList = update.downloaders;
        if (isMenuInvalidationAllowed && isUpdatingFeeds != update.feedIds.length > 0) {
            requireActivity().invalidateOptionsMenu();
        }
        if (update.mediaIds.length > 0) {
            for (long mediaId : update.mediaIds) {
                int pos = FeedItemUtil.indexOfItemWithMediaId(episodes, mediaId);
                if (pos >= 0) {
                    listAdapter.notifyItemChanged(pos);
                }
            }
        }
    }

    private final EventDistributor.EventListener contentUpdate = new EventDistributor.EventListener() {
        @Override
        public void update(EventDistributor eventDistributor, Integer arg) {
            if ((arg & EVENTS) != 0) {
                loadItems();
                if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing()) {
                    requireActivity().invalidateOptionsMenu();
                }
            }
        }
    };

    void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(this::loadData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    progLoading.setVisibility(View.GONE);
                    episodes = data;
                    onFragmentLoaded(episodes);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    protected abstract List<FeedItem> loadData();

    void removeNewFlagWithUndo(FeedItem item) {
        if (item == null) {
            return;
        }

        Log.d(TAG, "removeNewFlagWithUndo(" + item.getId() + ")");
        if (disposable != null) {
            disposable.dispose();
        }
        // we're marking it as unplayed since the user didn't actually play it
        // but they don't want it considered 'NEW' anymore
        DBWriter.markItemPlayed(FeedItem.UNPLAYED, item.getId());

        final Handler h = new Handler(getActivity().getMainLooper());
        final Runnable r = () -> {
            FeedMedia media = item.getMedia();
            if (media != null && media.hasAlmostEnded() && UserPreferences.isAutoDelete()) {
                DBWriter.deleteFeedMediaOfItem(getActivity(), media.getId());
            }
        };

        Snackbar snackbar = Snackbar.make(getView(), getString(R.string.removed_new_flag_label),
                Snackbar.LENGTH_LONG);
        snackbar.setAction(getString(R.string.undo), v -> {
            DBWriter.markItemPlayed(FeedItem.NEW, item.getId());
            // don't forget to cancel the thing that's going to remove the media
            h.removeCallbacks(r);
        });
        snackbar.show();
        h.postDelayed(r, (int) Math.ceil(snackbar.getDuration() * 1.05f));
    }
}
