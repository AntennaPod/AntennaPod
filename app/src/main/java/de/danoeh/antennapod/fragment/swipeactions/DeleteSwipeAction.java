package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;
import androidx.fragment.app.Fragment;

import java.util.Collections;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.view.LocalDeleteModal;

public class DeleteSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return DELETE;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_delete;
    }

    @Override
    public int getActionColor() {
        return R.attr.icon_red;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.delete_episode_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        if (!item.isDownloaded() && !item.getFeed().isLocalFeed()) {
            return;
        }
        LocalDeleteModal.showLocalFeedDeleteWarningIfNecessary(
                fragment.requireContext(), Collections.singletonList(item),
                () -> DBWriter.deleteFeedMediaOfItem(fragment.requireContext(), item.getMedia().getId()));
    }

    @Override
    public boolean willRemove(FeedItemFilter filter, FeedItem item) {
        return filter.showDownloaded && (item.isDownloaded() || item.getFeed().isLocalFeed());
    }
}
