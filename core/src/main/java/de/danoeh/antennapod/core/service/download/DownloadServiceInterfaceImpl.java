package de.danoeh.antennapod.core.service.download;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.google.android.exoplayer2.util.Log;
import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;

import java.util.ArrayList;

import static de.danoeh.antennapod.core.service.download.DownloadService.isDownloadingFile;

public class DownloadServiceInterfaceImpl extends DownloadServiceInterface {
    private static final String TAG = "DownloadServiceInterface";

    public void download(Context context, boolean cleanupMedia, DownloadRequest... requests) {
        ArrayList<DownloadRequest> requestsToSend = new ArrayList<>();
        for (DownloadRequest request : requests) {
            if (!isDownloadingFile(request.getSource())) {
                requestsToSend.add(request);
            }
        }
        if (requestsToSend.isEmpty()) {
            return;
        } else if (requestsToSend.size() > 100) {
            if (BuildConfig.DEBUG) {
                throw new IllegalArgumentException("Android silently drops intent payloads that are too large");
            } else {
                Log.d(TAG, "Too many download requests. Dropping some to avoid Android dropping all.");
                requestsToSend = new ArrayList<>(requestsToSend.subList(0, 100));
            }
        }

        Intent launchIntent = new Intent(context, DownloadService.class);
        launchIntent.putParcelableArrayListExtra(DownloadService.EXTRA_REQUESTS, requestsToSend);
        if (cleanupMedia) {
            launchIntent.putExtra(DownloadService.EXTRA_CLEANUP_MEDIA, true);
        }
        ContextCompat.startForegroundService(context, launchIntent);
    }

    public void refreshAllFeeds(Context context, boolean initiatedByUser) {
        Intent launchIntent = new Intent(context, DownloadService.class);
        launchIntent.putExtra(DownloadService.EXTRA_REFRESH_ALL, true);
        launchIntent.putExtra(DownloadService.EXTRA_INITIATED_BY_USER, initiatedByUser);
        ContextCompat.startForegroundService(context, launchIntent);
    }

    public void cancel(Context context, String url) {
        if (!DownloadService.isRunning) {
            return;
        }
        Intent cancelIntent = new Intent(DownloadService.ACTION_CANCEL_DOWNLOAD);
        cancelIntent.putExtra(DownloadService.EXTRA_DOWNLOAD_URL, url);
        cancelIntent.setPackage(context.getPackageName());
        context.sendBroadcast(cancelIntent);
    }

    public void cancelAll(Context context) {
        if (!DownloadService.isRunning) {
            return;
        }
        Intent cancelIntent = new Intent(DownloadService.ACTION_CANCEL_ALL_DOWNLOADS);
        cancelIntent.setPackage(context.getPackageName());
        context.sendBroadcast(cancelIntent);
    }
}
