package de.danoeh.antennapod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.preferences.UserPreferences;

/** Listens for events that make it necessary to reset the update alarm. */
public class AlarmUpdateReceiver extends BroadcastReceiver {
	private static final String TAG = "AlarmUpdateReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Received intent");
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Resetting update alarm after reboot");
		} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Resetting update alarm after app upgrade");
		}

		UserPreferences.restartUpdateAlarm(UserPreferences.getUpdateInterval());

	}

}
