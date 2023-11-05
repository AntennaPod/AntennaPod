package de.danoeh.antennapod.core.service.download;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DownloadServiceInterfaceImpl extends DownloadServiceInterface {
    public void downloadNow(Context context, FeedItem item, boolean ignoreConstraints) {
        OneTimeWorkRequest.Builder workRequest = getRequest(context, item);
        workRequest.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST);
        if (ignoreConstraints) {
            workRequest.setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build());
        } else {
            workRequest.setConstraints(getConstraints());
        }
        WorkManager.getInstance(context).enqueueUniqueWork(item.getMedia().getDownload_url(),
                ExistingWorkPolicy.KEEP, workRequest.build());
    }

    public void download(Context context, FeedItem item) {
        OneTimeWorkRequest.Builder workRequest = getRequest(context, item);
        workRequest.setConstraints(getConstraints());
        WorkManager.getInstance(context).enqueueUniqueWork(item.getMedia().getDownload_url(),
                ExistingWorkPolicy.KEEP, workRequest.build());
    }

    private static OneTimeWorkRequest.Builder getRequest(Context context, FeedItem item) {
        OneTimeWorkRequest.Builder workRequest = new OneTimeWorkRequest.Builder(EpisodeDownloadWorker.class)
                .setInitialDelay(0L, TimeUnit.MILLISECONDS)
                .addTag(DownloadServiceInterface.WORK_TAG)
                .addTag(DownloadServiceInterface.WORK_TAG_EPISODE_URL + item.getMedia().getDownload_url());
        if (!item.isTagged(FeedItem.TAG_QUEUE) && UserPreferences.enqueueDownloadedEpisodes()) {
            DBWriter.addQueueItem(context, false, item.getId());
            workRequest.addTag(DownloadServiceInterface.WORK_DATA_WAS_QUEUED);
        }
        workRequest.setInputData(new Data.Builder().putLong(WORK_DATA_MEDIA_ID, item.getMedia().getId()).build());
        return workRequest;
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
    public void cancel(Context context, FeedMedia media) {
        // This needs to be done here, not in the worker. Reason: The worker might or might not be running.
        DBWriter.deleteFeedMediaOfItem(context, media.getId()); // Remove partially downloaded file
        String tag = WORK_TAG_EPISODE_URL + media.getDownload_url();
        Future<List<WorkInfo>> future = WorkManager.getInstance(context).getWorkInfosByTag(tag);
        Observable.fromFuture(future)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    workInfos -> {
                        for (WorkInfo info : workInfos) {
                            if (info.getTags().contains(DownloadServiceInterface.WORK_DATA_WAS_QUEUED)) {
                                DBWriter.removeQueueItem(context, false, media.getItem());
                            }
                        }
                        WorkManager.getInstance(context).cancelAllWorkByTag(tag);
                    }, exception -> {
                        WorkManager.getInstance(context).cancelAllWorkByTag(tag);
                        exception.printStackTrace();
                    });
    }

    @Override
    public void cancelAll(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);
    }
}
