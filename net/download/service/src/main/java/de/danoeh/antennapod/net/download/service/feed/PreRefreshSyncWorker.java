package de.danoeh.antennapod.net.download.service.feed;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import de.danoeh.antennapod.net.sync.service.SyncService;

public class PreRefreshSyncWorker extends Worker {
    private final WorkerParameters workerParams;

    public PreRefreshSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.workerParams = workerParams;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SyncService syncService = new SyncService(getApplicationContext(), workerParams);
            return syncService.doWork();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}