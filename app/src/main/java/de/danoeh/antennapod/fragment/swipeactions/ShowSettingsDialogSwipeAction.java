package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class ShowSettingsDialogSwipeAction implements SwipeAction {

    @Override
    public int actionIcon() {
        return R.drawable.ic_settings;
    }

    @Override
    public int actionColor() {
        return R.color.swipe_yellow_200;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.settings_label);
    }

    @Override
    public void action(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        // This special swipe action is used to display the swipe settings dialog
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return false;
    }
}
