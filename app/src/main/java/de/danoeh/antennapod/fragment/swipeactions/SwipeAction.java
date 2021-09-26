package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public interface SwipeAction {

    String ADD_TO_QUEUE = "ADD_TO_QUEUE";
    String REMOVE_FROM_INBOX = "REMOVE_FROM_INBOX";
    String START_DOWNLOAD = "START_DOWNLOAD";
    String MARK_FAV = "MARK_FAV";
    String MARK_PLAYED = "MARK_PLAYED";
    String REMOVE_FROM_QUEUE = "REMOVE_FROM_QUEUE";

    String getId();

    String getTitle(Context context);

    @DrawableRes
    int getActionIcon();

    @AttrRes
    int getActionColor();

    void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter);

    boolean willRemove(FeedItemFilter filter);
}
