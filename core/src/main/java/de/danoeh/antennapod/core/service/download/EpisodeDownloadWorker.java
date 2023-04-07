package de.danoeh.antennapod.core.service.download;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.core.ClientConfigurator;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;

public class EpisodeDownloadWorker extends Worker {
    private static final String TAG = "EpisodeDownloadWorker";

    public EpisodeDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        ClientConfigurator.initialize(getApplicationContext());

        for (int i = 0; i < 100; i += 1) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            setProgressAsync(new Data.Builder().putInt(DownloadServiceInterface.WORK_DATA_PROGRESS, i).build());
        }
        return Result.success();
    }
}
