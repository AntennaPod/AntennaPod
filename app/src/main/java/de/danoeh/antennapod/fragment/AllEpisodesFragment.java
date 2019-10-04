package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.joanzapata.iconify.Iconify;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedItemFilter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.dialog.FilterDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import java.util.List;
import java.util.Set;

/**
 * Like 'EpisodesFragment' except that it only shows new episodes and
 * supports swiping to mark as read.
 */
public class AllEpisodesFragment extends EpisodesListFragment {

    public static final String TAG = "AllEpisodesFragment";
    private static final String PREF_NAME = "PrefAllEpisodesFragment";

    private static final int EPISODES_PER_PAGE = 150;
    private static final int VISIBLE_EPISODES_SCROLL_THRESHOLD = 5;
    private static int page = 1;

    private static FeedItemFilter feedItemFilter = new FeedItemFilter("");

    @Override
    protected boolean showOnlyNewEpisodes() {
        return false;
    }

    @Override
    protected String getPrefName() {
        return PREF_NAME;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            switch (item.getItemId()) {
                case R.id.filter_items:
                    showFilterDialog();
                    return true;
                default:
                    return false;
            }
        } else {
            return true;
        }
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            /* Total number of episodes after last load */
            private int previousTotalEpisodes = 0;

            /* True if loading more episodes is still in progress */
            private boolean isLoadingMore = true;

            @Override
            public void onScrolled(RecyclerView recyclerView, int deltaX, int deltaY) {
                super.onScrolled(recyclerView, deltaX, deltaY);

                int visibleEpisodeCount = recyclerView.getChildCount();
                int totalEpisodeCount = recyclerView.getLayoutManager().getItemCount();
                int firstVisibleEpisode = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();

                /* Determine if loading more episodes has finished */
                if (isLoadingMore) {
                    if (totalEpisodeCount > previousTotalEpisodes) {
                        isLoadingMore = false;
                        previousTotalEpisodes = totalEpisodeCount;
                    }
                }

                /* Determine if the user scrolled to the bottom and loading more episodes is not already in progress */
                if (!isLoadingMore && (totalEpisodeCount - visibleEpisodeCount)
                        <= (firstVisibleEpisode + VISIBLE_EPISODES_SCROLL_THRESHOLD)) {

                    /* The end of the list has been reached. Load more data. */
                    page++;
                    loadMoreItems();
                    isLoadingMore = true;
                }
            }
        });

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.filter_items).setVisible(true);
        menu.findItem(R.id.mark_all_read_item).setVisible(!episodes.isEmpty());
    }

    @Override
    protected void onFragmentLoaded(List<FeedItem> episodes) {
        super.onFragmentLoaded(episodes);

        if (feedItemFilter.getValues().length > 0) {
            txtvInformation.setText("{fa-info-circle} " + this.getString(R.string.filtered_label));
            Iconify.addIcons(txtvInformation);
            txtvInformation.setVisibility(View.VISIBLE);
        } else {
            txtvInformation.setVisibility(View.GONE);
        }
    }

    private void loadMoreItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(this::loadMoreData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    progLoading.setVisibility(View.GONE);
                    episodes.addAll(data);
                    onFragmentLoaded(episodes);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void showFilterDialog() {
        FilterDialog filterDialog = new FilterDialog(getContext(), feedItemFilter) {
            @Override
            protected void updateFilter(Set<String> filterValues) {
                feedItemFilter = new FeedItemFilter(filterValues.toArray(new String[filterValues.size()]));
                loadItems();
            }
        };

        filterDialog.openDialog();
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return feedItemFilter.filter( DBReader.getRecentlyPublishedEpisodes(0, page * EPISODES_PER_PAGE));
    }

    List<FeedItem> loadMoreData() {
        return feedItemFilter.filter( DBReader.getRecentlyPublishedEpisodes((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE));
    }
}
