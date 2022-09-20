package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class TogglePlaybackStateSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return TOGGLE_PLAYED;
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
        return context.getString(R.string.toggle_played_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        int newState = item.getPlayState() == FeedItem.UNPLAYED ? FeedItem.PLAYED : FeedItem.UNPLAYED;
        FeedItemMenuHandler.markReadWithUndo(fragment, item, newState, willRemove(filter, item));
    }

    @Override
    public boolean willRemove(FeedItemFilter filter, FeedItem item) {
        if (item.getPlayState() == FeedItem.NEW) {
            return filter.showPlayed || filter.showNew;
        } else {
            return filter.showUnplayed || filter.showPlayed || filter.showNew;
        }
    }
}
