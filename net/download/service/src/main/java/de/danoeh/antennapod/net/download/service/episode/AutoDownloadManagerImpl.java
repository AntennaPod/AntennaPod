package de.danoeh.antennapod.net.download.service.episode;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.util.concurrent.TimeUnit;

public class AutoDownloadManagerImpl extends AutoDownloadManager {
    /**
     * Looks for non-downloaded episodes in the queue or list of unread items and request a download if
     * 1. Network is available
     * 2. The device is charging or the user allows auto download on battery
     * 3. There is free space in the episode cache
     * This method is executed on an internal single thread executor.
     */
    public void performAutoDownload(final Context context) {
        OneTimeWorkRequest.Builder workRequest = new OneTimeWorkRequest.Builder(AutoDownloadWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(UserPreferences.isEnableAutodownloadOnBattery()).build())
                .setInitialDelay(0L, TimeUnit.MILLISECONDS);
        WorkManager.getInstance(context).enqueueUniqueWork(AutoDownloadWorker.TAG,
                ExistingWorkPolicy.KEEP, workRequest.build());
    }

    /**
     * Removed downloaded episodes outside of the queue if the episode cache is full. Episodes with a smaller
     * 'playbackCompletionDate'-value will be deleted first.
     * <p/>
     * This method should NOT be executed on the GUI thread.
     */
    public void performAutoDeletion(final Context context) {
        OneTimeWorkRequest.Builder workRequest = new OneTimeWorkRequest.Builder(AutoDeleteWorker.class)
                .setInitialDelay(0L, TimeUnit.MILLISECONDS);
        WorkManager.getInstance(context).enqueueUniqueWork(AutoDownloadWorker.TAG,
                ExistingWorkPolicy.KEEP, workRequest.build());
    }
}
