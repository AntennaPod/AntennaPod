package de.danoeh.antennapod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DownloadRequester;

// modified from http://developer.android.com/training/monitoring-device-state/battery-monitoring.html
// and ConnectivityActionReceiver.java
public class PowerConnectionReceiver extends BroadcastReceiver {
	private static final String TAG = "PowerConnectionReceiver";

	@Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        if (isCharging) {
            Log.d(TAG, "charging, starting auto-download");
            // we're plugged in, this is a great time to auto-download if everything else is
            // right. So, even if the user allows auto-dl on battery, let's still start
            // downloading now. They shouldn't mind.
            // autodownloadUndownloadedItems will make sure we're on the right wifi networks,
            // etc... so we don't have to worry about it.
            DBTasks.autodownloadUndownloadedItems(context);
        } else {
            // if we're not supposed to be auto-downloading when we're not charging, stop it
            if (!UserPreferences.isEnableAutodownloadOnBattery()) {
                Log.d(TAG, "not charging anymore, canceling auto-download");
                DownloadRequester.getInstance().cancelAllDownloads(context);
            } else {
                Log.d(TAG, "not charging anymore, but the user allows auto-download " +
                           "when on battery so we'll keep going");
            }
        }

    }
}
