package de.danoeh.antennapod.playback.wear;

import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.wearable.media.MediaControlConstants;

public class WearMediaSession {
    /**
     * Take a custom action builder and make sure the custom action shows on Wear OS because this is the Play version
     * of the app.
     */
    public static void addWearExtrasToAction(PlaybackStateCompat.CustomAction.Builder actionBuilder) {
        Bundle actionExtras = new Bundle();
        actionExtras.putBoolean(MediaControlConstants.EXTRA_CUSTOM_ACTION_SHOW_ON_WEAR, true);
        actionBuilder.setExtras(actionExtras);
    }

    public static void mediaSessionSetExtraForWear(MediaSessionCompat mediaSession) {
        Bundle sessionExtras = new Bundle();
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_PREVIOUS, false);
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_NEXT, false);
        mediaSession.setExtras(sessionExtras);
    }
}
