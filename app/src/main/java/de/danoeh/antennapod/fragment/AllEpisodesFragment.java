package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.dialog.AllEpisodesFilterDialog;
import de.danoeh.antennapod.dialog.ItemSortDialog;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Shows all episodes (possibly filtered by user).
 */
public class AllEpisodesFragment extends EpisodesListFragment {
    public static final String TAG = "EpisodesFragment";
    public static final String PREF_NAME = "PrefAllEpisodesFragment";

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        toolbar.inflateMenu(R.menu.episodes);
        toolbar.setTitle(R.string.episodes_label);
        updateToolbar();
        updateFilterUi();
        txtvInformation.setOnClickListener(
                v -> AllEpisodesFilterDialog.newInstance(getFilter()).show(getChildFragmentManager(), null));
        return root;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getEpisodes(0, page * EPISODES_PER_PAGE, getFilter(),
                UserPreferences.getAllEpisodesSortOrder());
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData(int page) {
        return DBReader.getEpisodes((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE, getFilter(),
                UserPreferences.getAllEpisodesSortOrder());
    }

    @Override
    protected int loadTotalItemCount() {
        return DBReader.getTotalEpisodeCount(getFilter());
    }

    @Override
    protected FeedItemFilter getFilter() {
        return new FeedItemFilter(UserPreferences.getPrefFilterAllEpisodes());
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
        } else if (item.getItemId() == R.id.episodes_sort) {
            new AllEpisodesSortDialog().show(getChildFragmentManager().beginTransaction(), "SortDialog");
            return true;
        }
        return false;
    }

    @Subscribe
    public void onFilterChanged(AllEpisodesFilterDialog.AllEpisodesFilterChangedEvent event) {
        UserPreferences.setPrefFilterAllEpisodes(StringUtils.join(event.filterValues, ","));
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

    public static class AllEpisodesSortDialog extends ItemSortDialog {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            sortOrder = UserPreferences.getAllEpisodesSortOrder();
        }

        @Override
        protected void onAddItem(int title, SortOrder ascending, SortOrder descending, boolean ascendingIsDefault) {
            if (ascending == SortOrder.DATE_OLD_NEW || ascending == SortOrder.DURATION_SHORT_LONG) {
                super.onAddItem(title, ascending, descending, ascendingIsDefault);
            }
        }

        @Override
        protected void onSelectionChanged() {
            super.onSelectionChanged();
            UserPreferences.setAllEpisodesSortOrder(sortOrder);
            EventBus.getDefault().post(new FeedListUpdateEvent(0));
        }
    }
}
