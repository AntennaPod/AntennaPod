package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.danoeh.antennapod.view.EpisodeItemViewHolder;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.AllEpisodesRecycleAdapter;
import de.danoeh.antennapod.core.event.FavoritesEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;

/**
 * Like 'EpisodesFragment' except that it only shows favorite episodes and
 * supports swiping to remove from favorites.
 */
public class FavoriteEpisodesFragment extends EpisodesListFragment {

    private static final String TAG = "FavoriteEpisodesFrag";
    private static final String PREF_NAME = "PrefFavoriteEpisodesFragment";

    @Override
    protected String getPrefName() {
        return PREF_NAME;
    }

    @Subscribe
    public void onEvent(FavoritesEvent event) {
        Log.d(TAG, String.format("onEvent() called with: event = [%s]", event));
        loadItems();
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        emptyView.setIcon(R.attr.ic_unfav);
        emptyView.setTitle(R.string.no_fav_episodes_head_label);
        emptyView.setMessage(R.string.no_fav_episodes_label);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                EpisodeItemViewHolder holder = (EpisodeItemViewHolder) viewHolder;
                Log.d(TAG, String.format("remove(%s)", holder.getFeedItem().getId()));

                if (disposable != null) {
                    disposable.dispose();
                }
                FeedItem item = holder.getFeedItem();
                if (item != null) {
                    DBWriter.removeFavoriteItem(item);

                    Snackbar snackbar = Snackbar.make(root, getString(R.string.removed_item), Snackbar.LENGTH_LONG);
                    snackbar.setAction(getString(R.string.undo), v -> DBWriter.addFavoriteItem(item));
                    snackbar.show();
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        return root;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getFavoriteItemsList(0, page * EPISODES_PER_PAGE);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData() {
        return DBReader.getFavoriteItemsList((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE);
    }
}
