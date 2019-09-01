package de.danoeh.antennapod.core.service;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.awaitility.Awaitility;

import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.NetworkUtils;

public class FeedUpdateWorker extends Worker {

    private static final String TAG = "FeedUpdateWorker";

    public static final String PARAM_RUN_IMMEDIATE = "runImmediate";

    public FeedUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        final boolean isImmediate = getInputData() != null ?
                getInputData().getBoolean(PARAM_RUN_IMMEDIATE, false) : false;
        Log.d(TAG, "doWork() : isImmediate = " + isImmediate);
        ClientConfig.initialize(getApplicationContext());

        if (NetworkUtils.networkAvailable() && NetworkUtils.isFeedRefreshAllowed()) {
            AtomicBoolean finished = new AtomicBoolean(false);
            DBTasks.refreshAllFeeds(getApplicationContext(), null, () -> finished.set(true));
            Awaitility.await().until(finished::get);
        } else {
            Log.d(TAG, "Blocking automatic update: no wifi available / no mobile updates allowed");
        }

        if (!isImmediate && UserPreferences.isAutoUpdateTimeOfDay()) {
            // WorkManager does not allow to set specific time for repeated tasks.
            // We repeatedly schedule a OneTimeWorkRequest instead.
            UserPreferences.restartUpdateAlarm();
        }

        return Result.success();
    }
}
