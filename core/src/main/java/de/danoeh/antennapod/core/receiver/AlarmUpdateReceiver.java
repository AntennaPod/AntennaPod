package de.danoeh.antennapod.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/** Listens for events that make it necessary to reset the update alarm. */
public class AlarmUpdateReceiver extends BroadcastReceiver {

	private static final String TAG = "AlarmUpdateReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Received intent");
		if (StringUtils.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
			Log.d(TAG, "Resetting update alarm after reboot");
		} else if (StringUtils.equals(intent.getAction(), Intent.ACTION_PACKAGE_REPLACED)) {
			Log.d(TAG, "Resetting update alarm after app upgrade");
		}
        PlaybackPreferences.init(context);
        UserPreferences.init(context);
        UserPreferences.restartUpdateAlarm(false);
	}

}
