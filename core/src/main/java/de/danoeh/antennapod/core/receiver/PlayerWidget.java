package de.danoeh.antennapod.core.receiver;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;

import de.danoeh.antennapod.core.service.PlayerWidgetJobService;

public class PlayerWidget extends AppWidgetProvider {
    private static final String TAG = "PlayerWidget";
    public static final String PREFS_NAME = "PlayerWidgetPrefs";
    private static final String KEY_ENABLED = "WidgetEnabled";
    public static final String KEY_WIDGET_COLOR = "widget_color";
    public static final String KEY_WIDGET_SKIP = "widget_skip";
    public static final String KEY_WIDGET_FAST_FORWARD = "widget_fast_forward";
    public static final String KEY_WIDGET_REWIND = "widget_rewind";
    public static final int DEFAULT_COLOR = 0x00262C31;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        super.onReceive(context, intent);
        PlayerWidgetJobService.updateWidget(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled");
        setEnabled(context, true);
        PlayerWidgetJobService.updateWidget(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate() called with: " + "context = [" + context + "], appWidgetManager = [" + appWidgetManager + "], appWidgetIds = [" + Arrays.toString(appWidgetIds) + "]");
        PlayerWidgetJobService.updateWidget(context);
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
        for (int appWidgetId : appWidgetIds) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_WIDGET_COLOR + appWidgetId).apply();
            prefs.edit().remove(KEY_WIDGET_REWIND + appWidgetId).apply();
            prefs.edit().remove(KEY_WIDGET_FAST_FORWARD + appWidgetId).apply();
            prefs.edit().remove(KEY_WIDGET_SKIP + appWidgetId).apply();
        }
        super.onDeleted(context, appWidgetIds);
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
