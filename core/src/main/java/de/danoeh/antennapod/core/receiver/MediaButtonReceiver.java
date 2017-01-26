package de.danoeh.antennapod.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.service.playback.PlaybackService;

/** Receives media button events. */
public class MediaButtonReceiver extends BroadcastReceiver {
	private static final String TAG = "MediaButtonReceiver";

	public static final String NOTIFY_BUTTON_RECEIVER = "de.danoeh.antennapod.NOTIFY_BUTTON_RECEIVER";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Received intent");
		KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
		if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount()==0) {
			ClientConfig.initialize(context);
			Intent serviceIntent = new Intent(context, PlaybackService.class);
			serviceIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);
			serviceIntent.setAction(Intent.ACTION_MEDIA_BUTTON);
			context.startService(serviceIntent);
		}

	}

}
