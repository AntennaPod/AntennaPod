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
    private static final String PREFS_NAME = "PlayerWidgetPrefs";
    private static final String KEY_ENABLED = "WidgetEnabled";

    public static final String WIDGET_PREFS = "widget_preferences";
    public static final String WIDGET_COLOR = "widget_color";

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
        Log.d(TAG,"OnDeleted");
        for (int appWidgetId : appWidgetIds) {
            SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(WIDGET_COLOR + appWidgetId);
            Log.d(TAG, "Widget deleted ID " + appWidgetId);
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
