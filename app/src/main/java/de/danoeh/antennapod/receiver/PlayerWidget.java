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

    @Override
	public void onReceive(Context context, Intent intent) {
		if (StringUtils.equals(intent.getAction(), PlaybackService.FORCE_WIDGET_UPDATE)) {
			startUpdate(context);
		} else if (StringUtils.equals(intent.getAction(), PlaybackService.STOP_WIDGET_UPDATE)) {
			stopUpdate(context);
		}

	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Widget enabled");
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		startUpdate(context);
	}

	private void startUpdate(Context context) {
		context.startService(new Intent(context, PlayerWidgetService.class));
	}

	private void stopUpdate(Context context) {
		context.stopService(new Intent(context, PlayerWidgetService.class));
	}

}
