package de.danoeh.antennapod.ui.appstartintent;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;

public abstract class MediaButtonStarter {
    private static final String INTENT = "de.danoeh.antennapod.NOTIFY_BUTTON_RECEIVER";
    private static final String MEDIA3_PLAYBACK_SERVICE =
            "de.danoeh.antennapod.playback.service.Media3PlaybackService";

    public static Intent createIntent(Context context, int eventCode) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, eventCode);
        Intent startingIntent = new Intent(BuildConfig.USE_MEDIA3_PLAYBACK_SERVICE
                ? Intent.ACTION_MEDIA_BUTTON : INTENT);
        startingIntent.setPackage(context.getPackageName());
        startingIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);
        return startingIntent;
    }

    public static PendingIntent createPendingIntent(Context context, int eventCode) {
        if (BuildConfig.USE_MEDIA3_PLAYBACK_SERVICE) {
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, eventCode);
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON)
                    .setComponent(new ComponentName(context, MEDIA3_PLAYBACK_SERVICE))
                    .putExtra(Intent.EXTRA_KEY_EVENT, event);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return PendingIntent.getForegroundService(context, eventCode, intent,
                        PendingIntent.FLAG_IMMUTABLE);
            }
            return PendingIntent.getService(context, eventCode, intent, PendingIntent.FLAG_IMMUTABLE);
        }
        return PendingIntent.getBroadcast(context, eventCode, createIntent(context, eventCode),
                PendingIntent.FLAG_IMMUTABLE);
    }
}
