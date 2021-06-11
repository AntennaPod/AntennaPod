package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.actionbutton.DownloadActionButton;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class StartDownload extends SwipeAction {

    @Override
    public int actionIcon() {
        return R.drawable.ic_download;
    }

    @Override
    public int actionColor() {
        return R.color.swipe_light_blue_200;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.download_label);
    }

    @Override
    public void action(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        if (!item.isDownloaded()) {
            new DownloadActionButton(item, item.isTagged(FeedItem.TAG_QUEUE))
                    .onClick(fragment.requireContext());
        }
    }

    @Override
    List<String> affectedFilters() {
        return Collections.emptyList();
    }
}
