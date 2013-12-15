package de.danoeh.antennapod.receiver;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.service.playback.PlayerWidgetService;

public class PlayerWidget extends AppWidgetProvider {
	private static final String TAG = "PlayerWidget";
	public static final String FORCE_WIDGET_UPDATE = "de.danoeh.antennapod.FORCE_WIDGET_UPDATE";
	public static final String STOP_WIDGET_UPDATE = "de.danoeh.antennapod.STOP_WIDGET_UPDATE";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(FORCE_WIDGET_UPDATE)) {
			startUpdate(context);
		} else if (intent.getAction().equals(STOP_WIDGET_UPDATE)) {
			stopUpdate(context);
		}

	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		if (AppConfig.DEBUG)
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
