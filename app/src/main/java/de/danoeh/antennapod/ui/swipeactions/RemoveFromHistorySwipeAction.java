package de.danoeh.antennapod.ui.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;
import java.util.Date;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import org.greenrobot.eventbus.EventBus;

public class RemoveFromHistorySwipeAction implements SwipeAction {

    public static final String TAG = "RemoveFromHistorySwipeAction";

    @Override
    public String getId() {
        return REMOVE_FROM_HISTORY;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_history_remove;
    }

    @Override
    public int getActionColor() {
        return R.attr.icon_purple;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.remove_history_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        Date lastPlayedTimeHistory = item.getMedia().getLastPlayedTimeHistory();
        DBWriter.deleteFromPlaybackHistory(item);
        EventBus.getDefault().post(new MessageEvent(fragment.getString(R.string.removed_history_label),
                context -> DBWriter.addItemToPlaybackHistory(item.getMedia(), lastPlayedTimeHistory),
                fragment.getString(R.string.undo)));
    }

    @Override
    public boolean willRemove(FeedItemFilter filter, FeedItem item) {
        return true;
    }
}