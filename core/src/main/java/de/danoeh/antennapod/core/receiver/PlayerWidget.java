package de.danoeh.antennapod.core.receiver;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import de.danoeh.antennapod.core.widget.WidgetUpdaterWorker;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class PlayerWidget extends AppWidgetProvider {
    private static final String TAG = "PlayerWidget";
    public static final String PREFS_NAME = "PlayerWidgetPrefs";
    private static final String KEY_WORKAROUND_ENABLED = "WorkaroundEnabled";
    private static final String KEY_ENABLED = "WidgetEnabled";
    public static final String KEY_WIDGET_COLOR = "widget_color";
    public static final String KEY_WIDGET_PLAYBACK_SPEED = "widget_playback_speed";
    public static final String KEY_WIDGET_SKIP = "widget_skip";
    public static final String KEY_WIDGET_FAST_FORWARD = "widget_fast_forward";
    public static final String KEY_WIDGET_REWIND = "widget_rewind";
    public static final int DEFAULT_COLOR = 0xff262C31;
    private static final String WORKAROUND_WORK_NAME = "WidgetUpdaterWorkaround";

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled");
        setEnabled(context, true);
        WidgetUpdaterWorker.enqueueWork(context);
        scheduleWorkaround(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate() called with: " + "context = [" + context + "], appWidgetManager = ["
                + appWidgetManager + "], appWidgetIds = [" + Arrays.toString(appWidgetIds) + "]");
        WidgetUpdaterWorker.enqueueWork(context);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_WORKAROUND_ENABLED, false)) {
            scheduleWorkaround(context);
            prefs.edit().putBoolean(KEY_WORKAROUND_ENABLED, true).apply();
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "Widget disabled");
        setEnabled(context, false);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "OnDeleted");
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        for (int appWidgetId : appWidgetIds) {
            prefs.edit().remove(KEY_WIDGET_COLOR + appWidgetId).apply();
            prefs.edit().remove(KEY_WIDGET_PLAYBACK_SPEED + appWidgetId).apply();
            prefs.edit().remove(KEY_WIDGET_REWIND + appWidgetId).apply();
            prefs.edit().remove(KEY_WIDGET_FAST_FORWARD + appWidgetId).apply();
            prefs.edit().remove(KEY_WIDGET_SKIP + appWidgetId).apply();
        }
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, PlayerWidget.class));
        if (widgetIds.length == 0) {
            prefs.edit().putBoolean(KEY_WORKAROUND_ENABLED, false).apply();
            WorkManager.getInstance(context).cancelUniqueWork(WORKAROUND_WORK_NAME);
        }
        super.onDeleted(context, appWidgetIds);
    }

    private static void scheduleWorkaround(Context context) {
        // Enqueueing work enables a BOOT_COMPLETED receiver, which in turn makes Android refresh widgets.
        // This creates an endless loop with a flickering widget.
        // Workaround: When there is a widget, schedule a dummy task in the far future, so that the receiver stays.
        final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WidgetUpdaterWorker.class)
                .setInitialDelay(100 * 356, TimeUnit.DAYS)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniqueWork(WORKAROUND_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest);
    }

    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    private void setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
