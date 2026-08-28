package de.danoeh.antennapod.ui.swipeactions;

import android.content.Context;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.ui.share.ShareDialog;

public class ShareSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return SHARE;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_share;
    }

    @Override
    public int getActionColor() {
        return R.attr.icon_gray;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.share_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        ShareDialog shareDialog = ShareDialog.newInstance(item);
        shareDialog.show(fragment.getActivity().getSupportFragmentManager(), "ShareEpisodeDialog");
    }

    @Override
    public boolean willRemove(FeedItemFilter filter, FeedItem item) {
        return false;
    }
}
