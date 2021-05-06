package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.widget.IconTextView;
import com.leinardi.android.speeddial.SpeedDialView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
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
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.glide.FastBlurTransformation;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.FeedItemPermutors;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.core.util.gui.MoreContentListFooterUtil;
import de.danoeh.antennapod.dialog.EpisodesApplyActionFragment;
import de.danoeh.antennapod.dialog.FilterDialog;
import de.danoeh.antennapod.dialog.RemoveFeedDialog;
import de.danoeh.antennapod.dialog.RenameFeedDialog;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;
import de.danoeh.antennapod.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.view.EpisodeItemListRecyclerView;
import de.danoeh.antennapod.view.ToolbarIconTintManager;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.Validate;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Displays a list of FeedItems.
 */
public class FeedItemlistFragment extends Fragment implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener {
    private static final String TAG = "ItemlistFragment";
    private static final String ARGUMENT_FEED_ID = "argument.de.danoeh.antennapod.feed_id";
    private static final String KEY_UP_ARROW = "up_arrow";

    private FeedItemListAdapter adapter;
    private MoreContentListFooterUtil nextPageLoader;

    private ProgressBar progressBar;
    private EpisodeItemListRecyclerView recyclerView;
    private TextView txtvTitle;
    private IconTextView txtvFailure;
    private ImageView imgvBackground;
    private ImageView imgvCover;
    private TextView txtvInformation;
    private TextView txtvAuthor;
    private TextView txtvUpdatesDisabled;
    private ImageButton butShowInfo;
    private ImageButton butShowSettings;
    private View header;
    private Toolbar toolbar;
    private ToolbarIconTintManager iconTintManager;

    // fab speed dial

    /**
     * Specify an action (defined by #flag) 's UI bindings.
     *
     * Includes: the menu / action item and the actual logic
     */
    public static class ActionBinding {
        int flag;
        @IdRes
        final int actionItemId;
        @NonNull
        final Runnable action;

        ActionBinding(int flag, @IdRes int actionItemId, @NonNull Runnable action) {
            this.flag = flag;
            this.actionItemId = actionItemId;
            this.action = action;
        }
    }
//    private final List<? extends ActionBinding> actionBindings;

    private SpeedDialView mSpeedDialView;
    private int actions;
    private  LongList checkedIds = new LongList();

    private boolean displayUpArrow;

    private long feedID;
    private Feed feed;
    private boolean headerCreated = false;
    private boolean isUpdatingFeed;
    private Disposable disposable;


    public static final int ACTION_ADD_TO_QUEUE = 1;
    public static final int ACTION_REMOVE_FROM_QUEUE = 2;
    private static final int ACTION_MARK_PLAYED = 4;
    private static final int ACTION_MARK_UNPLAYED = 8;
    public static final int ACTION_DOWNLOAD = 16;
    public static final int ACTION_DELETE = 32;
    public static final int ACTION_ALL = ACTION_ADD_TO_QUEUE | ACTION_REMOVE_FROM_QUEUE
            | ACTION_MARK_PLAYED | ACTION_MARK_UNPLAYED | ACTION_DOWNLOAD | ACTION_DELETE;

    public FeedItemlistFragment() {
//        actionBindings = Arrays.asList(
//                new ActionBinding(ACTION_ADD_TO_QUEUE,
//                        R.id.add_to_queue_batch, this::queueChecked),
//                new ActionBinding(ACTION_REMOVE_FROM_QUEUE,
//                        R.id.remove_from_queue_batch, this::removeFromQueueChecked),
//                new ActionBinding(ACTION_MARK_PLAYED,
//                        R.id.mark_read_batch, this::markedCheckedPlayed),
//                new ActionBinding(ACTION_MARK_UNPLAYED,
//                        R.id.mark_unread_batch, this::markedCheckedUnplayed),
//                new ActionBinding(ACTION_DOWNLOAD,
//                        R.id.download_batch, this::downloadChecked),
//                new ActionBinding(ACTION_DELETE,
//                        R.id.delete_batch, this::deleteChecked)
//        );
    }

