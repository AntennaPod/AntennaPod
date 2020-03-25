package de.danoeh.antennapod.core.service.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DownloadServiceNotification {
    private static final String TAG = "DownloadSvcNotification";
    private static final int REPORT_ID = 3;
    private static final int AUTO_REPORT_ID = 4;

    private final Context context;
    private NotificationCompat.Builder notificationCompatBuilder;

    public DownloadServiceNotification(Context context) {
        this.context = context;
        setupNotificationBuilders();
    }

    private void setupNotificationBuilders() {
        notificationCompatBuilder = new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_DOWNLOADING)
                .setOngoing(true)
                .setContentIntent(ClientConfig.downloadServiceCallbacks.getNotificationContentIntent(context))
                .setSmallIcon(R.drawable.stat_notify_sync);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationCompatBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }

        Log.d(TAG, "Notification set up");
    }

    /**
     * Updates the contents of the service's notifications. Should be called
     * after setupNotificationBuilders.
     */
    public Notification updateNotifications(int numDownloads, List<Downloader> downloads) {
        if (notificationCompatBuilder == null) {
            return null;
        }

        String contentTitle = context.getString(R.string.download_notification_title);
        String downloadsLeft = (numDownloads > 0)
                ? context.getResources().getQuantityString(R.plurals.downloads_left, numDownloads, numDownloads)
                : context.getString(R.string.downloads_processing);
        String bigText = compileNotificationString(downloads);

        notificationCompatBuilder.setContentTitle(contentTitle);
        notificationCompatBuilder.setContentText(downloadsLeft);
        notificationCompatBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        return notificationCompatBuilder.build();
    }

    private static String compileNotificationString(List<Downloader> downloads) {
        List<String> lines = new ArrayList<>(downloads.size());
        for (Downloader downloader : downloads) {
            if (downloader.cancelled) {
                continue;
            }
            StringBuilder line = new StringBuilder("• ");
            DownloadRequest request = downloader.getDownloadRequest();
            switch (request.getFeedfileType()) {
                case Feed.FEEDFILETYPE_FEED:
                    if (request.getTitle() != null) {
                        line.append(request.getTitle());
                    }
                    break;
                case FeedMedia.FEEDFILETYPE_FEEDMEDIA:
                    if (request.getTitle() != null) {
                        line.append(request.getTitle())
                                .append(" (")
                                .append(request.getProgressPercent())
                                .append("%)");
                    }
                    break;
                default:
                    line.append("Unknown: ").append(request.getFeedfileType());
            }
            lines.add(line.toString());
        }
        return TextUtils.join("\n", lines);
    }

    private static String createAutoDownloadNotificationContent(List<DownloadStatus> statuses) {
        int length = statuses.size();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append("• ").append(statuses.get(i).getTitle());
            if (i != length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Creates a notification at the end of the service lifecycle to notify the
     * user about the number of completed downloads. A report will only be
     * created if there is at least one failed download excluding images
     */
    public void updateReport(List<DownloadStatus> reportQueue, boolean showAutoDownloadReport) {
        // check if report should be created
        boolean createReport = false;
        int successfulDownloads = 0;
        int failedDownloads = 0;

        // a download report is created if at least one download has failed
        // (excluding failed image downloads)
        for (DownloadStatus status : reportQueue) {
            if (status.isSuccessful()) {
                successfulDownloads++;
                createReport |= showAutoDownloadReport && !status.isInitiatedByUser() && status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA;
            } else if (!status.isCancelled()) {
                failedDownloads++;
                createReport = true;
            }
        }

        if (createReport) {
            Log.d(TAG, "Creating report");

            // create notification object
            String channelId;
            int titleId;
            int iconId;
            int id;
            String content;
            PendingIntent intent;
            if (failedDownloads == 0) {
                // We are generating an auto-download report
                channelId = NotificationUtils.CHANNEL_ID_AUTO_DOWNLOAD;
                titleId = R.string.auto_download_report_title;
                iconId = R.drawable.auto_download_complete;
                intent = ClientConfig.downloadServiceCallbacks.getAutoDownloadReportNotificationContentIntent(context);
                id = AUTO_REPORT_ID;
                StringBuilder sb = new StringBuilder();
                for (DownloadStatus status: reportQueue) {
                    sb.append("A");
                    sb.append("\n");
                }
                content = createAutoDownloadNotificationContent(reportQueue);
            } else {
                channelId = NotificationUtils.CHANNEL_ID_ERROR;
                titleId = R.string.download_report_title;
                iconId = R.drawable.stat_notify_sync_error;
                intent = ClientConfig.downloadServiceCallbacks.getReportNotificationContentIntent(context);
                id = REPORT_ID;
                content = String.format(context.getString(R.string.download_report_content), successfulDownloads, failedDownloads);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
            builder.setTicker(context.getString(titleId))
                   .setContentTitle(context.getString(titleId))
                   .setContentText(content)
                   .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                   .setSmallIcon(iconId)
                   .setContentIntent(intent)
                   .setAutoCancel(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            }
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(id, builder.build());
        } else {
            Log.d(TAG, "No report is created");
        }
    }

    public void postAuthenticationNotification(final DownloadRequest downloadRequest) {
        final String resourceTitle = (downloadRequest.getTitle() != null) ?
                downloadRequest.getTitle() : downloadRequest.getSource();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_USER_ACTION);
        builder.setTicker(context.getText(R.string.authentication_notification_title))
                .setContentTitle(context.getText(R.string.authentication_notification_title))
                .setContentText(context.getText(R.string.authentication_notification_msg))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getText(R.string.authentication_notification_msg)
                        + ": " + resourceTitle))
                .setSmallIcon(R.drawable.ic_key_white)
                .setAutoCancel(true)
                .setContentIntent(ClientConfig.downloadServiceCallbacks.getAuthentificationNotificationContentIntent(context, downloadRequest));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(downloadRequest.getSource().hashCode(), builder.build());
    }
}
