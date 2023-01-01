package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.Date;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

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

        Date playbackCompletionDate = item.getMedia().getPlaybackCompletionDate();

        DBWriter.deleteFromPlaybackHistory(item);

        ((MainActivity) fragment.requireActivity())
                .showSnackbarAbovePlayer(R.string.removed_history_label, Snackbar.LENGTH_LONG)
                .setAction(fragment.getString(R.string.undo),
                        v -> DBWriter.addItemToPlaybackHistory(item.getMedia(), playbackCompletionDate));
    }

    @Override
    public boolean willRemove(FeedItemFilter filter, FeedItem item) {
        return true;
    }
}