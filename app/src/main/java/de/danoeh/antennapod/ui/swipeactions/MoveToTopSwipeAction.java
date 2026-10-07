package de.danoeh.antennapod.ui.swipeactions;

import android.content.Context;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import java.util.Collections;
import org.greenrobot.eventbus.EventBus;

public class MoveToTopSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return MOVE_TO_TOP;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_arrow_full_up;
    }

    @Override
    public int getActionColor() {
        return R.attr.colorAccent;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.move_to_top_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        DBWriter.moveQueueItemsToTop(Collections.singletonList(item));
        EventBus.getDefault().post(new MessageEvent(
                fragment.getResources().getQuantityString(R.plurals.move_to_top_message, 1, 1)));
    }

    @Override
    public boolean willRemove(FeedItemFilter filter, FeedItem item) {
        return false;
    }
}
