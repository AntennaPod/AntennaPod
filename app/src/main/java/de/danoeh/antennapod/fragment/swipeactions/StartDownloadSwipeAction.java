package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.actionbutton.DownloadActionButton;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class StartDownloadSwipeAction implements SwipeAction {

    @Override
    public int actionIcon() {
        return R.drawable.ic_download;
    }

    @Override
    public int actionColor() {
        return R.color.swipe_blue;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.download_label);
    }

    @Override
    public void action(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        if (!item.isDownloaded()) {
            new DownloadActionButton(item)
                    .onClick(fragment.requireContext());
        }
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return false;
    }
}
