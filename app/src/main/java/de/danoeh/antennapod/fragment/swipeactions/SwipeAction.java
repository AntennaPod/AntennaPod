package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public interface SwipeAction {

    //indexes of swipeActions
    public static final String ADD_TO_QUEUE = "ADD_TO_QUEUE";
    public static final String MARK_UNPLAYED = "MARK_UNPLAYED";
    public static final String START_DOWNLOAD = "START_DOWNLOAD";
    public static final String MARK_FAV = "MARK_FAV";
    public static final String MARK_PLAYED = "MARK_PLAYED";
    public static final String REMOVE_FROM_QUEUE = "REMOVE_FROM_QUEUE";

    String id();

    String title(Context context);

    int actionIcon();

    int actionColor();

    void action(FeedItem item, Fragment fragment, FeedItemFilter filter);

    boolean willRemove(FeedItemFilter filter);
}
