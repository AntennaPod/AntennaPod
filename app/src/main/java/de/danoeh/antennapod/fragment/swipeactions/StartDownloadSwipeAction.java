package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.actionbutton.DownloadActionButton;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class StartDownloadSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return START_DOWNLOAD;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_download;
    }

    @Override
    public int getActionColor() {
        return R.attr.icon_green;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.download_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        if (!item.isDownloaded() && !item.getFeed().isLocalFeed()) {
            new DownloadActionButton(item)
                    .onClick(fragment.requireContext());
        }
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return false;
    }
}
