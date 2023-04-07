package de.danoeh.antennapod.core.service.download;

import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import androidx.work.OneTimeWorkRequest;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DownloadServiceInterfaceImpl extends DownloadServiceInterface {
    private static final String TAG = "DownloadServiceInterface";

    public void download(Context context, FeedItem item) {
        OneTimeWorkRequest.Builder workRequest = new OneTimeWorkRequest.Builder(EpisodeDownloadWorker.class)
                .setInitialDelay(0L, TimeUnit.MILLISECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(DownloadServiceInterface.WORK_TAG)
                .addTag(DownloadServiceInterface.WORK_TAG_EPISODE_URL + item.getMedia().getDownload_url());
        Data.Builder builder = new Data.Builder();
        builder.putString("episode", item.getMedia().getDownload_url());
        workRequest.setInputData(builder.build());
        WorkManager.getInstance(context).enqueueUniqueWork(item.getMedia().getDownload_url(),
                ExistingWorkPolicy.KEEP, workRequest.build());
    }

    private static Constraints getConstraints() {
        Constraints.Builder constraints = new Constraints.Builder();
        if (UserPreferences.isAllowMobileEpisodeDownload()) {
            constraints.setRequiredNetworkType(NetworkType.CONNECTED);
        } else {
            constraints.setRequiredNetworkType(NetworkType.UNMETERED);
        }
        return constraints.build();
    }

    public void download(Context context, boolean cleanupMedia, DownloadRequest... requests) {
        Intent intent = makeDownloadIntent(context, cleanupMedia, requests);
        if (intent != null) {
            ContextCompat.startForegroundService(context, intent);
        }
    }

    public Intent makeDownloadIntent(Context context, boolean cleanupMedia, DownloadRequest... requests) {
        ArrayList<DownloadRequest> requestsToSend = new ArrayList<>();
        Intent launchIntent = new Intent(context, DownloadService.class);
        launchIntent.putParcelableArrayListExtra(DownloadService.EXTRA_REQUESTS, requestsToSend);
        if (cleanupMedia) {
            launchIntent.putExtra(DownloadService.EXTRA_CLEANUP_MEDIA, true);
        }
        return launchIntent;
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
