package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class ShowFirstSwipeDialogAction implements SwipeAction {

    @Override
    public String id() {
        return "SHOW_FIRST_SWIPE_DIALOG";
    }

    @Override
    public int actionIcon() {
        return R.drawable.ic_settings;
    }

    @Override
    public int actionColor() {
        return R.color.grey;
    }

    @Override
    public String title(Context context) {
        return "";
    }

    @Override
    public void action(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        //handled in SwipeActions
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return false;
    }
}
