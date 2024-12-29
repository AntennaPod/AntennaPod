package de.danoeh.antennapod.ui.screen;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemListAdapter;
import de.danoeh.antennapod.ui.screen.subscriptions.HorizontalFeedListAdapter;
import de.danoeh.antennapod.ui.MenuItemUtils;
import de.danoeh.antennapod.event.EpisodeDownloadEvent;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.ui.episodeslist.EpisodeMultiSelectActionHandler;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.episodeslist.FeedItemMenuHandler;
import de.danoeh.antennapod.net.discovery.CombinedSearcher;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import de.danoeh.antennapod.ui.discovery.OnlineSearchFragment;
import de.danoeh.antennapod.ui.view.EmptyViewHandler;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemListRecyclerView;
import de.danoeh.antennapod.ui.view.FloatingSelectMenu;
import de.danoeh.antennapod.ui.view.LiftOnScrollListener;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemViewHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;
import de.danoeh.antennapod.ui.screen.subscriptions.FeedMenuHandler;
import de.danoeh.antennapod.event.FeedListUpdateEvent;


/**
 * Performs a search operation on all feeds or one specific feed and displays the search result.
 */
public class SearchFragment extends Fragment implements EpisodeItemListAdapter.OnSelectModeListener {
    private static final String TAG = "SearchFragment";
    private static final String ARG_QUERY = "query";
    private static final String ARG_FEED = "feed";
    private static final String ARG_FEED_NAME = "feedName";
    private static final int SEARCH_DEBOUNCE_INTERVAL = 1500;

    private EpisodeItemListAdapter adapter;
    private HorizontalFeedListAdapter adapterFeeds;
    private Disposable disposableFeeds;
    private Disposable disposableEpisodes;
    private ProgressBar progressBar;
    private EmptyViewHandler emptyViewHandler;
    private EpisodeItemListRecyclerView recyclerView;
    private List<FeedItem> results;
    private Chip chip;
    private SearchView searchView;
    private FloatingSelectMenu floatingSelectMenu;
    private Handler automaticSearchDebouncer;
    private long lastQueryChange = 0;
    private boolean isOtherViewInFoucus = false;


