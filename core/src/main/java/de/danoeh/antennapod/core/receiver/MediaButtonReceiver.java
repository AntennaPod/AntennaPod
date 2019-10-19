package de.danoeh.antennapod.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;

import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.service.playback.PlaybackService;

/** Receives media button events. */
public class MediaButtonReceiver extends BroadcastReceiver {
	private static final String TAG = "MediaButtonReceiver";
	public static final String EXTRA_KEYCODE = "de.danoeh.antennapod.core.service.extra.MediaButtonReceiver.KEYCODE";
	public static final String EXTRA_SOURCE = "de.danoeh.antennapod.core.service.extra.MediaButtonReceiver.SOURCE";
	public static final String EXTRA_KEY_SOURCE = "de.danoeh.antennapod.core.service.extra.MediaButtonReceiver.KEY_SOURCE";

	public static final String NOTIFY_BUTTON_RECEIVER = "de.danoeh.antennapod.NOTIFY_BUTTON_RECEIVER";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Received intent");
		if (intent == null || intent.getExtras() == null) {
			return;
		}
		KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
		if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount()==0) {
			ClientConfig.initialize(context);
			Intent serviceIntent = new Intent(context, PlaybackService.class);
			serviceIntent.putExtra(EXTRA_KEYCODE, event.getKeyCode());
			serviceIntent.putExtra(EXTRA_SOURCE, event.getSource());
			serviceIntent.putExtra(EXTRA_KEY_SOURCE, Source.fromIntent(intent));

			ContextCompat.startForegroundService(context, serviceIntent);
		}

	}

	public enum Source {
		NOTIFICATION, WIDGET, MEDIA_BUTTON;

		public static Source fromIntent(Intent intent) {
			Object source = intent.getSerializableExtra(EXTRA_KEY_SOURCE);
			if (source instanceof Source) {
				return (Source) source;
			}
			return MEDIA_BUTTON;
		}
	}

}
