package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mobeta.android.dslv.DragSortListView;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.AllEpisodesRecycleAdapter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.event.QueueEvent;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.gui.FeedItemUndoToken;
import de.danoeh.antennapod.core.util.gui.UndoBarController;
import de.greenrobot.event.EventBus;


/**
 * Like 'EpisodesFragment' except that it only shows new episodes and
 * supports swiping to mark as read.
 */

public class NewEpisodesFragment extends AllEpisodesFragment {

    public static final String TAG = "NewEpisodesFragment";

    private static final String PREF_NAME = "PrefNewEpisodesFragment";

    private UndoBarController undoBarController;

    public NewEpisodesFragment() {
        super(true, PREF_NAME);
    }

    public void onEvent(QueueEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        loadItems();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void resetViewState() {
        super.resetViewState();
        undoBarController = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateViewHelper(inflater, container, savedInstanceState,
                R.layout.episodes_fragment_with_undo);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                AllEpisodesRecycleAdapter.Holder holder = (AllEpisodesRecycleAdapter.Holder)viewHolder;
                Log.d(TAG, "remove(" + holder.getItemId() + ")");
                if (subscription != null) {
                    subscription.unsubscribe();
                }
                FeedItem item = holder.getFeedItem();
                // we're marking it as unplayed since the user didn't actually play it
                // but they don't want it considered 'NEW' anymore
                DBWriter.markItemPlayed(FeedItem.UNPLAYED, item.getId());
                undoBarController.showUndoBar(false,
                        getString(R.string.marked_as_read_label), new FeedItemUndoToken(item,
                                holder.getItemPosition()));
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(listView);

        undoBarController = new UndoBarController<FeedItemUndoToken>(root.findViewById(R.id.undobar), new UndoBarController.UndoListener<FeedItemUndoToken>() {

            private final Context context = getActivity();

            @Override
            public void onUndo(FeedItemUndoToken token) {
                if (token != null) {
                    long itemId = token.getFeedItemId();
                    DBWriter.markItemPlayed(FeedItem.NEW, itemId);
                }
            }
            @Override
            public void onHide(FeedItemUndoToken token) {
                if (token != null && context != null) {
                    long itemId = token.getFeedItemId();
                    FeedItem item = DBReader.getFeedItem(itemId);
                    FeedMedia media = item.getMedia();
                    if(media != null && media.hasAlmostEnded() && item.getFeed().getPreferences().getCurrentAutoDelete()) {
                        DBWriter.deleteFeedMediaOfItem(context, media.getId());
                    }
                }
            }
        });
        return root;
    }

    @Override
    protected Pair<List<FeedItem>,LongList> loadData() {
        List<FeedItem> items;
        items = DBReader.getNewItemsList();
        LongList queuedIds = DBReader.getQueueIDList();
        return Pair.create(items, queuedIds);
    }

}
