package de.danoeh.antennapod.ui.appstartintent;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public abstract class MediaButtonStarter {
    private static final String INTENT = "de.danoeh.antennapod.NOTIFY_BUTTON_RECEIVER";
    private static final String MEDIA3_PLAYBACK_SERVICE =
            "de.danoeh.antennapod.playback.service.Media3PlaybackService";
    public static final String EXTRA_MEDIA_BUTTON_SOURCE = "media_button_source";
    public static final String MEDIA_BUTTON_SOURCE_WIDGET = "widget";

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
            Intent intent = createIntent(context, eventCode)
                    .setComponent(new ComponentName(context, MEDIA3_PLAYBACK_SERVICE))
                    .putExtra(EXTRA_MEDIA_BUTTON_SOURCE, MEDIA_BUTTON_SOURCE_WIDGET);
            return PendingIntent.getService(context, eventCode, intent, PendingIntent.FLAG_IMMUTABLE);
        }
        return PendingIntent.getBroadcast(context, eventCode, createIntent(context, eventCode),
                PendingIntent.FLAG_IMMUTABLE);
    }
}
