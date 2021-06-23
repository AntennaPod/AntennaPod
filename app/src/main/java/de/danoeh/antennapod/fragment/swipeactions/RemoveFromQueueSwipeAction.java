package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class RemoveFromQueueSwipeAction implements SwipeAction {

    @Override
    public int actionIcon() {
        return R.drawable.ic_delete;
    }

    @Override
    public int actionColor() {
        return R.color.swipe_light_red_200;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.remove_from_queue_label);
    }

    @Override
    public void action(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        int position = DBReader.getQueueIDList().indexOf(item.getId());

        DBWriter.removeQueueItem(fragment.requireActivity(), true, item);

        if (willRemove(filter)) {
            ((MainActivity) fragment.requireActivity()).showSnackbarAbovePlayer(
                    fragment.getResources().getQuantityString(R.plurals.removed_from_queue_batch_label, 1, 1),
                    Snackbar.LENGTH_LONG)
                    .setAction(fragment.getString(R.string.undo), v ->
                            DBWriter.addQueueItemAt(fragment.requireActivity(), item.getId(), position, false));
        }
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return filter.showQueued;
    }
}
