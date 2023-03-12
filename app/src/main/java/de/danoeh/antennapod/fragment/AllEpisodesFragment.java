package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.dialog.AllEpisodesFilterDialog;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Shows all episodes (possibly filtered by user).
 */
public class AllEpisodesFragment extends EpisodesListFragment {
    public static final String TAG = "EpisodesFragment";
    private static final String PREF_NAME = "PrefAllEpisodesFragment";
    private static final String PREF_FILTER = "filter";
    public static final String PREF_SORT = "prefEpisodesSort";
    private SharedPreferences prefs;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        toolbar.inflateMenu(R.menu.episodes);
        inflateSortMenu();
        toolbar.setTitle(R.string.episodes_label);
        updateToolbar();
        updateFilterUi();
        prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        txtvInformation.setOnClickListener(
                v -> AllEpisodesFilterDialog.newInstance(getFilter()).show(getChildFragmentManager(), null));
        return root;
    }

    private void inflateSortMenu() {
        MenuItem sortItem = toolbar.getMenu().findItem(R.id.episodes_sort);
        getActivity().getMenuInflater().inflate(R.menu.sort_menu, sortItem.getSubMenu());

        // Remove the sorting options that are not needed in this fragment
        toolbar.getMenu().findItem(R.id.sort_episode_title).setVisible(false);
        toolbar.getMenu().findItem(R.id.sort_feed_title).setVisible(false);
        toolbar.getMenu().findItem(R.id.sort_random).setVisible(false);
        toolbar.getMenu().findItem(R.id.sort_smart_shuffle).setVisible(false);
        toolbar.getMenu().findItem(R.id.keep_sorted).setVisible(false);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getEpisodes(0, page * EPISODES_PER_PAGE, getFilter(), getSortOrder());
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData(int page) {
        return DBReader.getEpisodes((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE, getFilter(), getSortOrder());
    }

    @Override
    protected int loadTotalItemCount() {
        return DBReader.getTotalEpisodeCount(getFilter());
    }

    @Override
    protected FeedItemFilter getFilter() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new FeedItemFilter(prefs.getString(PREF_FILTER, ""));
    }

    @Override
    protected String getFragmentTag() {
        return TAG;
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
            AllEpisodesFilterDialog.newInstance(getFilter()).show(getChildFragmentManager(), null);
            return true;
        } else if (item.getItemId() == R.id.action_favorites) {
            ArrayList<String> filter = new ArrayList<>(getFilter().getValuesList());
            if (filter.contains(FeedItemFilter.IS_FAVORITE)) {
                filter.remove(FeedItemFilter.IS_FAVORITE);
            } else {
                filter.add(FeedItemFilter.IS_FAVORITE);
            }
            onFilterChanged(new AllEpisodesFilterDialog.AllEpisodesFilterChangedEvent(new HashSet<>(filter)));
            return true;
        } else {
            SortOrder sortOrder = MenuItemToSortOrderConverter.convert(item);
            if (sortOrder != null) {
                saveSortOrderAndRefresh(sortOrder);
                return true;
            }
        }
        return false;
    }

    private void saveSortOrderAndRefresh(SortOrder type) {
        prefs.edit().putString(PREF_SORT, "" + type.code).apply();
        loadItems();
    }

    @Subscribe
    public void onFilterChanged(AllEpisodesFilterDialog.AllEpisodesFilterChangedEvent event) {
        prefs.edit().putString(PREF_FILTER, StringUtils.join(event.filterValues, ",")).apply();
        updateFilterUi();
        page = 1;
        loadItems();
    }

    private void updateFilterUi() {
        swipeActions.setFilter(getFilter());
        if (getFilter().getValues().length > 0) {
            txtvInformation.setVisibility(View.VISIBLE);
            emptyView.setMessage(R.string.no_all_episodes_filtered_label);
        } else {
            txtvInformation.setVisibility(View.GONE);
            emptyView.setMessage(R.string.no_all_episodes_label);
        }
        toolbar.getMenu().findItem(R.id.action_favorites).setIcon(
                getFilter().showIsFavorite ? R.drawable.ic_star : R.drawable.ic_star_border);
    }

    private SortOrder getSortOrder() {
        return SortOrder.fromCodeString(prefs.getString(PREF_SORT, "" + SortOrder.DATE_NEW_OLD.code));
    }
}
