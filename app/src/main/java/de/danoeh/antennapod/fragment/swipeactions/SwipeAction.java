package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import java.util.List;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public abstract class SwipeAction {

    abstract public String title(Context context);

    abstract public int actionIcon();
    abstract public int actionColor();

    abstract public void action(FeedItem item, Fragment fragment, FeedItemFilter filter);

    //see FeedItemFilter for valid properties
    abstract List<String> affectedFilters();

    public boolean willRemove(FeedItemFilter filter) {
        for (String s:
             affectedFilters()) {
            if (filter != null && filter.hasProperty(s)) {
                //filter matches affected, therefore item will be removed from list with this filter
                return true;
            }
        }
        return false;
    }

}
