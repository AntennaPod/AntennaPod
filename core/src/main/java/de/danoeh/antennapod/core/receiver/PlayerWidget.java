package de.danoeh.antennapod.core.receiver;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Arrays;

import de.danoeh.antennapod.core.service.PlayerWidgetJobService;


public class PlayerWidget extends AppWidgetProvider {
    private static final String TAG = "PlayerWidget";
    private static final String PREFS_NAME = "PlayerWidgetPrefs";
    private static final String KEY_ENABLED = "WidgetEnabled";

    @Override
    public void onReceive(@NonNull Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        super.onReceive(context, intent);
        PlayerWidgetJobService.updateWidget(context);
    }

    @Override
    public void onEnabled(@NonNull Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled");
        setEnabled(context, true);
        PlayerWidgetJobService.updateWidget(context);
    }

    @Override
    public void onUpdate(@NonNull Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate() called with: " + "context = [" + context + "], appWidgetManager = [" + appWidgetManager + "], appWidgetIds = [" + Arrays.toString(appWidgetIds) + "]");
        PlayerWidgetJobService.updateWidget(context);
    }

    @Override
    public void onDisabled(@NonNull Context context) {
        super.onDisabled(context);
        Log.d(TAG, "Widget disabled");
        setEnabled(context, false);
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
