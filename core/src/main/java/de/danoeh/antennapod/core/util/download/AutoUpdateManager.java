package de.danoeh.antennapod.core.util.download;

import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.FeedUpdateWorker;

public class AutoUpdateManager {
    private static final String WORK_ID_FEED_UPDATE = FeedUpdateWorker.class.getName();
    private static final String WORK_ID_FEED_UPDATE_IMMEDIATE = FeedUpdateWorker.class.getName() +"Immediate";
    private static final String TAG = "AutoUpdateManager";

    private AutoUpdateManager() {

    }

    /**
     * Sets the interval in which the feeds are refreshed automatically
     */
    public static void restartUpdateIntervalAlarm(long intervalMillis) {
        Log.d(TAG, "Restarting update alarm.");

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(FeedUpdateWorker.class,
                intervalMillis, TimeUnit.MILLISECONDS)
                .setConstraints(getConstraints())
                .build();

        WorkManager.getInstance().enqueueUniquePeriodicWork(
                WORK_ID_FEED_UPDATE, ExistingPeriodicWorkPolicy.REPLACE, workRequest);
    }

    /**
     * Sets time of day the feeds are refreshed automatically
     */
    public static void restartUpdateTimeOfDayAlarm(int hoursOfDay, int minute) {
        Log.d(TAG, "Restarting update alarm.");

        Calendar now = Calendar.getInstance();
        Calendar alarm = (Calendar)now.clone();
        alarm.set(Calendar.HOUR_OF_DAY, hoursOfDay);
        alarm.set(Calendar.MINUTE, minute);
        if (alarm.before(now) || alarm.equals(now)) {
            alarm.add(Calendar.DATE, 1);
        }
        long triggerAtMillis = alarm.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(FeedUpdateWorker.class)
                .setConstraints(getConstraints())
                .setInitialDelay(triggerAtMillis, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance().enqueueUniqueWork(WORK_ID_FEED_UPDATE, ExistingWorkPolicy.REPLACE, workRequest);
    }

    public static void runImmediate() {
        Log.d(TAG, "Run auto update immediately.");

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(FeedUpdateWorker.class)
                .setConstraints(getConstraints())
                .setInitialDelay(0L, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putBoolean(FeedUpdateWorker.PARAM_RUN_IMMEDIATE, true)
                        .build()
                )
                .build();

        WorkManager.getInstance().enqueueUniqueWork(WORK_ID_FEED_UPDATE_IMMEDIATE, ExistingWorkPolicy.REPLACE, workRequest);

    }

    public static void disableAutoUpdate() {
        WorkManager.getInstance().cancelUniqueWork(WORK_ID_FEED_UPDATE);
    }

    private static Constraints getConstraints() {
        Constraints.Builder constraints = new Constraints.Builder();

        if (UserPreferences.isAllowMobileFeedRefresh()) {
            constraints.setRequiredNetworkType(NetworkType.CONNECTED);
        } else {
            constraints.setRequiredNetworkType(NetworkType.UNMETERED);
        }
        return constraints.build();
    }
}
