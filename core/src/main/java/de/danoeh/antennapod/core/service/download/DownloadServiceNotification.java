package de.danoeh.antennapod.core.service.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.util.DownloadErrorLabel;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.ui.appstartintent.DownloadAuthenticationActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;

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
                .setContentIntent(getNotificationContentIntent(context))
                .setSmallIcon(R.drawable.ic_notification_sync)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
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

        int numDownloads = getNumberOfRunningDownloads(downloads);
        String contentText = context.getString(R.string.completing);
        String bigText = context.getString(R.string.completing);
        notificationCompatBuilder.clearActions();
        if (numDownloads > 0) {
            bigText = compileNotificationString(downloads);
            if (numDownloads == 1) {
                contentText = bigText;
            } else {
                contentText = context.getResources().getQuantityString(R.plurals.downloads_left,
                        numDownloads, numDownloads);
            }

            Intent cancelDownloadsIntent = new Intent(DownloadService.ACTION_CANCEL_ALL_DOWNLOADS);
            cancelDownloadsIntent.setPackage(context.getPackageName());
            PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context,
                    R.id.pending_intent_download_cancel_all, cancelDownloadsIntent, PendingIntent.FLAG_UPDATE_CURRENT
                            | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            notificationCompatBuilder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_notification_cancel, context.getString(R.string.cancel_label), cancelPendingIntent));
        }

        notificationCompatBuilder.setContentTitle(contentTitle);
        notificationCompatBuilder.setContentText(contentText);
        notificationCompatBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        return notificationCompatBuilder.build();
    }

    private int getNumberOfRunningDownloads(List<Downloader> downloads) {
        int running = 0;
        for (Downloader downloader : downloads) {
            if (!downloader.cancelled && !downloader.isFinished()) {
                running++;
            }
        }
        return running;
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
            if (statuses.get(i) == null || statuses.get(i).isSuccessful()) {
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
    public void updateReport(List<DownloadStatus> reportQueue, boolean showAutoDownloadReport,
                             List<DownloadRequest> failedRequests) {
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

        if (!createReport) {
            Log.d(TAG, "No report is created");
            return;
        }
        Log.d(TAG, "Creating report");
        if (failedDownloads == 0) {
            createAutoDownloadReportNotification(reportQueue);
        } else {
            createDownloadFailedNotification(reportQueue, failedRequests);
        }
        Log.d(TAG, "Download report notification was posted");
    }

    private void createAutoDownloadReportNotification(List<DownloadStatus> reportQueue) {
        PendingIntent intent = getAutoDownloadReportNotificationContentIntent(context);
        String content = createAutoDownloadNotificationContent(reportQueue);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NotificationUtils.CHANNEL_ID_AUTO_DOWNLOAD);
        builder.setTicker(context.getString(R.string.auto_download_report_title))
                .setContentTitle(context.getString(R.string.auto_download_report_title))
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setSmallIcon(R.drawable.ic_notification_new)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(R.id.notification_auto_download_report, builder.build());
    }

    private void createDownloadFailedNotification(List<DownloadStatus> reportQueue,
                                                  List<DownloadRequest> failedRequests) {
        Intent retryIntent = DownloadServiceInterface.get().makeDownloadIntent(context,
                false, failedRequests.toArray(new DownloadRequest[0]));
        PendingIntent retryPendingIntent = null;
        if (retryIntent != null && Build.VERSION.SDK_INT >= 26) {
            retryPendingIntent = PendingIntent.getForegroundService(context, R.id.pending_intent_download_service_retry,
                    retryIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else if (retryIntent != null) {
            retryPendingIntent = PendingIntent.getService(context,
                    R.id.pending_intent_download_service_retry, retryIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        }
        PendingIntent intent = getReportNotificationContentIntent(context);
        String content = createFailedDownloadNotificationContent(reportQueue);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NotificationUtils.CHANNEL_ID_DOWNLOAD_ERROR);
        builder.setTicker(context.getString(R.string.download_report_title))
                .setContentTitle(context.getString(R.string.download_report_title))
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setSmallIcon(R.drawable.ic_notification_sync_error)
                .setContentIntent(intent)
                .setAutoCancel(true);
        if (retryPendingIntent != null) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_notification_sync, context.getString(R.string.retry_label), retryPendingIntent));
        }
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(R.id.notification_download_report, builder.build());
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
                .setContentIntent(new DownloadAuthenticationActivityStarter(
                        context, downloadRequest.getFeedfileId(), downloadRequest).getPendingIntent());
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(downloadRequest.getSource().hashCode(), builder.build());
    }

    public PendingIntent getReportNotificationContentIntent(Context context) {
        Intent intent = new MainActivityStarter(context)
                .withFragmentLoaded("DownloadsFragment")
                .withFragmentArgs("show_logs", true)
                .getIntent();
        return PendingIntent.getActivity(context, R.id.pending_intent_download_service_report, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    public PendingIntent getAutoDownloadReportNotificationContentIntent(Context context) {
        Intent intent = new MainActivityStarter(context).withFragmentLoaded("QueueFragment").getIntent();
        return PendingIntent.getActivity(context, R.id.pending_intent_download_service_autodownload_report, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    public PendingIntent getNotificationContentIntent(Context context) {
        Intent intent = new MainActivityStarter(context).withFragmentLoaded("DownloadsFragment").getIntent();
        return PendingIntent.getActivity(context,
                R.id.pending_intent_download_service_notification, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }
}
