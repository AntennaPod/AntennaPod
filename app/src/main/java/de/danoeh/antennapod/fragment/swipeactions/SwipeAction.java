package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public abstract class SwipeAction {

    abstract public String title(Context context);

    abstract public int actionIcon();
    abstract public int actionColor();

    abstract public void action(FeedItem item, Fragment fragment, FeedItemFilter filter);

    //see FeedItemFilter for valid properties
    abstract public boolean willRemove(FeedItemFilter filter);

}
