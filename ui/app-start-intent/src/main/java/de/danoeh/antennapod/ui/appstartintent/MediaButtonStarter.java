package de.danoeh.antennapod.ui.appstartintent;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.OptIn;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.PlaybackPendingIntentBuilder;

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

    @OptIn(markerClass = UnstableApi.class)
    public static PendingIntent createPendingIntent(Context context, @Player.Command int command) {
        Bundle extras = new Bundle();
        extras.putString(EXTRA_MEDIA_BUTTON_SOURCE, MEDIA_BUTTON_SOURCE_WIDGET);
        return new PlaybackPendingIntentBuilder(context, command, getMedia3ServiceClass())
                .setStartAsForegroundService(command == Player.COMMAND_PLAY_PAUSE)
                .setExtras(extras)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends MediaSessionService> getMedia3ServiceClass() {
        try {
            return (Class<? extends MediaSessionService>) Class.forName(MEDIA3_PLAYBACK_SERVICE);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
