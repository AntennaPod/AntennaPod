package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequestException;

public class MobileDownloadHelper {
    private static long addToQueueTimestamp;
    private static long allowMobileDownloadTimestamp;
    private static final int TEN_MINUTES_IN_MILLIS = 10 * 60 * 1000;

    static boolean userChoseAddToQueue() {
        return System.currentTimeMillis() - addToQueueTimestamp < TEN_MINUTES_IN_MILLIS;
    }

    static boolean userAllowedMobileDownloads() {
        return System.currentTimeMillis() - allowMobileDownloadTimestamp < TEN_MINUTES_IN_MILLIS;
    }

    static void confirmMobileDownload(final Context context, final FeedItem item) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                .title(R.string.confirm_mobile_download_dialog_title)
                .content(R.string.confirm_mobile_download_dialog_message)
                .positiveText(context.getText(R.string.confirm_mobile_download_dialog_enable_temporarily))
                .onPositive((dialog, which) -> downloadFeedItems(context, item));
        if (!DBReader.getQueueIDList().contains(item.getId())) {
            builder
                    .content(R.string.confirm_mobile_download_dialog_message_not_in_queue)
                    .neutralText(R.string.confirm_mobile_download_dialog_only_add_to_queue)
                    .onNeutral((dialog, which) -> addToQueue(context, item));
        }
        builder.show();
    }

    public static void confirmMobileStreaming(final Context context, MaterialDialog.SingleButtonCallback onAllowed) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                .title(R.string.confirm_mobile_streaming_dialog_title)
                .content(R.string.confirm_mobile_streaming_dialog_message)
                .positiveText(R.string.yes)
                .onPositive(onAllowed)
                .negativeText(R.string.no);
        builder.show();
    }

    private static void addToQueue(Context context, FeedItem item) {
        addToQueueTimestamp = System.currentTimeMillis();
        DBWriter.addQueueItem(context, item);
        Toast.makeText(context, R.string.added_to_queue_label, Toast.LENGTH_SHORT).show();
    }

    private static void downloadFeedItems(Context context, FeedItem item) {
        allowMobileDownloadTimestamp = System.currentTimeMillis();
        try {
            DBTasks.downloadFeedItems(context, item);
            Toast.makeText(context, R.string.status_downloading_label, Toast.LENGTH_SHORT).show();
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DownloadRequestErrorDialogCreator.newRequestErrorDialog(context, e.getMessage());
        }
    }
}