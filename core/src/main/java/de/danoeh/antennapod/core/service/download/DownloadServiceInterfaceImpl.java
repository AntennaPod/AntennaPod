package de.danoeh.antennapod.core.service.download;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.util.concurrent.TimeUnit;

public class DownloadServiceInterfaceImpl extends DownloadServiceInterface {
    public void download(Context context, FeedItem item, boolean ignoreConstraints) {
        OneTimeWorkRequest.Builder workRequest = new OneTimeWorkRequest.Builder(EpisodeDownloadWorker.class)
                .setInitialDelay(0L, TimeUnit.MILLISECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(DownloadServiceInterface.WORK_TAG)
                .addTag(DownloadServiceInterface.WORK_TAG_EPISODE_URL + item.getMedia().getDownload_url());
        Data.Builder builder = new Data.Builder();
        builder.putLong(WORK_DATA_MEDIA_ID, item.getMedia().getId());
        if (!item.isTagged(FeedItem.TAG_QUEUE) && UserPreferences.enqueueDownloadedEpisodes()) {
            DBWriter.addQueueItem(context, false, item.getId());
            builder.putBoolean(WORK_DATA_WAS_QUEUED, true);
        }
        workRequest.setInputData(builder.build());
        if (!ignoreConstraints) {
            workRequest.setConstraints(getConstraints());
        }
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

    @Override
    public void cancel(Context context, String url) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG_EPISODE_URL + url);
    }

    @Override
    public void cancelAll(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);
    }
}
