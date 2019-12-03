package de.danoeh.antennapodSA.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.event.FavoritesEvent;
import de.danoeh.antennapodSA.core.feed.FeedItem;
import de.danoeh.antennapodSA.core.storage.DBReader;
import de.danoeh.antennapodSA.core.storage.DBWriter;
import de.danoeh.antennapodSA.adapter.AllEpisodesRecycleAdapter;

/**
 * Like 'EpisodesFragment' except that it only shows favorite episodes and
 * supports swiping to remove from favorites.
 */
public class FavoriteEpisodesFragment extends EpisodesListFragment {

    private static final String TAG = "FavoriteEpisodesFrag";
    private static final String PREF_NAME = "PrefFavoriteEpisodesFragment";

    @Override
    protected boolean showOnlyNewEpisodes() {
        return true;
    }

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
                AllEpisodesRecycleAdapter.Holder holder = (AllEpisodesRecycleAdapter.Holder) viewHolder;
                Log.d(TAG, String.format("remove(%s)", holder.getItemId()));

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
        return DBReader.getFavoriteItemsList();
    }
}
