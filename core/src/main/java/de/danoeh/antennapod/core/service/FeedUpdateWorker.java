package de.danoeh.antennapod.core.service;

import android.content.Context;
import android.support.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.FeedUpdateUtils;
import org.awaitility.Awaitility;

import java.util.concurrent.atomic.AtomicBoolean;

public class FeedUpdateWorker extends Worker {

    public FeedUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        ClientConfig.initialize(getApplicationContext());

        AtomicBoolean finished = new AtomicBoolean(false);
        FeedUpdateUtils.startAutoUpdate(getApplicationContext(), () -> finished.set(true));
        Awaitility.await().until(finished::get);

        if (UserPreferences.isAutoUpdateTimeOfDay()) {
            // WorkManager does not allow to set specific time for repeated tasks.
            // We repeatedly schedule a OneTimeWorkRequest instead.
            UserPreferences.restartUpdateAlarm();
        }

        return Result.success();
    }
}
