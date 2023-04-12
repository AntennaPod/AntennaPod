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

import java.util.concurrent.TimeUnit;

public class DownloadServiceInterfaceImpl extends DownloadServiceInterface {
    public void download(Context context, FeedItem item) {
        OneTimeWorkRequest.Builder workRequest = new OneTimeWorkRequest.Builder(EpisodeDownloadWorker.class)
                .setInitialDelay(0L, TimeUnit.MILLISECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(DownloadServiceInterface.WORK_TAG)
                .addTag(DownloadServiceInterface.WORK_TAG_EPISODE_URL + item.getMedia().getDownload_url());
        Data.Builder builder = new Data.Builder();
        builder.putLong(WORK_DATA_MEDIA_ID, item.getMedia().getId());
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
        return null;
    }

    public void cancel(Context context, String url) {
    }

    public void cancelAll(Context context) {
    }
}
