package de.danoeh.antennapod.receiver;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;

import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.service.PlayerWidgetService;

public class PlayerWidget extends AppWidgetProvider {
    private static final String TAG = "PlayerWidget";
    private static final String PREFS_NAME = "PlayerWidgetPrefs";
    private static final String KEY_ENABLED = "WidgetEnabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        super.onReceive(context, intent);
        // don't do anything if we're not enabled
        if (!isEnabled(context)) {
            return;
        }

        // these come from the PlaybackService when things should get updated
        if (TextUtils.equals(intent.getAction(), PlaybackService.FORCE_WIDGET_UPDATE)) {
            startUpdate(context);
        } else if (TextUtils.equals(intent.getAction(), PlaybackService.STOP_WIDGET_UPDATE)) {
            stopUpdate(context);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled");
        setEnabled(context, true);
        startUpdate(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        Log.d(TAG, "onUpdate() called with: " + "context = [" + context + "], appWidgetManager = [" + appWidgetManager + "], appWidgetIds = [" + Arrays.toString(appWidgetIds) + "]");
        startUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "Widget disabled");
        setEnabled(context, false);
        stopUpdate(context);
    }

    private void startUpdate(Context context) {
        Log.d(TAG, "startUpdate() called with: " + "context = [" + context + "]");
        context.startService(new Intent(context, PlayerWidgetService.class));
    }

    private void stopUpdate(Context context) {
        Log.d(TAG, "stopUpdate() called with: " + "context = [" + context + "]");
        context.stopService(new Intent(context, PlayerWidgetService.class));
    }

    private boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    private void setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
