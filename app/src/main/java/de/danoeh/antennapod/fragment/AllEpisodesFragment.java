package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import com.joanzapata.iconify.Iconify;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.dialog.AllEpisodesFilterDialog;
import de.danoeh.antennapod.fragment.swipeactions.SwipeActions;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;

/**
 * Shows all episodes (possibly filtered by user).
 */
public class AllEpisodesFragment extends EpisodesListFragment implements Toolbar.OnMenuItemClickListener {
    public static final String TAG = "EpisodesFragment";
    private static final String PREF_NAME = "PrefAllEpisodesFragment";
    private static final String PREF_FILTER = "filter";
    private static final String KEY_UP_ARROW = "up_arrow";
    private Toolbar toolbar;
    private boolean displayUpArrow;
    private volatile boolean isUpdatingFeeds;
    private SwipeActions swipeActions;

    private FeedItemFilter feedItemFilter = new FeedItemFilter("");

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        feedItemFilter = new FeedItemFilter(prefs.getString(PREF_FILTER, ""));
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View appEpisodesContainer = View.inflate(getContext(), R.layout.list_container_fragment, null);
        View root = super.onCreateView(inflater, container, savedInstanceState);
        ((FrameLayout) appEpisodesContainer.findViewById(R.id.listContent)).addView(root);

        toolbar = appEpisodesContainer.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.inflateMenu(R.menu.episodes);
        toolbar.setTitle(R.string.episodes_label);
        toolbar.setOnLongClickListener(v -> {
            recyclerView.scrollToPosition(5);
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(0));
            return false;
        });
        updateToolbar();
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar, displayUpArrow);

        swipeActions = new SwipeActions(this, TAG).attachTo(recyclerView);
        swipeActions.setFilter(feedItemFilter);

        speedDialView.removeActionItemById(R.id.mark_unread_batch);
        speedDialView.removeActionItemById(R.id.remove_from_queue_batch);
        speedDialView.removeActionItemById(R.id.delete_batch);
        return appEpisodesContainer;
    }

    @Override
    protected String getPrefName() {
        return PREF_NAME;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == R.id.filter_items) {
            AllEpisodesFilterDialog.newInstance(feedItemFilter).show(getChildFragmentManager(), null);
            return true;
        } else if (item.getItemId() == R.id.action_favorites) {
            onFilterChanged(new AllEpisodesFilterDialog.AllEpisodesFilterChangedEvent(feedItemFilter.showIsFavorite
                            ? Collections.emptySet() : Collections.singleton(FeedItemFilter.IS_FAVORITE)));
            return true;
        }
        return false;
    }

    @Subscribe
    public void onFilterChanged(AllEpisodesFilterDialog.AllEpisodesFilterChangedEvent event) {
        feedItemFilter = new FeedItemFilter(event.filterValues.toArray(new String[0]));
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_FILTER, StringUtils.join(event.filterValues, ",")).apply();
        page = 1;
        swipeActions.setFilter(feedItemFilter);
        loadItems();
    }

    @Override
    protected void onFragmentLoaded(List<FeedItem> episodes) {
        super.onFragmentLoaded(episodes);

        if (feedItemFilter.getValues().length > 0) {
            txtvInformation.setText("{md-info-outline} " + this.getString(R.string.filtered_label));
            Iconify.addIcons(txtvInformation);
            txtvInformation.setVisibility(View.VISIBLE);
        } else {
            txtvInformation.setVisibility(View.GONE);
        }
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadService.isDownloadingFeeds();

    private void updateToolbar() {
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(toolbar.getMenu(),
                R.id.refresh_item, updateRefreshMenuItemChecker);
        toolbar.getMenu().findItem(R.id.filter_items).setVisible(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (isUpdatingFeeds != updateRefreshMenuItemChecker.isRefreshing()) {
            updateToolbar();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        super.onEventMainThread(event);
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            updateToolbar();
        }
    }

    @Override
    protected boolean shouldUpdatedItemRemainInList(FeedItem item) {
        return feedItemFilter.matches(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getRecentlyPublishedEpisodes(0, page * EPISODES_PER_PAGE, feedItemFilter);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData(int page) {
        return DBReader.getRecentlyPublishedEpisodes((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE, feedItemFilter);
    }

    @Override
    protected int loadTotalItemCount() {
        return DBReader.getTotalEpisodeCount(feedItemFilter);
    }
}
