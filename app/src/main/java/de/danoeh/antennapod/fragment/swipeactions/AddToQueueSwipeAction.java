package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class AddToQueueSwipeAction implements SwipeAction {

    @Override
    public int actionIcon() {
        return R.drawable.ic_playlist;
    }

    @Override
    public int actionColor() {
        return R.color.swipe_light_green_200;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.add_to_queue_label);
    }

    @Override
    public void action(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        if (!item.isTagged(FeedItem.TAG_QUEUE)) {
            DBWriter.addQueueItem(fragment.requireContext(), item);
        }
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return filter.showQueued || filter.showUnplayed;
    }
}
