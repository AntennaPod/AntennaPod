package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.widget.Toast;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;

/**
 * Default implementation of an ActionButtonCallback
 */
public class DefaultActionButtonCallback implements ActionButtonCallback {
    private static final String TAG = "DefaultActionButtonCallback";

    private final Context context;

    public DefaultActionButtonCallback(Context context) {
        if (context == null) throw new IllegalArgumentException("context = null");
        this.context = context;
    }

    @Override
    public void onActionButtonPressed(final FeedItem item) {
        final FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }

        boolean isDownloading = DownloadRequester.getInstance().isDownloadingFile(media);
        if (!isDownloading && !media.isDownloaded()) {
            try {
                DBTasks.downloadFeedItems(context, item);
                Toast.makeText(context, R.string.status_downloading_label, Toast.LENGTH_SHORT).show();
            } catch (DownloadRequestException e) {
                e.printStackTrace();
                DownloadRequestErrorDialogCreator.newRequestErrorDialog(context, e.getMessage());
            }
        } else if (isDownloading) {
            DownloadRequester.getInstance().cancelDownload(context, media);
            Toast.makeText(context, R.string.download_cancelled_msg, Toast.LENGTH_SHORT).show();
        } else { // media is downloaded
            DBTasks.playMedia(context, media, true, true, false);
        }
    }
}
