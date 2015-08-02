package de.danoeh.antennapod.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Toast;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.util.LongList;
import de.danoeh.antennapod.core.util.NetworkUtils;

/**
 * Default implementation of an ActionButtonCallback
 */
public class DefaultActionButtonCallback implements ActionButtonCallback {

    private static final String TAG = "DefaultActionButtonCallback";

    private final Context context;

    private static final int TEN_MINUTES_IN_MILLIS = 60 * 1000 * 10;

    // remember timestamp when user allowed downloading via mobile connection
    private static long allowMobileDownloadsTimestamp;
    private static long onlyAddToQueueTimeStamp;

    public DefaultActionButtonCallback(Context context) {
        Validate.notNull(context);
        this.context = context;
    }

    public static boolean userAllowedMobileDownloads() {
        return System.currentTimeMillis() - allowMobileDownloadsTimestamp < TEN_MINUTES_IN_MILLIS;
    }

    public static boolean userChoseAddToQueue() {
        return System.currentTimeMillis() - onlyAddToQueueTimeStamp < TEN_MINUTES_IN_MILLIS;
    }

    @Override
    public void onActionButtonPressed(final FeedItem item) {

        if (item.hasMedia()) {
            final FeedMedia media = item.getMedia();
            boolean isDownloading = DownloadRequester.getInstance().isDownloadingFile(media);
            if (!isDownloading && !media.isDownloaded()) {
                LongList queueIds = DBReader.getQueueIDList(context);
                if (NetworkUtils.isDownloadAllowed() || userAllowedMobileDownloads()) {
                    try {
                        DBTasks.downloadFeedItems(context, item);
                        Toast.makeText(context, R.string.status_downloading_label, Toast.LENGTH_SHORT).show();
                    } catch (DownloadRequestException e) {
                        e.printStackTrace();
                        DownloadRequestErrorDialogCreator.newRequestErrorDialog(context, e.getMessage());
                    }
                } else if(userChoseAddToQueue() && !queueIds.contains(item.getId())) {
                    DBWriter.addQueueItem(context, item.getId());
                    Toast.makeText(context, R.string.added_to_queue_label, Toast.LENGTH_SHORT).show();
                } else {
                    confirmMobileDownload(context, item);
                }
            } else if (isDownloading) {
                DownloadRequester.getInstance().cancelDownload(context, media);
                if(UserPreferences.isEnableAutodownload()) {
                    DBWriter.setFeedItemAutoDownload(context, media.getItem(), false);
                    Toast.makeText(context, R.string.download_canceled_autodownload_enabled_msg, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, R.string.download_canceled_msg, Toast.LENGTH_LONG).show();
                }
            } else { // media is downloaded
                if (item.hasMedia() && item.getMedia().isCurrentlyPlaying()) {
                    context.sendBroadcast(new Intent(PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE));
                }
                else if (item.hasMedia() && item.getMedia().isCurrentlyPaused()) {
                    context.sendBroadcast(new Intent(PlaybackService.ACTION_RESUME_PLAY_CURRENT_EPISODE));
                }
                else {
                    DBTasks.playMedia(context, media, false, true, false);
                }
            }
        } else {
            if (!item.isPlayed()) {
                DBWriter.markItemRead(context, item, true, true);
            }
        }
    }

    private void confirmMobileDownload(final Context context, final FeedItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle(R.string.confirm_mobile_download_dialog_title)
                .setMessage(context.getText(R.string.confirm_mobile_download_dialog_message))
                .setPositiveButton(context.getText(R.string.confirm_mobile_download_dialog_enable_temporarily),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                allowMobileDownloadsTimestamp = System.currentTimeMillis();
                                try {
                                    DBTasks.downloadFeedItems(context, item);
                                    Toast.makeText(context, R.string.status_downloading_label, Toast.LENGTH_SHORT).show();
                                } catch (DownloadRequestException e) {
                                    e.printStackTrace();
                                    DownloadRequestErrorDialogCreator.newRequestErrorDialog(context, e.getMessage());
                                }
                            }
                        });
        LongList queueIds = DBReader.getQueueIDList(context);
        if(!queueIds.contains(item.getId())) {
            builder.setNeutralButton(context.getText(R.string.confirm_mobile_download_dialog_only_add_to_queue),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onlyAddToQueueTimeStamp = System.currentTimeMillis();
                            DBWriter.addQueueItem(context, item.getId());
                            Toast.makeText(context, R.string.added_to_queue_label, Toast.LENGTH_SHORT).show();
                        }
                    })
            .setMessage(context.getText(R.string.confirm_mobile_download_dialog_message_not_in_queue));
        } else {
            builder.setMessage(context.getText(R.string.confirm_mobile_download_dialog_message));
        }
        builder.create()
                .show();
    }
}
