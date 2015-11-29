package de.danoeh.antennapod.receiver;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.service.PlayerWidgetService;

public class PlayerWidget extends AppWidgetProvider {
    private static final String TAG = "PlayerWidget";

    // static because there should only ever be one widget...
    // and otherwise it just keeps getting reset when it gets messages
    private static boolean enabled = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        super.onReceive(context, intent);
        if (!enabled) {
            // do nothing
            return;
        }

        // these come from the PlaybackService when things should get updated
        if (StringUtils.equals(intent.getAction(), PlaybackService.FORCE_WIDGET_UPDATE)) {
            startUpdate(context);
        } else if (StringUtils.equals(intent.getAction(), PlaybackService.STOP_WIDGET_UPDATE)) {
            stopUpdate(context);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled");
        enabled = true;
        startUpdate(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        Log.d(TAG, "onUpdate() called with: " + "context = [" + context + "], appWidgetManager = [" + appWidgetManager + "], appWidgetIds = [" + appWidgetIds + "]");
        startUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "Widet disabled");
        enabled = false;
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
}
