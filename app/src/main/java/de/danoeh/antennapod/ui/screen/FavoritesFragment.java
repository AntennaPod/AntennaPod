package de.danoeh.antennapod.ui.screen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.episodeslist.EpisodesListFragment;

import java.util.List;

public class FavoritesFragment extends EpisodesListFragment {
    public static final String TAG = "FavoritesFragment";
    private static final FeedItemFilter FILTER_FAVORITES = new FeedItemFilter(
            FeedItemFilter.IS_FAVORITE, FeedItemFilter.INCLUDE_NOT_SUBSCRIBED);
    private static Pair<Integer, Integer> scrollPosition = null;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        toolbar.setTitle(R.string.favorite_episodes_label);
        updateToolbar();
        emptyView.setIcon(R.drawable.ic_star);
        emptyView.setTitle(R.string.no_fav_episodes_head_label);
        emptyView.setMessage(R.string.no_fav_episodes_label);
        return root;
    }

    @Override
    protected FeedItemFilter getFilter() {
        return FILTER_FAVORITES;
    }

    @Override
    protected String getFragmentTag() {
        return TAG;
    }

    @Override
    public void onPause() {
        super.onPause();
        scrollPosition = recyclerView.getScrollPosition();
    }

    @Override
    protected void onItemsFirstLoaded() {
        recyclerView.restoreScrollPosition(scrollPosition);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getEpisodes(0, page * EPISODES_PER_PAGE, FILTER_FAVORITES,
                UserPreferences.getAllEpisodesSortOrder());
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData(int page) {
        return DBReader.getEpisodes((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE, FILTER_FAVORITES,
                UserPreferences.getAllEpisodesSortOrder());
    }

    @Override
    protected int loadTotalItemCount() {
        return DBReader.getTotalEpisodeCount(FILTER_FAVORITES);
    }
}
