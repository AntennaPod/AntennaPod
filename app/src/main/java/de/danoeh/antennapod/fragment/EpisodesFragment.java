package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.joanzapata.iconify.Iconify;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.adapter.actionbutton.DeleteActionButton;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.dialog.FilterDialog;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.view.EmptyViewHandler;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;

public class EpisodesFragment extends EpisodesListFragment {

    public static final String TAG = "PowerEpisodesFragment";
    private static final String PREF_NAME = "PrefPowerEpisodesFragment";
    private static final String PREF_POSITION = "lastquickfilter";

    public static final String PREF_FILTER = "filter";

    public EpisodesFragment() {
        super();
    }

    public EpisodesFragment(boolean hideToolbar) {
        super();
        this.hideToolbar = hideToolbar;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        feedItemFilter = new FeedItemFilter(getPrefFilter());
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        toolbar.setTitle(R.string.episodes_label);

        //setUpQuickFilter();

        setSwipeActions(TAG);

        return  rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        updateFloatingFilterButton(prefs.getString(PREF_POSITION, QUICKFILTER_ALL));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public String getPrefFilter() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_FILTER, "");
    }

    private void updateFloatingFilterButton(String id) {
        String newFilter;
        int menuitemId;
        switch (id) {
            default:
            case EpisodesListFragment.QUICKFILTER_ALL:
                newFilter = getPrefFilter();
                menuitemId = R.id.filter_all;
                break;
            case EpisodesListFragment.QUICKFILTER_UNPLAYED:
                newFilter = FeedItemFilter.UNPLAYED;
                menuitemId = R.id.filter_unplayed;
                break;
            case EpisodesListFragment.QUICKFILTER_DOWNLOADED:
                newFilter = FeedItemFilter.DOWNLOADED;
                menuitemId = R.id.filter_downloaded;
                break;
            case EpisodesListFragment.QUICKFILTER_FAV:
                newFilter = FeedItemFilter.IS_FAVORITE;
                menuitemId = R.id.filter_favorites;
                break;
        }

        //floatingQuickFilterButton.setImageDrawable(quickFilterBar.getMenu().findItem(menuitemId).getIcon());

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_POSITION, id).apply();

        updateFeedItemFilter(newFilter);
    }

    @Override
    protected String getPrefName() {
        return PREF_NAME;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (!super.onMenuItemClick(item)) {
            if (item.getItemId() == R.id.filter_items) {
                AutoUpdateManager.runImmediate(requireContext());
                updateFloatingFilterButton(QUICKFILTER_ALL);
                showFilterDialog();
            } else {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.filter_items).setVisible(true);
        menu.findItem(R.id.refresh_item).setVisible(false);
    }

    @Override
    protected void onFragmentLoaded(List<FeedItem> episodes) {
        super.onFragmentLoaded(episodes);

        //smoothly animate filter info
        TransitionSet auto = new TransitionSet();
        auto.addTransition(new ChangeBounds());
        auto.excludeChildren(EmptyViewHandler.class, true);
        auto.excludeChildren(R.id.swipeRefresh, true);
        TransitionManager.beginDelayedTransition(
                (ViewGroup) txtvInformation.getParent(),
                auto);

        if (feedItemFilter.getValues().length > 0) {
            txtvInformation.setText("{md-info-outline} " + this.getString(R.string.filtered_label));
            Iconify.addIcons(txtvInformation);
            txtvInformation.setVisibility(View.VISIBLE);
        } else {
            txtvInformation.setVisibility(View.GONE);
        }

        setEmptyView(TAG); //default
    }

    private void showFilterDialog() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        FeedItemFilter prefFilter = new FeedItemFilter(prefs.getString(PREF_FILTER, ""));
        FilterDialog filterDialog = new FilterDialog(getContext(), prefFilter) {
            @Override
            protected void updateFilter(Set<String> filterValues) {
                feedItemFilter = new FeedItemFilter(filterValues.toArray(new String[0]));
                prefs.edit().putString(PREF_FILTER, StringUtils.join(filterValues, ",")).apply();
                loadItems();
            }
        };

        filterDialog.openDialog();
    }

    public void updateFeedItemFilter(String strings) {
        feedItemFilter = new FeedItemFilter(strings);
        swipeActions.setFilter(feedItemFilter);
        loadItems();
    }

    @Override
    protected boolean shouldUpdatedItemRemainInList(FeedItem item) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        FeedItemFilter feedItemFilter = new FeedItemFilter(prefs.getString(PREF_FILTER, ""));

        if (feedItemFilter.isShowDownloaded() && (!item.hasMedia() || !item.isDownloaded())) {
            return false;
        }

        return true;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return load(0);
    }

    private List<FeedItem> load(int offset) {
        int limit = EPISODES_PER_PAGE;
        return DBReader.getRecentlyPublishedEpisodes(offset, limit, feedItemFilter);
    }

    @Override
    protected EpisodeItemListAdapter newAdapter(MainActivity mainActivity) {
        return new EpisodesListAdapter(mainActivity);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData() {
        return load((page - 1) * EPISODES_PER_PAGE);
    }

    private static class EpisodesListAdapter extends EpisodeItemListAdapter {

        public EpisodesListAdapter(MainActivity mainActivity) {
            super(mainActivity);
        }

        @Override
        public void afterBindViewHolder(EpisodeItemViewHolder holder, int pos) {
            FeedItem item = getItem(pos);
            if (item.isPlayed() && item.getMedia() != null && item.getMedia().isDownloaded()) {
                DeleteActionButton actionButton = new DeleteActionButton(getItem(pos));
                actionButton.configure(holder.secondaryActionButton, holder.secondaryActionIcon, getActivity());
            }
        }
    }
}
