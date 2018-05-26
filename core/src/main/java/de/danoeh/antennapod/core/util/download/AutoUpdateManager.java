package de.danoeh.antennapod.core.util.download;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;
import de.danoeh.antennapod.core.receiver.FeedUpdateReceiver;
import de.danoeh.antennapod.core.service.FeedUpdateJobService;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class AutoUpdateManager {
    private static final int JOB_ID_FEED_UPDATE = 42;
    private static final String TAG = "AutoUpdateManager";

    private AutoUpdateManager() {

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static JobInfo.Builder getFeedUpdateJobBuilder(Context context) {
        ComponentName serviceComponent = new ComponentName(context, FeedUpdateJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID_FEED_UPDATE, serviceComponent);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setPersisted(true);
        return builder;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void restartJobServiceInterval(Context context, long intervalMillis) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        if (jobScheduler == null) {
            Log.d(TAG, "JobScheduler was null.");
            return;
        }

        JobInfo oldJob = jobScheduler.getPendingJob(JOB_ID_FEED_UPDATE);
        if (oldJob != null && oldJob.getIntervalMillis() == intervalMillis) {
            Log.d(TAG, "JobScheduler was already set at interval " + intervalMillis + ", ignoring.");
            return;
        }

        JobInfo.Builder builder = getFeedUpdateJobBuilder(context);
        builder.setPeriodic(intervalMillis);
        jobScheduler.cancel(JOB_ID_FEED_UPDATE);

        if (intervalMillis <= 0) {
            Log.d(TAG, "Automatic update was deactivated");
            return;
        }

        jobScheduler.schedule(builder.build());
        Log.d(TAG, "JobScheduler was set at interval " + intervalMillis);
    }

    public static void restartAlarmManagerInterval(Context context, long triggerAtMillis, long intervalMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            Log.d(TAG, "AlarmManager was null");
            return;
        }

        PendingIntent updateIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(context, FeedUpdateReceiver.class), 0);
        alarmManager.cancel(updateIntent);

        if (intervalMillis <= 0) {
            Log.d(TAG, "Automatic update was deactivated");
            return;
        }

        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + triggerAtMillis,
                updateIntent);
        Log.d(TAG, "Changed alarm to new interval " + TimeUnit.MILLISECONDS.toHours(intervalMillis) + " h");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void restartJobServiceTriggerAt(Context context, long triggerAtMillis) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        if (jobScheduler == null) {
            Log.d(TAG, "JobScheduler was null.");
            return;
        }

        JobInfo.Builder builder = getFeedUpdateJobBuilder(context);
        builder.setMinimumLatency(triggerAtMillis);
        jobScheduler.cancel(JOB_ID_FEED_UPDATE);
        jobScheduler.schedule(builder.build());
        Log.d(TAG, "JobScheduler was set for " + triggerAtMillis);
    }

    public static void restartAlarmManagerTimeOfDay(Context context, Calendar alarm) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            Log.d(TAG, "AlarmManager was null");
            return;
        }

        PendingIntent updateIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(context, FeedUpdateReceiver.class), 0);
        alarmManager.cancel(updateIntent);

        Log.d(TAG, "Alarm set for: " + alarm.toString() + " : " + alarm.getTimeInMillis());
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                alarm.getTimeInMillis(),
                updateIntent);
        Log.d(TAG, "Changed alarm to new time of day " + alarm.get(Calendar.HOUR_OF_DAY) + ":" + alarm.get(Calendar.MINUTE));
    }
}
