package de.danoeh.antennapod.core.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;

import de.danoeh.antennapod.core.ClientConfigurator;

/**
 * Receives media button events.
 */
public class MediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaButtonReceiver";
    public static final String EXTRA_KEYCODE = "de.danoeh.antennapod.core.service.extra.MediaButtonReceiver.KEYCODE";
    public static final String EXTRA_SOURCE = "de.danoeh.antennapod.core.service.extra.MediaButtonReceiver.SOURCE";
    public static final String EXTRA_HARDWAREBUTTON
            = "de.danoeh.antennapod.core.service.extra.MediaButtonReceiver.HARDWAREBUTTON";
    public static final String NOTIFY_BUTTON_RECEIVER = "de.danoeh.antennapod.NOTIFY_BUTTON_RECEIVER";
    public static final String PLAYBACK_SERVICE_INTENT = "de.danoeh.antennapod.intents.PLAYBACK_SERVICE";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received intent");
        if (intent == null || intent.getExtras() == null) {
            return;
        }
        KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            ClientConfigurator.initialize(context);
            Intent serviceIntent = new Intent(PLAYBACK_SERVICE_INTENT);
            serviceIntent.setPackage(context.getPackageName());
            serviceIntent.putExtra(EXTRA_KEYCODE, event.getKeyCode());
            serviceIntent.putExtra(EXTRA_SOURCE, event.getSource());
            serviceIntent.putExtra(EXTRA_HARDWAREBUTTON, event.getEventTime() > 0 || event.getDownTime() > 0);
            try {
                ContextCompat.startForegroundService(context, serviceIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Intent createIntent(Context context, int eventCode) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, eventCode);
        Intent startingIntent = new Intent(context, MediaButtonReceiver.class);
        startingIntent.setAction(MediaButtonReceiver.NOTIFY_BUTTON_RECEIVER);
        startingIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);
        return startingIntent;
    }

    public static PendingIntent createPendingIntent(Context context, int eventCode) {
        return PendingIntent.getBroadcast(context, eventCode, createIntent(context, eventCode),
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }
}