    /**
     * Create a new SearchFragment that searches all feeds.
     */
    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_FEED, 0);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Create a new SearchFragment that searches all feeds with pre-defined query.
     */
    public static SearchFragment newInstance(String query) {
        SearchFragment fragment = newInstance();
        fragment.getArguments().putString(ARG_QUERY, query);
        return fragment;
    }

    /**
     * Create a new SearchFragment that searches one specific feed.
     */
    public static SearchFragment newInstance(long feed, String feedTitle) {
        SearchFragment fragment = newInstance();
        fragment.getArguments().putLong(ARG_FEED, feed);
        fragment.getArguments().putString(ARG_FEED_NAME, feedTitle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        automaticSearchDebouncer = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposableFeeds != null) {
            disposableFeeds.dispose();
        }
        if (disposableEpisodes != null) {
            disposableEpisodes.dispose();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.search_fragment, container, false);
        setupToolbar(layout.findViewById(R.id.toolbar));
        progressBar = layout.findViewById(R.id.progressBar);
        recyclerView = layout.findViewById(R.id.recyclerView);
        floatingSelectMenu = layout.findViewById(R.id.floatingSelectMenu);
        recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        registerForContextMenu(recyclerView);
        adapter = new EpisodeItemListAdapter(getActivity()) {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                super.onCreateContextMenu(menu, v, menuInfo);
                if (!inActionMode()) {
                    menu.findItem(R.id.multi_select).setVisible(true);
                }
                MenuItemUtils.setOnClickListeners(menu, SearchFragment.this::onContextItemSelected);
            }

            @Override
            protected void onSelectedItemsUpdated() {
                super.onSelectedItemsUpdated();
                FeedItemMenuHandler.onPrepareMenu(floatingSelectMenu.getMenu(), getSelectedItems(),
                        R.id.add_to_queue_item, R.id.remove_inbox_item);
                floatingSelectMenu.updateItemVisibility();
            }
        };
        adapter.setOnSelectModeListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new LiftOnScrollListener(layout.findViewById(R.id.appbar)));

        RecyclerView recyclerViewFeeds = layout.findViewById(R.id.recyclerViewFeeds);
        LinearLayoutManager layoutManagerFeeds = new LinearLayoutManager(getActivity());
        layoutManagerFeeds.setOrientation(RecyclerView.HORIZONTAL);
        recyclerViewFeeds.setLayoutManager(layoutManagerFeeds);
        adapterFeeds = new HorizontalFeedListAdapter((MainActivity) getActivity()) {
            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view,
                                            ContextMenu.ContextMenuInfo contextMenuInfo) {
                super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
                MenuItemUtils.setOnClickListeners(contextMenu, SearchFragment.this::onContextItemSelected);
            }
        };
        recyclerViewFeeds.setAdapter(adapterFeeds);

        emptyViewHandler = new EmptyViewHandler(getContext());
        emptyViewHandler.attachToRecyclerView(recyclerView);
        emptyViewHandler.setIcon(R.drawable.ic_search);
        emptyViewHandler.setTitle(R.string.type_to_search);
        EventBus.getDefault().register(this);

        chip = layout.findViewById(R.id.feed_title_chip);
        chip.setOnCloseIconClickListener(v -> {
            getArguments().putLong(ARG_FEED, 0);
            searchWithProgressBar();
        });
        chip.setVisibility((getArguments().getLong(ARG_FEED, 0) == 0) ? View.GONE : View.VISIBLE);
        chip.setText(getArguments().getString(ARG_FEED_NAME, ""));
        if (getArguments().getString(ARG_QUERY, null) != null) {
            search();
        }
        searchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            if (hasFocus && !isOtherViewInFoucus) {
                showInputMethod(view.findFocus());
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    InputMethodManager imm = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(recyclerView.getWindowToken(), 0);
                }
            }
        });
        floatingSelectMenu.inflate(R.menu.episodes_apply_action_speeddial);
        floatingSelectMenu.setOnMenuItemClickListener(menuItem -> {
            if (adapter.getSelectedCount() == 0) {
                ((MainActivity) getActivity())
                        .showSnackbarAbovePlayer(R.string.no_items_selected, Snackbar.LENGTH_SHORT);
                return false;
            }
            new EpisodeMultiSelectActionHandler((MainActivity) getActivity(), menuItem.getItemId())
                    .handleAction(adapter.getSelectedItems());
            adapter.endSelectMode();
            return true;
        });

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    private void setupToolbar(MaterialToolbar toolbar) {
        toolbar.setTitle(R.string.search_label);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        toolbar.inflateMenu(R.menu.search);

        MenuItem item = toolbar.getMenu().findItem(R.id.action_search);
        item.expandActionView();
        searchView = (SearchView) item.getActionView();
        searchView.setQueryHint(getString(R.string.search_label));
        searchView.setQuery(getArguments().getString(ARG_QUERY), true);
        searchView.requestFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchView.clearFocus();
                searchWithProgressBar();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                automaticSearchDebouncer.removeCallbacksAndMessages(null);
                if (s.isEmpty() || s.endsWith(" ") || (lastQueryChange != 0
                        && System.currentTimeMillis() > lastQueryChange + SEARCH_DEBOUNCE_INTERVAL)) {
                    search();
                } else {
                    automaticSearchDebouncer.postDelayed(() -> {
                        search();
                        lastQueryChange = 0; // Don't search instantly with first symbol after some pause
                    }, SEARCH_DEBOUNCE_INTERVAL / 2);
                }
                lastQueryChange = System.currentTimeMillis();
                return false;
            }
        });
        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                getParentFragmentManager().popBackStack();
                return true;
            }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        Feed selectedFeedItem  = adapterFeeds.getLongPressedItem();
        if (selectedFeedItem != null
                && FeedMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedFeedItem, () -> { })) {
            return true;
        }
        FeedItem selectedItem = adapter.getLongPressedItem();
        if (selectedItem != null) {
            if (adapter.onContextItemSelected(item)) {
                return true;
            }
            if (FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedItem)) {
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        search();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        search();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (results == null) {
            return;
        } else if (adapter == null) {
            search();
            return;
        }
        for (int i = 0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemEvent.indexOfItemWithId(results, item.getId());
            if (pos >= 0) {
                results.remove(pos);
                results.add(pos, item);
                adapter.notifyItemChangedCompat(pos);
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(EpisodeDownloadEvent event) {
        if (results == null) {
            return;
        }
        for (String downloadUrl : event.getUrls()) {
            int pos = EpisodeDownloadEvent.indexOfItemWithDownloadUrl(results, downloadUrl);
            if (pos >= 0) {
                adapter.notifyItemChangedCompat(pos);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        search();
    }

    private void searchWithProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        emptyViewHandler.hide();
        search();
    }

    private void search() {
        if (disposableFeeds != null) {
            disposableFeeds.dispose();
        }
        if (disposableEpisodes != null) {
            disposableEpisodes.dispose();
        }
        adapterFeeds.setEndButton(R.string.search_online, this::searchOnline);
        long feed = getArguments().getLong(ARG_FEED, 0);
        chip.setVisibility((feed == 0) ? View.GONE : View.VISIBLE);

        String query = searchView.getQuery().toString();
        if (query.isEmpty()) {
            emptyViewHandler.setTitle(R.string.type_to_search);
            return;
        }
        if (feed != 0) {
            // Search within a feed
            adapterFeeds.updateData(Collections.emptyList());
        } else {
            disposableFeeds = Observable.fromCallable(() -> DBReader.searchFeeds(query))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(results -> {
                        progressBar.setVisibility(View.GONE);
                        adapterFeeds.updateData(results);
                        emptyViewHandler.setTitle(getString(R.string.no_results_for_query, query));
                    }, error -> Log.e(TAG, Log.getStackTraceString(error)));
        }
        disposableEpisodes = Observable.fromCallable(() -> DBReader.searchFeedItems(feed, query))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(results -> {
                    progressBar.setVisibility(View.GONE);
                    this.results = results;
                    adapter.updateItems(results);
                    emptyViewHandler.setTitle(getString(R.string.no_results_for_query, searchView.getQuery()));
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    private void searchOnline() {
        searchView.clearFocus();
        InputMethodManager in = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        in.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        String query = searchView.getQuery().toString();
        if (query.matches("http[s]?://.*")) {
            startActivity(new OnlineFeedviewActivityStarter(getContext(), query).getIntent());
            return;
        }
        ((MainActivity) getActivity()).loadChildFragment(
                OnlineSearchFragment.newInstance(CombinedSearcher.class, query));
    }

    @Override
    public void onStartSelectMode() {
        searchViewFocusOff();
        floatingSelectMenu.setVisibility(View.VISIBLE);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                (int) getResources().getDimension(R.dimen.floating_select_menu_height));
    }

    @Override
    public void onEndSelectMode() {
        floatingSelectMenu.setVisibility(View.GONE);
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(), 0);
        searchViewFocusOn();
    }

    private void searchViewFocusOff() {
        isOtherViewInFoucus = true;
        searchView.clearFocus();
    }

    private void searchViewFocusOn() {
        isOtherViewInFoucus = false;
        searchView.requestFocus();
    }
}
