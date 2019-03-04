package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.AllEpisodesRecycleAdapter;
import de.danoeh.antennapod.core.event.FeedItemEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;


/**
 * Like 'EpisodesFragment' except that it only shows new episodes and
 * supports swiping to mark as read.
 */

public class NewEpisodesFragment extends AllEpisodesFragment {

    public static final String TAG = "NewEpisodesFragment";

    private static final String PREF_NAME = "PrefNewEpisodesFragment";

    @Override
    protected boolean showOnlyNewEpisodes() { return true; }

    @Override
    protected String getPrefName() { return PREF_NAME; }

    @Override
    protected void resetViewState() {
        super.resetViewState();
    }

    @Override
    protected boolean shouldUpdatedItemRemainInList(FeedItem item) {
        return item.isNew();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateViewHelper(inflater, container, savedInstanceState,
                R.layout.all_episodes_fragment);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                AllEpisodesRecycleAdapter.Holder holder = (AllEpisodesRecycleAdapter.Holder)viewHolder;
                markItemAsSeenWithUndo(holder.getFeedItem());
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder,
                                          int actionState) {
                // We only want the active item
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    if (viewHolder instanceof AllEpisodesRecycleAdapter.ItemTouchHelperViewHolder) {
                        AllEpisodesRecycleAdapter.ItemTouchHelperViewHolder itemViewHolder =
                                (AllEpisodesRecycleAdapter.ItemTouchHelperViewHolder) viewHolder;
                        itemViewHolder.onItemSelected();
                    }
                }

                super.onSelectedChanged(viewHolder, actionState);
            }
            @Override
            public void clearView(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                if (viewHolder instanceof AllEpisodesRecycleAdapter.ItemTouchHelperViewHolder) {
                    AllEpisodesRecycleAdapter.ItemTouchHelperViewHolder itemViewHolder =
                            (AllEpisodesRecycleAdapter.ItemTouchHelperViewHolder) viewHolder;
                    itemViewHolder.onItemClear();
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        return root;
    }

    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getNewItemsList();
    }

}
