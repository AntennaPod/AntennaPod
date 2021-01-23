package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UsageStatistics;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.NetworkUtils;

public class DownloadActionButton extends ItemActionButton {
    private boolean isInQueue;

    public DownloadActionButton(FeedItem item, boolean isInQueue) {
        super(item);
        this.isInQueue = isInQueue;
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.download_label;
    }

    @Override
    @AttrRes
    public int getDrawable() {
        return R.attr.av_download;
    }

    @Override
    public int getVisibility() {
        return item.getFeed().isLocalFeed() ? View.INVISIBLE : View.VISIBLE;
    }

    @Override
    public void onClick(Context context) {
        final FeedMedia media = item.getMedia();
        if (media == null || shouldNotDownload(media)) {
            return;
        }

        UsageStatistics.logAction(UsageStatistics.ACTION_DOWNLOAD);

        if (NetworkUtils.isEpisodeDownloadAllowed() || MobileDownloadHelper.userAllowedMobileDownloads()) {
            downloadEpisode(context);
        } else if (MobileDownloadHelper.userChoseAddToQueue() && !isInQueue) {
            addEpisodeToQueue(context);
        } else {
            MobileDownloadHelper.confirmMobileDownload(context, item);
        }
    }

    private boolean shouldNotDownload(@NonNull FeedMedia media) {
        boolean isDownloading = DownloadRequester.getInstance().isDownloadingFile(media);
        return isDownloading || media.isDownloaded();
    }

    private void addEpisodeToQueue(Context context) {
        DBWriter.addQueueItem(context, item);
        Toast.makeText(context, R.string.added_to_queue_label, Toast.LENGTH_SHORT).show();
    }

    private void downloadEpisode(Context context) {
        try {
            DownloadRequester.getInstance().downloadMedia(context, true, item);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DownloadRequestErrorDialogCreator.newRequestErrorDialog(context, e.getMessage());
        }
    }
}
