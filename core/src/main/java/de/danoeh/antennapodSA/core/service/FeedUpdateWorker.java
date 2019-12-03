package de.danoeh.antennapodSA.core.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import de.danoeh.antennapodSA.core.ClientConfig;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.core.storage.DBTasks;
import de.danoeh.antennapodSA.core.util.NetworkUtils;
import de.danoeh.antennapodSA.core.util.download.AutoUpdateManager;

public class FeedUpdateWorker extends Worker {

    private static final String TAG = "FeedUpdateWorker";

    public static final String PARAM_RUN_ONCE = "runOnce";

    public FeedUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        final boolean isRunOnce = getInputData().getBoolean(PARAM_RUN_ONCE, false);
        Log.d(TAG, "doWork() : isRunOnce = " + isRunOnce);
        ClientConfig.initialize(getApplicationContext());

        if (NetworkUtils.networkAvailable() && NetworkUtils.isFeedRefreshAllowed()) {
            DBTasks.refreshAllFeeds(getApplicationContext());
        } else {
            Log.d(TAG, "Blocking automatic update: no wifi available / no mobile updates allowed");
        }

        if (!isRunOnce && UserPreferences.isAutoUpdateTimeOfDay()) {
            // WorkManager does not allow to set specific time for repeated tasks.
            // We repeatedly schedule a OneTimeWorkRequest instead.
            AutoUpdateManager.restartUpdateAlarm();
        }

        return Result.success();
    }
}
