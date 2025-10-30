package de.danoeh.antennapod.ui.swipeactions;

import android.content.Context;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import org.greenrobot.eventbus.EventBus;

public class RemoveFromQueueSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return REMOVE_FROM_QUEUE;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_playlist_remove;
    }

    @Override
    public int getActionColor() {
        return R.attr.colorAccent;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.remove_from_queue_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        int position = DBReader.getQueueIDList().indexOf(item.getId());
        DBWriter.removeQueueItem(fragment.requireActivity(), true, item);
        if (willRemove(filter, item)) {
            EventBus.getDefault().post(new MessageEvent(
                    fragment.getResources().getQuantityString(R.plurals.removed_from_queue_message, 1, 1),
                    context -> DBWriter.addQueueItemAt(fragment.requireActivity(), item.getId(), position),
                    fragment.getString(R.string.undo)));
        }
    }

    @Override
    public boolean willRemove(FeedItemFilter filter, FeedItem item) {
        return filter.showQueued || filter.showNotQueued;
    }
}
