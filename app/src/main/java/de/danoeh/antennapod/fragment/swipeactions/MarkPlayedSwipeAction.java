package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class MarkPlayedSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return MARK_PLAYED;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_mark_played;
    }

    @Override
    public int getActionColor() {
        return R.attr.icon_gray;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.mark_read_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        int togglePlayState =
                item.getPlayState() != FeedItem.PLAYED  ? FeedItem.PLAYED : FeedItem.UNPLAYED;
        FeedItemMenuHandler.markReadWithUndo(fragment,
                item, togglePlayState, willRemove(filter));
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return filter.showUnplayed || filter.showPlayed;
    }
}
