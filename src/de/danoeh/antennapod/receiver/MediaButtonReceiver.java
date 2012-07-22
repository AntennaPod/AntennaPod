package de.danoeh.antennapod.receiver;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.service.PlaybackService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

/** Receives media button events. */
public class MediaButtonReceiver extends BroadcastReceiver {
	private static final String TAG = "MediaButtonReceiver";
	public static final String EXTRA_KEYCODE = "de.danoeh.antennapod.service.extra.MediaButtonReceiver.KEYCODE";

	public static final String NOTIFY_BUTTON_RECEIVER = "de.danoeh.antennapod.NOTIFY_BUTTON_RECEIVER";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (BuildConfig.DEBUG) Log.d(TAG, "Received intent");
		KeyEvent event = (KeyEvent) intent.getExtras().get(
				Intent.EXTRA_KEY_EVENT);
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			Intent serviceIntent = new Intent(context, PlaybackService.class);
			int keycode = event.getKeyCode();
			serviceIntent.putExtra(EXTRA_KEYCODE, keycode);
			context.startService(serviceIntent);
		}

	}

}
