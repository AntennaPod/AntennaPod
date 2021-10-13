package de.danoeh.antennapod.core.util.download;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.FeedUpdateWorker;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.util.NetworkUtils;

public class AutoUpdateManager {
    private static final String WORK_ID_FEED_UPDATE = "de.danoeh.antennapod.core.service.FeedUpdateWorker";
    private static final String WORK_ID_FEED_UPDATE_ONCE = WORK_ID_FEED_UPDATE + "Once";
    private static final String TAG = "AutoUpdateManager";

    private AutoUpdateManager() {

    }

    /**
     * Start / restart periodic auto feed refresh
     * @param context Context
     */
    public static void restartUpdateAlarm(Context context) {
        if (UserPreferences.isAutoUpdateDisabled()) {
            disableAutoUpdate(context);
        } else if (UserPreferences.isAutoUpdateTimeOfDay()) {
            int[] timeOfDay = UserPreferences.getUpdateTimeOfDay();
            Log.d(TAG, "timeOfDay: " + Arrays.toString(timeOfDay));
            restartUpdateTimeOfDayAlarm(timeOfDay[0], timeOfDay[1], context);
        } else {
            long milliseconds = UserPreferences.getUpdateInterval();
            restartUpdateIntervalAlarm(milliseconds, context);
        }
    }

    /**
     * Sets the interval in which the feeds are refreshed automatically
     */
    private static void restartUpdateIntervalAlarm(long intervalMillis, Context context) {
        Log.d(TAG, "Restarting update alarm.");

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(FeedUpdateWorker.class,
                intervalMillis, TimeUnit.MILLISECONDS)
                .setConstraints(getConstraints())
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_ID_FEED_UPDATE, ExistingPeriodicWorkPolicy.REPLACE, workRequest);
    }

    /**
     * Sets time of day the feeds are refreshed automatically
     */
    private static void restartUpdateTimeOfDayAlarm(int hoursOfDay, int minute, Context context) {
        Log.d(TAG, "Restarting update alarm.");

        Calendar now = Calendar.getInstance();
        Calendar alarm = (Calendar) now.clone();
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

        WorkManager.getInstance(context).enqueueUniqueWork(WORK_ID_FEED_UPDATE,
                ExistingWorkPolicy.REPLACE, workRequest);
    }

    /**
     * Run auto feed refresh once in background, as soon as what OS scheduling allows.
     *
     * Callers from UI should use {@link #runImmediate(Context)}, as it will guarantee
     * the refresh be run immediately.
     * @param context Context
     */
    public static void runOnce(Context context) {
        Log.d(TAG, "Run auto update once, as soon as OS allows.");

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(FeedUpdateWorker.class)
                .setConstraints(getConstraints())
                .setInitialDelay(0L, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putBoolean(FeedUpdateWorker.PARAM_RUN_ONCE, true)
                        .build()
                )
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(WORK_ID_FEED_UPDATE_ONCE,
                ExistingWorkPolicy.REPLACE, workRequest);

    }

    /**
     /**
     * Run auto feed refresh once in background immediately, using its own thread.
     *
     * Callers where the additional threads is not suitable should use {@link #runOnce(Context)}
     */
    public static void runImmediate(@NonNull Context context) {
        Log.d(TAG, "Run auto update immediately in background.");
        if (!NetworkUtils.networkAvailable()) {
            Log.d(TAG, "Ignoring: No network connection.");
        } else if (NetworkUtils.isEpisodeDownloadAllowed()) {
            startRefreshAllFeeds(context);
        } else {
            confirmMobileAllFeedsRefresh(context);
        }
    }

    private static void confirmMobileAllFeedsRefresh(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.feed_refresh_title)
                .setMessage(R.string.confirm_mobile_feed_refresh_dialog_message)
                .setPositiveButton(R.string.yes,
                        (dialog, which) -> startRefreshAllFeeds(context))
                .setNegativeButton(R.string.no, null);
        builder.show();
    }

    private static void startRefreshAllFeeds(final Context context) {
        new Thread(() -> DBTasks.refreshAllFeeds(
                context.getApplicationContext(), true), "ManualRefreshAllFeeds").start();
    }

    public static void disableAutoUpdate(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_ID_FEED_UPDATE);
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
