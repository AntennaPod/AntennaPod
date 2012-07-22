package de.danoeh.antennapod.receiver;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.service.PlayerWidgetService;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PlayerWidget extends AppWidgetProvider {
	private static final String TAG = "PlayerWidget";
	public static final String FORCE_WIDGET_UPDATE = "de.danoeh.antennapod.FORCE_WIDGET_UPDATE";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(FORCE_WIDGET_UPDATE)) {
			startUpdate(context);
		}

	}
	
	

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		if (AppConfig.DEBUG) Log.d(TAG, "Widget enabled");
	}



	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		startUpdate(context);
	}

	private void startUpdate(Context context) {
		context.startService(new Intent(context, PlayerWidgetService.class));
	}

}
