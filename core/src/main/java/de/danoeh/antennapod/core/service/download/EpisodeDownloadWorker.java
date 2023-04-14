package de.danoeh.antennapod.core.service.download;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.core.ClientConfigurator;
import de.danoeh.antennapod.core.service.download.handler.MediaDownloadedHandler;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class EpisodeDownloadWorker extends Worker {
    private static final String TAG = "EpisodeDownloadWorker";
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
                        setProgressAsync(
                                new Data.Builder()
                                    .putInt(DownloadServiceInterface.WORK_DATA_PROGRESS, request.getProgressPercent())
                                    .build())
                                .get();
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
        } catch (InterruptedException ignore) {
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
            return Result.failure();
        }

        DownloadStatus status = downloader.getResult();

        if (status.isSuccessful()) {
            MediaDownloadedHandler handler = new MediaDownloadedHandler(
                    getApplicationContext(), downloader.getResult(), request);
            handler.run();
            DBWriter.addDownloadStatus(handler.getUpdatedStatus());
            return Result.success();
        }

        if (status.isCancelled()) {
            if (getInputData().getBoolean(DownloadServiceInterface.WORK_DATA_WAS_QUEUED, false)) {
                try {
                    DBWriter.removeQueueItem(getApplicationContext(), false, media.getItem()).get();
                } catch (ExecutionException | InterruptedException ignore) {
                }
            }
            return Result.success();
        }

        if (status.getReason() == DownloadError.ERROR_HTTP_DATA_ERROR
                && Integer.parseInt(status.getReasonDetailed()) == 416) {
            Log.d(TAG, "Requested invalid range, restarting download from the beginning");
            FileUtils.deleteQuietly(new File(downloader.getDownloadRequest().getDestination()));
            return Result.retry();
        }

        Log.e(TAG, "Download failed");
        DBWriter.addDownloadStatus(status);

        if (getRunAttemptCount() < 3) {
            return Result.retry();
        } else {
            return Result.failure();
        }
    }
}
