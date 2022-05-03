package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.joanzapata.iconify.Iconify;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.dialog.AllEpisodesFilterDialog;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

/**
 * Like 'EpisodesFragment' except that it only shows new episodes and
 * supports swiping to mark as read.
 */
public class AllEpisodesFragment extends EpisodesListFragment {
    private static final String PREF_NAME = "PrefAllEpisodesFragment";
    private static final String PREF_FILTER = "filter";

    private FeedItemFilter feedItemFilter = new FeedItemFilter("");

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        feedItemFilter = new FeedItemFilter(prefs.getString(PREF_FILTER, ""));
    }

    @Override
    protected String getPrefName() {
        return PREF_NAME;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == R.id.filter_items) {
            AllEpisodesFilterDialog.newInstance(feedItemFilter).show(getChildFragmentManager(), null);
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
        loadItems();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.filter_items).setVisible(true);
        menu.findItem(R.id.mark_all_read_item).setVisible(true);
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

    @Override
    protected boolean shouldUpdatedItemRemainInList(FeedItem item) {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        FeedItemFilter feedItemFilter = new FeedItemFilter(prefs.getString(PREF_FILTER, ""));

        if (feedItemFilter.isShowDownloaded() && (!item.hasMedia() || !item.getMedia().isDownloaded())) {
            return false;
        }

        return true;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getRecentlyPublishedEpisodes(0, page * EPISODES_PER_PAGE, feedItemFilter);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData() {
        return DBReader.getRecentlyPublishedEpisodes((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE, feedItemFilter);
    }
}
