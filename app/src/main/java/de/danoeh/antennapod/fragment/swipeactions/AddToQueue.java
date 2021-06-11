package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class AddToQueue extends SwipeAction {

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
            FeedItemMenuHandler.addToQueue(fragment.requireContext(), item);
        }
    }

    @Override
    List<String> affectedFilters() {
        return Arrays.asList("queued", "unplayed");
    }
}
