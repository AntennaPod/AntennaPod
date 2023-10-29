package de.danoeh.antennapod.core.service.download;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.core.ClientConfigurator;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.service.download.handler.MediaDownloadedHandler;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import org.apache.commons.io.FileUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class EpisodeDownloadWorker extends Worker {
    private static final String TAG = "EpisodeDownloadWorker";
    private static final Map<String, Integer> notificationProgress = new HashMap<>();

    private Downloader downloader = null;

    public EpisodeDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        ClientConfigurator.initialize(getApplicationContext());
        long mediaId = getInputData().getLong(DownloadServiceInterface.WORK_DATA_MEDIA_ID, 0);
        FeedMedia media = DBReader.getFeedMedia(mediaId);
        if (media == null) {
            return Result.failure();
        }

        DownloadRequest request = DownloadRequestCreator.create(media).build();
        Thread progressUpdaterThread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(1000);
                        notificationProgress.put(media.getEpisodeTitle(), request.getProgressPercent());
                        setProgressAsync(
                                new Data.Builder()
                                    .putInt(DownloadServiceInterface.WORK_DATA_PROGRESS, request.getProgressPercent())
                                    .build())
                                .get();
                        sendProgressNotification();
                    } catch (InterruptedException | ExecutionException e) {
                        return;
                    }
                }
            }
        };
        progressUpdaterThread.start();
        final Result result = performDownload(media, request);
        progressUpdaterThread.interrupt();
        try {
            progressUpdaterThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        notificationProgress.remove(media.getEpisodeTitle());
        if (notificationProgress.isEmpty()) {
            NotificationManager nm = (NotificationManager) getApplicationContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(R.id.notification_downloading);
        }
        Log.d(TAG, "Worker for " + media.getDownload_url() + " returned.");
        return result;
    }

    @Override
    public void onStopped() {
        super.onStopped();
        if (downloader != null) {
            downloader.cancel();
        }
    }

    private Result performDownload(FeedMedia media, DownloadRequest request) {
        File dest = new File(request.getDestination());
        if (!dest.exists()) {
            try {
                dest.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "Unable to create file");
            }
        }

        if (dest.exists()) {
            media.setFile_url(request.getDestination());
            try {
                DBWriter.setFeedMedia(media).get();
            } catch (Exception e) {
                Log.e(TAG, "ExecutionException in writeFileUrl: " + e.getMessage());
            }
        }

        downloader = new DefaultDownloaderFactory().create(request);
        if (downloader == null) {
            Log.d(TAG, "Unable to create downloader");
            return Result.failure();
        }

        try {
            downloader.call();
        } catch (Exception e) {
            DBWriter.addDownloadStatus(downloader.getResult());
            sendErrorNotification(request.getTitle());
            FileUtils.deleteQuietly(new File(downloader.getDownloadRequest().getDestination()));
            return Result.failure();
        }

        if (downloader.cancelled) {
            if (getInputData().getBoolean(DownloadServiceInterface.WORK_DATA_WAS_QUEUED, false)) {
                try {
                    DBWriter.removeQueueItem(getApplicationContext(), false, media.getItem()).get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return Result.success();
        }

        DownloadResult status = downloader.getResult();
        if (status.isSuccessful()) {
            MediaDownloadedHandler handler = new MediaDownloadedHandler(
                    getApplicationContext(), downloader.getResult(), request);
            handler.run();
            DBWriter.addDownloadStatus(handler.getUpdatedStatus());
            return Result.success();
        }

        if (status.getReason() == DownloadError.ERROR_HTTP_DATA_ERROR
                && Integer.parseInt(status.getReasonDetailed()) == 416) {
            Log.d(TAG, "Requested invalid range, restarting download from the beginning");
            FileUtils.deleteQuietly(new File(downloader.getDownloadRequest().getDestination()));
            sendMessage(request.getTitle(), true);
            if (isLastRunAttempt()) {
                FileUtils.deleteQuietly(new File(downloader.getDownloadRequest().getDestination()));
            }
            return retry3times();
        }

        Log.e(TAG, "Download failed");
        DBWriter.addDownloadStatus(status);
        if (status.getReason() == DownloadError.ERROR_FORBIDDEN
                || status.getReason() == DownloadError.ERROR_NOT_FOUND
                || status.getReason() == DownloadError.ERROR_UNAUTHORIZED
                || status.getReason() == DownloadError.ERROR_IO_BLOCKED) {
            // Fail fast, these are probably unrecoverable
            sendErrorNotification(request.getTitle());
            FileUtils.deleteQuietly(new File(downloader.getDownloadRequest().getDestination()));
            return Result.failure();
        }
        sendMessage(request.getTitle(), true);
        if (isLastRunAttempt()) {
            FileUtils.deleteQuietly(new File(downloader.getDownloadRequest().getDestination()));
        }
        return retry3times();
    }

    private Result retry3times() {
        if (isLastRunAttempt()) {
            sendErrorNotification(downloader.getDownloadRequest().getTitle());
            return Result.failure();
        } else {
            return Result.retry();
        }
    }

    private boolean isLastRunAttempt() {
        return getRunAttemptCount() >= 2;
    }

    private void sendMessage(String episodeTitle, boolean retrying) {
        if (episodeTitle.length() > 20) {
            episodeTitle = episodeTitle.substring(0, 19) + "…";
        }
        EventBus.getDefault().post(new MessageEvent(
                    getApplicationContext().getString(
                            retrying ? R.string.download_error_retrying : R.string.download_error_not_retrying,
                            episodeTitle), (ctx) -> new MainActivityStarter(ctx).withDownloadLogsOpen().start(),
                getApplicationContext().getString(R.string.download_error_details)));
    }

    private PendingIntent getDownloadLogsIntent(Context context) {
        Intent intent = new MainActivityStarter(context).withDownloadLogsOpen().getIntent();
        return PendingIntent.getActivity(context, R.id.pending_intent_download_service_report, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    private PendingIntent getDownloadsIntent(Context context) {
        Intent intent = new MainActivityStarter(context).withFragmentLoaded("DownloadsFragment").getIntent();
        return PendingIntent.getActivity(context, R.id.pending_intent_download_service_notification, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    private void sendErrorNotification(String title) {
        if (EventBus.getDefault().hasSubscriberForEvent(MessageEvent.class)) {
            sendMessage(title, false);
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                NotificationUtils.CHANNEL_ID_DOWNLOAD_ERROR);
        builder.setTicker(getApplicationContext().getString(R.string.download_report_title))
                .setContentTitle(getApplicationContext().getString(R.string.download_report_title))
                .setContentText(getApplicationContext().getString(R.string.download_error_tap_for_details))
                .setSmallIcon(R.drawable.ic_notification_sync_error)
                .setContentIntent(getDownloadLogsIntent(getApplicationContext()))
                .setAutoCancel(true);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationManager nm = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(R.id.notification_download_report, builder.build());
    }

    private void sendProgressNotification() {
        StringBuilder bigTextB = new StringBuilder();
        Map<String, Integer> progressCopy = new HashMap<>(notificationProgress);
        for (Map.Entry<String, Integer> entry : progressCopy.entrySet()) {
            bigTextB.append(String.format(Locale.getDefault(), "%s (%d%%)\n", entry.getKey(), entry.getValue()));
        }
        String bigText = bigTextB.toString().trim();
        String contentText;
        if (notificationProgress.size() == 1) {
            contentText = bigText;
        } else {
            contentText = getApplicationContext().getResources().getQuantityString(R.plurals.downloads_left,
                    notificationProgress.size(), notificationProgress.size());
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                NotificationUtils.CHANNEL_ID_DOWNLOADING);
        builder.setTicker(getApplicationContext().getString(R.string.download_notification_title_episodes))
                .setContentTitle(getApplicationContext().getString(R.string.download_notification_title_episodes))
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setContentIntent(getDownloadsIntent(getApplicationContext()))
                .setAutoCancel(false)
                .setOngoing(true)
                .setWhen(0)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setSmallIcon(R.drawable.ic_notification_sync)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationManager nm = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(R.id.notification_downloading, builder.build());
    }
}
