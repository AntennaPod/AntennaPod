package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.joanzapata.iconify.Iconify;
import com.leinardi.android.speeddial.SpeedDialView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloaderUpdate;
import de.danoeh.antennapod.core.feed.FeedEvent;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.glide.FastBlurTransformation;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.FeedItemPermutors;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.gui.MoreContentListFooterUtil;
import de.danoeh.antennapod.databinding.FeedItemListFragmentBinding;
import de.danoeh.antennapod.databinding.MultiSelectSpeedDialBinding;
import de.danoeh.antennapod.dialog.DownloadLogDetailsDialog;
import de.danoeh.antennapod.dialog.FilterDialog;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
import de.danoeh.antennapod.dialog.RenameItemDialog;
import de.danoeh.antennapod.event.FavoritesEvent;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.QueueEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.fragment.actions.EpisodeMultiSelectActionHandler;
import de.danoeh.antennapod.fragment.swipeactions.SwipeActions;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.view.ToolbarIconTintManager;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.Validate;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Set;

/**
 * Displays a list of FeedItems.
 */
public class FeedItemlistFragment extends Fragment implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener, EpisodeItemListAdapter.OnSelectModeListener {
    public static final String TAG = "ItemlistFragment";
    private static final String ARGUMENT_FEED_ID = "argument.de.danoeh.antennapod.feed_id";
    private static final String KEY_UP_ARROW = "up_arrow";

    private FeedItemListAdapter adapter;
    private SwipeActions swipeActions;
    private MoreContentListFooterUtil nextPageLoader;
    private boolean displayUpArrow;
    private long feedID;
    private Feed feed;
    private boolean headerCreated = false;
    private boolean isUpdatingFeed;
    private Disposable disposable;
    private FeedItemListFragmentBinding viewBinding;
    private MultiSelectSpeedDialBinding speedDialBinding;

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