    private void action() {
    }

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
        i.actions = ACTION_ALL;
        b.putLong(ARGUMENT_FEED_ID, feedId);
        i.setArguments(b);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Bundle args = getArguments();
        Validate.notNull(args);
        feedID = args.getLong(ARGUMENT_FEED_ID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.feed_item_list_fragment, container, false);
        toolbar = root.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.feedlist);
        toolbar.setOnMenuItemClickListener(this);
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar, displayUpArrow);
        refreshToolbarState();

        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        recyclerView.setVisibility(View.GONE);

        progressBar = root.findViewById(R.id.progLoading);
        txtvTitle = root.findViewById(R.id.txtvTitle);
        txtvAuthor = root.findViewById(R.id.txtvAuthor);
        imgvBackground = root.findViewById(R.id.imgvBackground);
        imgvCover = root.findViewById(R.id.imgvCover);
        butShowInfo = root.findViewById(R.id.butShowInfo);
        butShowSettings = root.findViewById(R.id.butShowSettings);
        txtvInformation = root.findViewById(R.id.txtvInformation);
        txtvFailure = root.findViewById(R.id.txtvFailure);
        txtvUpdatesDisabled = root.findViewById(R.id.txtvUpdatesDisabled);
        header = root.findViewById(R.id.headerContainer);
        AppBarLayout appBar = root.findViewById(R.id.appBar);
        CollapsingToolbarLayout collapsingToolbar = root.findViewById(R.id.collapsing_toolbar);

        iconTintManager = new ToolbarIconTintManager(getContext(), toolbar, collapsingToolbar) {
            @Override
            protected void doTint(Context themedContext) {
                toolbar.getMenu().findItem(R.id.sort_items)
                        .setIcon(AppCompatDrawableManager.get().getDrawable(themedContext, R.drawable.ic_sort));
                toolbar.getMenu().findItem(R.id.filter_items)
                        .setIcon(AppCompatDrawableManager.get().getDrawable(themedContext, R.drawable.ic_filter));
                toolbar.getMenu().findItem(R.id.refresh_item)
                        .setIcon(AppCompatDrawableManager.get().getDrawable(themedContext, R.drawable.ic_refresh));
                toolbar.getMenu().findItem(R.id.action_search)
                        .setIcon(AppCompatDrawableManager.get().getDrawable(themedContext, R.drawable.ic_search));
            }
        };
        iconTintManager.updateTint();
        appBar.addOnOffsetChangedListener(iconTintManager);

        nextPageLoader = new MoreContentListFooterUtil(root.findViewById(R.id.more_content_list_footer));
        nextPageLoader.setClickListener(() -> {
            if (feed != null) {
                try {
                    DBTasks.loadNextPageOfFeed(getActivity(), feed, false);
                } catch (DownloadRequestException e) {
                    e.printStackTrace();
                    DownloadRequestErrorDialogCreator.newRequestErrorDialog(getActivity(), e.getMessage());
                }
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView view, int deltaX, int deltaY) {
                super.onScrolled(view, deltaX, deltaY);
                boolean hasMorePages = feed != null && feed.isPaged() && feed.getNextPageLink() != null;
                nextPageLoader.getRoot().setVisibility(
                        (recyclerView.isScrolledToBottom() && hasMorePages) ? View.VISIBLE : View.GONE);
            }
        });

        EventBus.getDefault().register(this);

        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            try {
                DBTasks.forceRefreshFeed(requireContext(), feed, true);
            } catch (DownloadRequestException e) {
                e.printStackTrace();
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false),
                    getResources().getInteger(R.integer.swipe_to_refresh_duration_in_ms));
        });

        loadItems();

        // Speed dial
        // Init action UI (via a FAB Speed Dial)
        mSpeedDialView = root.findViewById(R.id.fabSD);
        mSpeedDialView.inflate(R.menu.episodes_apply_action_speeddial);
        mSpeedDialView.setOnChangeListener(new SpeedDialView.OnChangeListener() {
            @Override
            public boolean onMainActionSelected() {
                return false;
            }

            @Override
            public void onToggleChanged(boolean open) {
                if (open && adapter.getSelectedCount() == 0) {
                    ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.no_items_selected,
                            Snackbar.LENGTH_SHORT);
                    mSpeedDialView.close();
                }
            }
        });
        mSpeedDialView.setOnActionSelectedListener(actionItem -> {
            checkedIds = new LongList();
            for (FeedItem episode : getCheckedItems()) {
                checkedIds.add(episode.getId());
            }

            switch (actionItem.getId()) {
                case R.id.add_to_queue_batch:
                    queueChecked();
                    break;
                case R.id.remove_from_queue_batch:
                    removeFromQueueChecked();
                    break;
                case R.id.mark_read_batch:
                    markedCheckedPlayed();
                    break;
                case R.id.mark_unread_batch:
                    markedCheckedUnplayed();
                    break;
                case R.id.download_batch:
                    downloadChecked();
                    break;
                case R.id.delete_batch:
                    deleteChecked();
                    break;
                default:
                    Log.e(TAG, "Unrecognized speed dial action item. Do nothing. id=" + actionItem.getId());
            }

            mSpeedDialView.close();
            mSpeedDialView.setVisibility(View.GONE);
            adapter.finish();
            return true;
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
        adapter = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker = new MenuItemUtils.UpdateRefreshMenuItemChecker() {
        @Override
        public boolean isRefreshing() {
            return feed != null && DownloadService.isRunning && DownloadRequester.getInstance().isDownloadingFile(feed);
        }
    };

    private void refreshToolbarState() {
        if (feed == null) {
            return;
        }
        MenuItemUtils.setupSearchItem(toolbar.getMenu(), (MainActivity) getActivity(), feedID, feed.getTitle());

        toolbar.getMenu().findItem(R.id.share_link_item).setVisible(feed.getLink() != null);
        toolbar.getMenu().findItem(R.id.visit_website_item).setVisible(feed.getLink() != null);

        isUpdatingFeed = MenuItemUtils.updateRefreshMenuItem(toolbar.getMenu(),
                R.id.refresh_item, updateRefreshMenuItemChecker);
        FeedMenuHandler.onPrepareOptionsMenu(toolbar.getMenu(), feed);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int horizontalSpacing = (int) getResources().getDimension(R.dimen.additional_horizontal_spacing);
        header.setPadding(horizontalSpacing, header.getPaddingTop(), horizontalSpacing, header.getPaddingBottom());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            item.getActionView().post(() -> iconTintManager.updateTint());
        }
        if (feed == null) {
            ((MainActivity) getActivity()).showSnackbarAbovePlayer(
                    R.string.please_wait_for_data, Toast.LENGTH_LONG);
            return true;
        }
        boolean feedMenuHandled;
        try {
            feedMenuHandled = FeedMenuHandler.onOptionsItemClicked(getActivity(), item, feed);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DownloadRequestErrorDialogCreator.newRequestErrorDialog(getActivity(), e.getMessage());
            return true;
        }
        if (feedMenuHandled) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.episode_actions:
                int actions = EpisodesApplyActionFragment.ACTION_ALL;
                if (feed.isLocalFeed()) {
                    // turn off download and delete actions for local feed
                    actions ^= EpisodesApplyActionFragment.ACTION_DOWNLOAD;
                    actions ^= EpisodesApplyActionFragment.ACTION_DELETE;
                }
                EpisodesApplyActionFragment fragment = EpisodesApplyActionFragment
                        .newInstance(feed.getItems(), actions);
                ((MainActivity) getActivity()).loadChildFragment(fragment);
                return true;
            case R.id.rename_item:
                new RenameFeedDialog(getActivity(), feed).show();
                return true;
            case R.id.remove_item:
                RemoveFeedDialog.show(getContext(), feed, () ->
                        ((MainActivity) getActivity()).loadFragment(EpisodesFragment.TAG, null));
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        FeedItem selectedItem = adapter.getSelectedItem();
        if (selectedItem == null) {
            Log.i(TAG, "Selected item at current position was null, ignoring selection");
            return super.onContextItemSelected(item);
        }

        // if multi select
        if (item.getItemId() == R.id.episode_actions) {
            adapter.startActionMode(adapter.getSelectedPosition());
            if (feed.isLocalFeed()) {
                mSpeedDialView.removeActionItemById(R.id.download_batch);
                mSpeedDialView.removeActionItemById(R.id.delete_batch);
            }
            mSpeedDialView.setVisibility(View.VISIBLE);
            refreshToolbarState();
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
                EpisodeItemViewHolder holder = (EpisodeItemViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null && holder.isCurrentlyPlayingItem()) {
                    holder.notifyPlaybackPositionUpdated(event);
                    break;
                }
            }
        }
    }


    private List<FeedItem> getCheckedItems() {
        return adapter.getSelectedItems();
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
        if (!DownloadRequester.getInstance().isDownloadingFeeds()) {
            nextPageLoader.getRoot().setVisibility(View.GONE);
        }
        nextPageLoader.setLoadingState(DownloadRequester.getInstance().isDownloadingFeeds());
    }

    private void displayList() {
        if (getView() == null) {
            Log.e(TAG, "Required root view is not yet created. Stop binding data to UI.");
            return;
        }
        if (adapter == null) {
            recyclerView.setAdapter(null);
            adapter = new FeedItemListAdapter((MainActivity) getActivity());
            recyclerView.setAdapter(adapter);
        }
        recyclerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        if (feed != null) {
            adapter.updateItems(feed.getItems());
        }

        refreshToolbarState();
        updateSyncProgressBarVisibility();
    }

    private void refreshHeaderView() {
        setupHeaderView();
        if (recyclerView == null || feed == null) {
            Log.e(TAG, "Unable to refresh header view");
            return;
        }
        loadFeedImage();
        if (feed.hasLastUpdateFailed()) {
            txtvFailure.setVisibility(View.VISIBLE);
        } else {
            txtvFailure.setVisibility(View.GONE);
        }
        if (!feed.getPreferences().getKeepUpdated()) {
            txtvUpdatesDisabled.setText("{md-pause-circle-outline} " + this.getString(R.string.updates_disabled_label));
            Iconify.addIcons(txtvUpdatesDisabled);
            txtvUpdatesDisabled.setVisibility(View.VISIBLE);
        } else {
            txtvUpdatesDisabled.setVisibility(View.GONE);
        }
        txtvTitle.setText(feed.getTitle());
        txtvAuthor.setText(feed.getAuthor());
        if (feed.getItemFilter() != null) {
            FeedItemFilter filter = feed.getItemFilter();
            if (filter.getValues().length > 0) {
                txtvInformation.setText("{md-info-outline} " + this.getString(R.string.filtered_label));
                Iconify.addIcons(txtvInformation);
                txtvInformation.setOnClickListener((l) -> {
                    FilterDialog filterDialog = new FilterDialog(requireContext(), feed.getItemFilter()) {
                        @Override
                        protected void updateFilter(Set<String> filterValues) {
                            feed.setItemFilter(filterValues.toArray(new String[0]));
                            DBWriter.setFeedItemsFilter(feed.getId(), filterValues);
                        }
                    };

                    filterDialog.openDialog();
                });
                txtvInformation.setVisibility(View.VISIBLE);
            } else {
                txtvInformation.setVisibility(View.GONE);
            }
        } else {
            txtvInformation.setVisibility(View.GONE);
        }
    }

    private void setupHeaderView() {
        if (feed == null || headerCreated) {
            return;
        }

        // https://github.com/bumptech/glide/issues/529
        imgvBackground.setColorFilter(new LightingColorFilter(0xff666666, 0x000000));
        butShowInfo.setVisibility(View.VISIBLE);
        butShowInfo.setOnClickListener(v -> showFeedInfo());
        imgvCover.setOnClickListener(v -> showFeedInfo());
        butShowSettings.setVisibility(View.VISIBLE);
        butShowSettings.setOnClickListener(v -> {
            if (feed != null) {
                FeedSettingsFragment fragment = FeedSettingsFragment.newInstance(feed);
                ((MainActivity) getActivity()).loadChildFragment(fragment, TransitionEffect.SLIDE);
            }
        });
        txtvFailure.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_FRAGMENT_TAG, DownloadsFragment.TAG);
            Bundle args = new Bundle();
            args.putInt(DownloadsFragment.ARG_SELECTED_TAB, DownloadsFragment.POS_LOG);
            intent.putExtra(MainActivity.EXTRA_FRAGMENT_ARGS, args);
            startActivity(intent);
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
                .load(feed.getImageUrl())
                .apply(new RequestOptions()
                    .placeholder(R.color.image_readability_tint)
                    .error(R.color.image_readability_tint)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .transform(new FastBlurTransformation())
                    .dontAnimate())
                .into(imgvBackground);

        Glide.with(getActivity())
                .load(feed.getImageUrl())
                .apply(new RequestOptions()
                    .placeholder(R.color.light_gray)
                    .error(R.color.light_gray)
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .fitCenter()
                    .dontAnimate())
                .into(imgvCover);
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        progressBar.setVisibility(View.VISIBLE);
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

    private void queueChecked() {
        // Check if an episode actually contains any media files before adding it to queue
        LongList toQueue = new LongList(checkedIds.size());
        for (FeedItem episode : getCheckedItems()) {
            if (checkedIds.contains(episode.getId()) && episode.hasMedia()) {
                toQueue.add(episode.getId());
            }
        }
        DBWriter.addQueueItem(getActivity(), true, toQueue.toArray());
        close(R.plurals.added_to_queue_batch_label, toQueue.size());
    }

    private void removeFromQueueChecked() {
//        LongList removeQueue = new LongList(checkedIds.size());
//        for (FeedItem episode : getCheckedItems()) {
//            if (checkedIds.contains(episode.getId()) && episode.hasMedia()) {
//                removeQueue.add(episode.getId());
//            }
//        }
        DBWriter.removeQueueItem(getActivity(), true, checkedIds.toArray());
        close(R.plurals.removed_from_queue_batch_label, checkedIds.size());
    }

    private void markedCheckedPlayed() {
        DBWriter.markItemPlayed(FeedItem.PLAYED, checkedIds.toArray());
        close(R.plurals.marked_read_batch_label, checkedIds.size());
    }

    private void markedCheckedUnplayed() {
        DBWriter.markItemPlayed(FeedItem.UNPLAYED, checkedIds.toArray());
        close(R.plurals.marked_unread_batch_label, checkedIds.size());
    }

    private void downloadChecked() {
        // download the check episodes in the same order as they are currently displayed
        List<FeedItem> toDownload = new ArrayList<>(checkedIds.size());
        List<FeedItem> episodes = adapter.getSelectedItems();
        for (FeedItem episode : episodes) {
            if (checkedIds.contains(episode.getId()) && episode.hasMedia() && !episode.getFeed().isLocalFeed()) {
                toDownload.add(episode);
            }
        }
        try {
            DownloadRequester.getInstance().downloadMedia(getActivity(), true, toDownload.toArray(new FeedItem[0]));
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DownloadRequestErrorDialogCreator.newRequestErrorDialog(getActivity(), e.getMessage());
        }
        close(R.plurals.downloading_batch_label, toDownload.size());
    }

    private void deleteChecked() {
        int countHasMedia = 0;
        int countNoMedia = 0;
        List<FeedItem> episodes = adapter.getSelectedItems();
        for (FeedItem feedItem : episodes) {
            checkedIds.contains(feedItem.getId());
            if (feedItem.hasMedia() && feedItem.getMedia().isDownloaded()) {
                countHasMedia++;
                DBWriter.deleteFeedMediaOfItem(getActivity(), feedItem.getMedia().getId());
            } else {
                countNoMedia++;
            }
        }
        closeMore(R.plurals.deleted_multi_episode_batch_label, countNoMedia, countHasMedia);
    }

    private class FeedItemListAdapter extends EpisodeItemListAdapter {
        public FeedItemListAdapter(MainActivity mainActivity) {
            super(mainActivity);
        }

        @Override
        protected void beforeBindViewHolder(EpisodeItemViewHolder holder, int pos) {
            holder.coverHolder.setVisibility(View.GONE);
        }
    }

    private void close(@PluralsRes int msgId, int numItems) {
        ((MainActivity) getActivity()).showSnackbarAbovePlayer(
                getResources().getQuantityString(msgId, numItems, numItems), Snackbar.LENGTH_LONG);
//        getActivity().getSupportFragmentManager().popBackStack();
    }

    private void closeMore(@PluralsRes int msgId, int countNoMedia, int countHasMedia) {
        ((MainActivity) getActivity()).showSnackbarAbovePlayer(
                getResources().getQuantityString(msgId,
                        (countHasMedia + countNoMedia),
                        (countHasMedia + countNoMedia), countHasMedia),
                Snackbar.LENGTH_LONG);
    }
}