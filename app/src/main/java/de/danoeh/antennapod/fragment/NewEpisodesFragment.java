package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;

/**
 * Like 'EpisodesFragment' except that it only shows new episodes and
 * supports swiping to mark as read.
 */
public class NewEpisodesFragment extends EpisodesListFragment {

    public static final String TAG = "NewEpisodesFragment";
    private static final String PREF_NAME = "PrefNewEpisodesFragment";

    @Override
    protected String getPrefName() {
        return PREF_NAME;
    }

    @Override
    protected boolean shouldUpdatedItemRemainInList(FeedItem item) {
        return item.isNew();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.filter_items).setVisible(false);
        menu.findItem(R.id.mark_all_read_item).setVisible(false);
        menu.findItem(R.id.remove_all_new_flags_item).setVisible(true);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        emptyView.setTitle(R.string.no_new_episodes_head_label);
        emptyView.setMessage(R.string.no_new_episodes_label);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                EpisodeItemViewHolder holder = (EpisodeItemViewHolder) viewHolder;
                FeedItemMenuHandler.removeNewFlagWithUndo(NewEpisodesFragment.this, holder.getFeedItem());
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        return root;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getNewItemsList(0, page * EPISODES_PER_PAGE);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData() {
        return DBReader.getNewItemsList((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE);
    }
}
