package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mobeta.android.dslv.DragSortListView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.QueueEvent;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
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
        startItemLoader();
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
                R.layout.new_episodes_fragment, R.string.new_episodes_label);

        listView.setRemoveListener(new DragSortListView.RemoveListener() {
            @Override
            public void remove(int which) {
                Log.d(TAG, "remove(" + which + ")");
                stopItemLoader();
                FeedItem item = (FeedItem) listView.getAdapter().getItem(which);
                DBWriter.markItemRead(getActivity(), true, item.getId());
                undoBarController.showUndoBar(false,
                        getString(R.string.marked_as_read_label), new FeedItemUndoToken(item,
                                which)
                );
            }
        });

        undoBarController = new UndoBarController<FeedItemUndoToken>(root.findViewById(R.id.undobar), new UndoBarController.UndoListener<FeedItemUndoToken>() {

            private final Context context = getActivity();

            @Override
            public void onUndo(FeedItemUndoToken token) {
                if (token != null) {
                    long itemId = token.getFeedItemId();
                    DBWriter.markItemRead(context, false, itemId);
                }
            }
            @Override
            public void onHide(FeedItemUndoToken token) {
                if (token != null && context != null) {
                    long itemId = token.getFeedItemId();
                    FeedItem item = DBReader.getFeedItem(context, itemId);
                    FeedMedia media = item.getMedia();
                    if(media != null && media.hasAlmostEnded() && item.getFeed().getPreferences().getCurrentAutoDelete()) {
                        DBWriter.deleteFeedMediaOfItem(context, media.getId());
                    }
                }
            }
        });
        return root;
    }

}
