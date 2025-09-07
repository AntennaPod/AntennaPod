package de.danoeh.antennapod.storage.databasemaintenanceservice;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;

import java.util.concurrent.TimeUnit;

public class DatabaseMaintenanceWorker extends Worker {
    private static final String WORK_ID_DATABASE_MAINTENANCE = "DatabaseMaintenanceWorker";

    public static void enqueueIfNeeded(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                DatabaseMaintenanceWorker.class, 3, TimeUnit.DAYS).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_ID_DATABASE_MAINTENANCE,
                        ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }

    public DatabaseMaintenanceWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.clearOldDownloadLog();
        adapter.close();
        return Result.success();
    }

    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        return Futures.immediateFuture(new ForegroundInfo(R.id.notification_db_maintenance,
                new NotificationCompat.Builder(getApplicationContext(), NotificationUtils.CHANNEL_ID_REFRESHING)
                    .setContentTitle(getApplicationContext().getString(R.string.download_notification_title_feeds))
                    .setSmallIcon(R.drawable.ic_notification_sync)
                    .setOngoing(true)
                    .build()));
    }
}
