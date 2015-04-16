package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mobeta.android.dslv.DragSortListView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.gui.FeedItemUndoToken;
import de.danoeh.antennapod.core.util.gui.UndoBarController;

/**
 * Like 'EpisodesFragment' except that it only shows new episodes and
 * supports swiping to mark as read.
 */
public class NewEpisodesFragment extends AllEpisodesFragment {

    private static final String TAG = "NewEpisodesFragment";
    private static final String PREF_NAME = "PrefNewEpisodesFragment";

    private UndoBarController undoBarController;

    public NewEpisodesFragment() {
        super(true, PREF_NAME);
    }

    @Override
    protected void resetViewState() {
        super.resetViewState();
        undoBarController = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateViewHelper(inflater, container, savedInstanceState,
                R.layout.new_episodes_fragment, R.string.new_episodes_label);

        final DragSortListView listView = (DragSortListView) root.findViewById(android.R.id.list);

        listView.setRemoveListener(new DragSortListView.RemoveListener() {
            @Override
            public void remove(int which) {
                Log.d(TAG, "remove(" + which + ")");
                stopItemLoader();
                FeedItem item = (FeedItem) listView.getAdapter().getItem(which);
                DBWriter.markItemRead(getActivity(), item.getId(), true);
                undoBarController.showUndoBar(false,
                        getString(R.string.marked_as_read_label), new FeedItemUndoToken(item,
                                which)
                );
            }
        });

        undoBarController = new UndoBarController(root.findViewById(R.id.undobar), new UndoBarController.UndoListener() {
            @Override
            public void onUndo(Parcelable token) {
                // Perform the undo
                FeedItemUndoToken undoToken = (FeedItemUndoToken) token;
                if (token != null) {
                    long itemId = undoToken.getFeedItemId();
                    int position = undoToken.getPosition();
                    DBWriter.markItemRead(getActivity(), itemId, false);
                }
            }
        });
        return root;
    }
}