        Bundle args = getArguments();
        Validate.notNull(args);
        feedID = args.getLong(ARGUMENT_FEED_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewBinding = FeedItemListFragmentBinding.inflate(inflater);
        speedDialBinding = MultiSelectSpeedDialBinding.bind(viewBinding.getRoot());
        viewBinding.toolbar.inflateMenu(R.menu.feedlist);
        viewBinding.toolbar.setOnMenuItemClickListener(this);
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(viewBinding.toolbar, displayUpArrow);
        refreshToolbarState();

        viewBinding.recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        viewBinding.progLoading.setVisibility(View.VISIBLE);
        ToolbarIconTintManager iconTintManager = new ToolbarIconTintManager(
                getContext(), viewBinding.toolbar, viewBinding.collapsingToolbar) {
            @Override
            protected void doTint(Context themedContext) {
                viewBinding.toolbar.getMenu().findItem(R.id.refresh_item)
                        .setIcon(AppCompatResources.getDrawable(themedContext, R.drawable.ic_refresh));
                viewBinding.toolbar.getMenu().findItem(R.id.action_search)
                        .setIcon(AppCompatResources.getDrawable(themedContext, R.drawable.ic_search));
            }
        };
        iconTintManager.updateTint();
        viewBinding.appBar.addOnOffsetChangedListener(iconTintManager);

        nextPageLoader = new MoreContentListFooterUtil(viewBinding.moreContent.moreContentListFooter);
        nextPageLoader.setClickListener(() -> {
            if (feed != null) {
                DBTasks.loadNextPageOfFeed(getActivity(), feed, false);
            }
        });
        viewBinding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView view, int deltaX, int deltaY) {
                super.onScrolled(view, deltaX, deltaY);
                boolean hasMorePages = feed != null && feed.isPaged() && feed.getNextPageLink() != null;
                nextPageLoader.getRoot().setVisibility(
                        (viewBinding.recyclerView.isScrolledToBottom() && hasMorePages) ? View.VISIBLE : View.GONE);
            }
        });

        EventBus.getDefault().register(this);

        viewBinding.swipeRefresh.setDistanceToTriggerSync(getResources().getInteger(R.integer.swipe_refresh_distance));
        viewBinding.swipeRefresh.setOnRefreshListener(() -> {
            DBTasks.forceRefreshFeed(requireContext(), feed, true);
            new Handler(Looper.getMainLooper()).postDelayed(() -> viewBinding.swipeRefresh.setRefreshing(false),
                    getResources().getInteger(R.integer.swipe_to_refresh_duration_in_ms));
        });

        loadItems();

        // Init action UI (via a FAB Speed Dial)
        speedDialBinding.fabSD.setOverlayLayout(speedDialBinding.fabSDOverlay);
        speedDialBinding.fabSD.inflate(R.menu.episodes_apply_action_speeddial);
        speedDialBinding.fabSD.setOnChangeListener(new SpeedDialView.OnChangeListener() {
            @Override
            public boolean onMainActionSelected() {
                return false;
            }

            @Override
            public void onToggleChanged(boolean open) {
                if (open && adapter.getSelectedCount() == 0) {
                    ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.no_items_selected,
                            Snackbar.LENGTH_SHORT);
                    speedDialBinding.fabSD.close();
                }
            }
        });
        speedDialBinding.fabSD.setOnActionSelectedListener(actionItem -> {
            new EpisodeMultiSelectActionHandler(((MainActivity) getActivity()), adapter.getSelectedItems())
                    .handleAction(actionItem.getId());
            adapter.endSelectMode();
            return true;
        });
        return viewBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
        if (adapter != null) {
            adapter.endSelectMode();
        }
        adapter = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadService.isDownloadingFile(feed.getDownload_url());

    private void refreshToolbarState() {
        if (feed == null) {
            return;
        }
        viewBinding.toolbar.getMenu().findItem(R.id.visit_website_item).setVisible(feed.getLink() != null);

        isUpdatingFeed = MenuItemUtils.updateRefreshMenuItem(viewBinding.toolbar.getMenu(),
                R.id.refresh_item, updateRefreshMenuItemChecker);
        FeedMenuHandler.onPrepareOptionsMenu(viewBinding.toolbar.getMenu(), feed);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int horizontalSpacing = (int) getResources().getDimension(R.dimen.additional_horizontal_spacing);
        viewBinding.header.headerContainer.setPadding(
                horizontalSpacing, viewBinding.header.headerContainer.getPaddingTop(),
                horizontalSpacing, viewBinding.header.headerContainer.getPaddingBottom());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (feed == null) {
            ((MainActivity) getActivity()).showSnackbarAbovePlayer(
                    R.string.please_wait_for_data, Toast.LENGTH_LONG);
            return true;
        }
        boolean feedMenuHandled = FeedMenuHandler.onOptionsItemClicked(getActivity(), item, feed);
        if (feedMenuHandled) {
            return true;
        }
        final int itemId = item.getItemId();
        if (itemId == R.id.rename_item) {
            new RenameItemDialog(getActivity(), feed).show();
            return true;
        } else if (itemId == R.id.remove_feed) {
            ((MainActivity) getActivity()).loadFragment(EpisodesFragment.TAG, null);
            RemoveFeedDialog.show(getContext(), feed);
            return true;
        } else if (itemId == R.id.action_search) {
            ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance(feed.getId(), feed.getTitle()));
            return true;
        }
        return false;
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        FeedItem selectedItem = adapter.getLongPressedItem();
        if (selectedItem == null) {
            Log.i(TAG, "Selected item at current position was null, ignoring selection");
            return super.onContextItemSelected(item);
        }
        if (adapter.onContextItemSelected(item)) {
            return true;
        }
        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedItem);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (adapter == null) {
            return;
        }
        MainActivity activity = (MainActivity) getActivity();
        long[] ids = FeedItemUtil.getIds(feed.getItems());
        activity.loadChildFragment(ItemPagerFragment.newInstance(ids, position));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(FeedEvent event) {
        Log.d(TAG, "onEvent() called with: " + "event = [" + event + "]");
        if (event.feedId == feedID) {
            loadItems();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (feed == null || feed.getItems() == null) {
            return;
        } else if (adapter == null) {
            loadItems();
            return;
        }
        for (int i = 0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemUtil.indexOfItemWithId(feed.getItems(), item.getId());
            if (pos >= 0) {
                feed.getItems().remove(pos);
                feed.getItems().add(pos, item);
                adapter.notifyItemChangedCompat(pos);
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        DownloaderUpdate update = event.update;
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeed)) {
            updateSyncProgressBarVisibility();
        }
        if (adapter != null && update.mediaIds.length > 0 && feed != null) {
            for (long mediaId : update.mediaIds) {
                int pos = FeedItemUtil.indexOfItemWithMediaId(feed.getItems(), mediaId);
                if (pos >= 0) {
                    adapter.notifyItemChangedCompat(pos);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (adapter != null) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                EpisodeItemViewHolder holder = (EpisodeItemViewHolder)
                        viewBinding.recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null && holder.isCurrentlyPlayingItem()) {
                    holder.notifyPlaybackPositionUpdated(event);
                    break;
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void favoritesChanged(FavoritesEvent event) {
        updateUi();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onQueueChanged(QueueEvent event) {
        updateUi();
    }

    @Override
    public void onStartSelectMode() {
        swipeActions.detach();
        if (feed.isLocalFeed()) {
            speedDialBinding.fabSD.removeActionItemById(R.id.download_batch);
            speedDialBinding.fabSD.removeActionItemById(R.id.delete_batch);
        }
        speedDialBinding.fabSD.setVisibility(View.VISIBLE);
        refreshToolbarState();
    }

    @Override
    public void onEndSelectMode() {
        speedDialBinding.fabSD.close();
        speedDialBinding.fabSD.setVisibility(View.GONE);
        swipeActions.attachTo(viewBinding.recyclerView);
    }

    private void updateUi() {
        loadItems();
        updateSyncProgressBarVisibility();
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
        if (feed != null && event.contains(feed)) {
            updateUi();
        }
    }

    private void updateSyncProgressBarVisibility() {
        if (isUpdatingFeed != updateRefreshMenuItemChecker.isRefreshing()) {
            refreshToolbarState();
        }
        if (!DownloadService.isDownloadingFeeds()) {
            nextPageLoader.getRoot().setVisibility(View.GONE);
        }
        nextPageLoader.setLoadingState(DownloadService.isDownloadingFeeds());
    }

    private void displayList() {
        if (getView() == null) {
            Log.e(TAG, "Required root view is not yet created. Stop binding data to UI.");
            return;
        }
        if (adapter == null) {
            viewBinding.recyclerView.setAdapter(null);
            adapter = new FeedItemListAdapter((MainActivity) getActivity());
            adapter.setOnSelectModeListener(this);
            viewBinding.recyclerView.setAdapter(adapter);
            swipeActions = new SwipeActions(this, TAG).attachTo(viewBinding.recyclerView);
        }
        viewBinding.progLoading.setVisibility(View.GONE);
        if (feed != null) {
            adapter.updateItems(feed.getItems());
            swipeActions.setFilter(feed.getItemFilter());
        }

        refreshToolbarState();
        updateSyncProgressBarVisibility();
    }

    private void refreshHeaderView() {
        setupHeaderView();
        if (viewBinding == null || feed == null) {
            Log.e(TAG, "Unable to refresh header view");
            return;
        }
        loadFeedImage();
        if (feed.hasLastUpdateFailed()) {
            viewBinding.header.txtvFailure.setVisibility(View.VISIBLE);
        } else {
            viewBinding.header.txtvFailure.setVisibility(View.GONE);
        }
        if (!feed.getPreferences().getKeepUpdated()) {
            viewBinding.header.txtvUpdatesDisabled.setText("{md-pause-circle-outline} "
                    + this.getString(R.string.updates_disabled_label));
            Iconify.addIcons(viewBinding.header.txtvUpdatesDisabled);
            viewBinding.header.txtvUpdatesDisabled.setVisibility(View.VISIBLE);
        } else {
            viewBinding.header.txtvUpdatesDisabled.setVisibility(View.GONE);
        }
        viewBinding.header.txtvTitle.setText(feed.getTitle());
        viewBinding.header.txtvAuthor.setText(feed.getAuthor());
        if (feed.getItemFilter() != null) {
            FeedItemFilter filter = feed.getItemFilter();
            if (filter.getValues().length > 0) {
                viewBinding.header.txtvInformation.setText("{md-info-outline} "
                        + this.getString(R.string.filtered_label));
                Iconify.addIcons(viewBinding.header.txtvInformation);
                viewBinding.header.txtvInformation.setOnClickListener((l) -> {
                    FilterDialog filterDialog = new FilterDialog(requireContext(), feed.getItemFilter()) {
                        @Override
                        protected void updateFilter(Set<String> filterValues) {
                            feed.setItemFilter(filterValues.toArray(new String[0]));
                            DBWriter.setFeedItemsFilter(feed.getId(), filterValues);
                        }
                    };

                    filterDialog.openDialog();
                });
                viewBinding.header.txtvInformation.setVisibility(View.VISIBLE);
            } else {
                viewBinding.header.txtvInformation.setVisibility(View.GONE);
            }
        } else {
            viewBinding.header.txtvInformation.setVisibility(View.GONE);
        }
    }

    private void setupHeaderView() {
        if (feed == null || headerCreated) {
            return;
        }

        // https://github.com/bumptech/glide/issues/529
        viewBinding.imgvBackground.setColorFilter(new LightingColorFilter(0xff666666, 0x000000));
        viewBinding.header.butShowInfo.setOnClickListener(v -> showFeedInfo());
        viewBinding.header.imgvCover.setOnClickListener(v -> showFeedInfo());
        viewBinding.header.butShowSettings.setOnClickListener(v -> {
            if (feed != null) {
                FeedSettingsFragment fragment = FeedSettingsFragment.newInstance(feed);
                ((MainActivity) getActivity()).loadChildFragment(fragment, TransitionEffect.SLIDE);
            }
        });
        viewBinding.header.butFilter.setOnClickListener(
                v -> new FilterDialog(getContext(), feed.getItemFilter()) {
                    @Override
                    protected void updateFilter(Set<String> filterValues) {
                        feed.setItemFilter(filterValues.toArray(new String[0]));
                        DBWriter.setFeedItemsFilter(feed.getId(), filterValues);
                    }
                }.openDialog());
        viewBinding.header.txtvFailure.setOnClickListener(v -> showErrorDetails());
        headerCreated = true;
    }

    private void showErrorDetails() {
        Maybe.fromCallable(
                () -> {
                    List<DownloadStatus> feedDownloadLog = DBReader.getFeedDownloadLog(feedID);
                    if (feedDownloadLog.size() == 0 || feedDownloadLog.get(0).isSuccessful()) {
                        return null;
                    }
                    return feedDownloadLog.get(0);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    downloadStatus -> new DownloadLogDetailsDialog(getContext(), downloadStatus).show(),
                    error -> error.printStackTrace(),
                    () -> {
                        Intent intent = new Intent(getContext(), MainActivity.class);
                        intent.putExtra(MainActivity.EXTRA_FRAGMENT_TAG, DownloadsFragment.TAG);
                        Bundle args = new Bundle();
                        args.putInt(DownloadsFragment.ARG_SELECTED_TAB, DownloadsFragment.POS_LOG);
                        intent.putExtra(MainActivity.EXTRA_FRAGMENT_ARGS, args);
                        startActivity(intent);
                    });
    }

    private void showFeedInfo() {
        if (feed != null) {
            FeedInfoFragment fragment = FeedInfoFragment.newInstance(feed);
            ((MainActivity) getActivity()).loadChildFragment(fragment, TransitionEffect.SLIDE);
        }
    }

    private void loadFeedImage() {
        Glide.with(getActivity())
                .load(feed.getImageUrl())
                .apply(new RequestOptions()
                    .placeholder(R.color.image_readability_tint)
                    .error(R.color.image_readability_tint)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .transform(new FastBlurTransformation())
                    .dontAnimate())
                .into(viewBinding.imgvBackground);

        Glide.with(getActivity())
                .load(feed.getImageUrl())
                .apply(new RequestOptions()
                    .placeholder(R.color.light_gray)
                    .error(R.color.light_gray)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .fitCenter()
                    .dontAnimate())
                .into(viewBinding.header.imgvCover);
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(this::loadData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        feed = result;
                        refreshHeaderView();
                        displayList();
                    }, error -> {
                        feed = null;
                        refreshHeaderView();
                        displayList();
                        Log.e(TAG, Log.getStackTraceString(error));
                    });
    }

    @Nullable
    private Feed loadData() {
        Feed feed = DBReader.getFeed(feedID, true);
        if (feed == null) {
            return null;
        }
        DBReader.loadAdditionalFeedItemListData(feed.getItems());
        if (feed.getSortOrder() != null) {
            List<FeedItem> feedItems = feed.getItems();
            FeedItemPermutors.getPermutor(feed.getSortOrder()).reorder(feedItems);
            feed.setItems(feedItems);
        }
        return feed;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onKeyUp(KeyEvent event) {
        if (!isAdded() || !isVisible() || !isMenuVisible()) {
            return;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_T:
                viewBinding.recyclerView.smoothScrollToPosition(0);
                break;
            case KeyEvent.KEYCODE_B:
                viewBinding.recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                break;
            default:
                break;
        }
    }

    private class FeedItemListAdapter extends EpisodeItemListAdapter {
        public FeedItemListAdapter(MainActivity mainActivity) {
            super(mainActivity);
        }

        @Override
        protected void beforeBindViewHolder(EpisodeItemViewHolder holder, int pos) {
            holder.coverHolder.setVisibility(View.GONE);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            if (!inActionMode()) {
                menu.findItem(R.id.multi_select).setVisible(true);
            }
            MenuItemUtils.setOnClickListeners(menu, FeedItemlistFragment.this::onContextItemSelected);
        }
    }
}
