package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.widget.Toast;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;

/**
 * Default implementation of an ActionButtonCallback
 */
public class DefaultActionButtonCallback implements ActionButtonCallback {
    private static final String TAG = "DefaultActionButtonCallback";

    private final Context context;

    public DefaultActionButtonCallback(Context context) {
        Validate.notNull(context);
        this.context = context;
    }

    @Override
    public void onActionButtonPressed(final FeedItem item) {


        if (item.hasMedia()) {
            final FeedMedia media = item.getMedia();
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
        } else {
            if (!item.isRead()) {
                DBWriter.markItemRead(context, item, true, true);
            }
        }
    }
}
