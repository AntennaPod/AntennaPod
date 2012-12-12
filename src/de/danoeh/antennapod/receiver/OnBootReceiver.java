package de.danoeh.antennapod.receiver;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.PodcastApp;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {
	private static final String TAG = "OnBootReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Received!");
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(
						context.getApplicationContext());
		
		int hours = Integer.parseInt(prefs.getString(
				PodcastApp.PREF_UPDATE_INTERVALL, "0"));
		PendingIntent updateIntent = PendingIntent.getBroadcast(context, 0,
				new Intent(FeedUpdateReceiver.ACTION_REFRESH_FEEDS), 0);
		alarmManager.cancel(updateIntent);
		if (hours != 0) {
			long newIntervall = TimeUnit.HOURS.toMillis(hours);
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
					newIntervall, newIntervall, updateIntent);
			if (AppConfig.DEBUG)
				Log.d(TAG, "Started for "+hours+" hour(s).");
		}
	}

}
