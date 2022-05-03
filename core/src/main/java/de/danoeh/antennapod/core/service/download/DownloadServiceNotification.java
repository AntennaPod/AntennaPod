package de.danoeh.antennapod.core.service.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.util.DownloadErrorLabel;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;

import java.util.List;

public class DownloadServiceNotification {
    private static final String TAG = "DownloadSvcNotification";

    private final Context context;
    private NotificationCompat.Builder notificationCompatBuilder;

    public DownloadServiceNotification(Context context) {
        this.context = context;
        setupNotificationBuilders();
    }

    private void setupNotificationBuilders() {
        notificationCompatBuilder = new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_DOWNLOADING)
                .setOngoing(false)
                .setWhen(0)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(ClientConfig.downloadServiceCallbacks.getNotificationContentIntent(context))
                .setSmallIcon(R.drawable.ic_notification_sync);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationCompatBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }

        Log.d(TAG, "Notification set up");
    }

    /**
     * Updates the contents of the service's notifications. Should be called
     * after setupNotificationBuilders.
     */
    public Notification updateNotifications(List<Downloader> downloads) {
        if (notificationCompatBuilder == null) {
            return null;
        }

        String contentTitle;
        if (typeIsOnly(downloads, Feed.FEEDFILETYPE_FEED)) {
            contentTitle = context.getString(R.string.download_notification_title_feeds);
        } else if (typeIsOnly(downloads, FeedMedia.FEEDFILETYPE_FEEDMEDIA)) {
            contentTitle = context.getString(R.string.download_notification_title_episodes);
        } else {
            contentTitle = context.getString(R.string.download_notification_title);
        }
        String contentText = (downloads.size() > 0)
                ? context.getResources().getQuantityString(R.plurals.downloads_left, downloads.size(), downloads.size())
                : context.getString(R.string.completing);
        String bigText = compileNotificationString(downloads);
        if (!bigText.contains("\n")) {
            contentText = bigText;
        }

        notificationCompatBuilder.setContentTitle(contentTitle);
        notificationCompatBuilder.setContentText(contentText);
        notificationCompatBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        return notificationCompatBuilder.build();
    }

    private boolean typeIsOnly(List<Downloader> downloads, int feedFileType) {
        for (Downloader downloader : downloads) {
            if (downloader.cancelled) {
                continue;
            }
            DownloadRequest request = downloader.getDownloadRequest();
            if (request.getFeedfileType() != feedFileType) {
                return false;
            }
        }
        return true;
    }

    private static String compileNotificationString(List<Downloader> downloads) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < downloads.size(); i++) {
            Downloader downloader = downloads.get(i);
            if (downloader.cancelled) {
                continue;
            }
            stringBuilder.append("• ");
            DownloadRequest request = downloader.getDownloadRequest();
            if (request.getTitle() != null) {
                stringBuilder.append(request.getTitle());
            } else {
                stringBuilder.append(request.getSource());
            }
            if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                stringBuilder.append(" (").append(request.getProgressPercent()).append("%)");
            } else if (request.getSource().startsWith(Feed.PREFIX_LOCAL_FOLDER)) {
                stringBuilder.append(" (").append(request.getSoFar())
                        .append("/").append(request.getSize()).append(")");
            }
            if (i != downloads.size() - 1) {
                stringBuilder.append("\n");
            }
        }
        return stringBuilder.toString();
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

    private String createFailedDownloadNotificationContent(List<DownloadStatus> statuses) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < statuses.size(); i++) {
            if (statuses.get(i).isSuccessful()) {
                continue;
            }
            sb.append("• ").append(statuses.get(i).getTitle());
            if (statuses.get(i).getReason() != null) {
                sb.append(": ").append(context.getString(DownloadErrorLabel.from(statuses.get(i).getReason())));
            }
            if (i != statuses.size() - 1) {
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
        int failedDownloads = 0;

        // a download report is created if at least one download has failed
        // (excluding failed image downloads)
        for (DownloadStatus status : reportQueue) {
            if (status == null || status.isCancelled()) {
                continue;
            }
            if (status.isSuccessful()) {
                createReport |= showAutoDownloadReport && !status.isInitiatedByUser()
                        && status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA;
            } else {
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
                iconId = R.drawable.ic_notification_new;
                intent = ClientConfig.downloadServiceCallbacks.getAutoDownloadReportNotificationContentIntent(context);
                id = R.id.notification_auto_download_report;
                content = createAutoDownloadNotificationContent(reportQueue);
            } else {
                channelId = NotificationUtils.CHANNEL_ID_DOWNLOAD_ERROR;
                titleId = R.string.download_report_title;
                iconId = R.drawable.ic_notification_sync_error;
                intent = ClientConfig.downloadServiceCallbacks.getReportNotificationContentIntent(context);
                id = R.id.notification_download_report;
                content = createFailedDownloadNotificationContent(reportQueue);
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
            Log.d(TAG, "Download report notification was posted");
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
                .setSmallIcon(R.drawable.ic_notification_key)
                .setAutoCancel(true)
                .setContentIntent(ClientConfig.downloadServiceCallbacks.getAuthentificationNotificationContentIntent(context, downloadRequest));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(downloadRequest.getSource().hashCode(), builder.build());
    }
}
