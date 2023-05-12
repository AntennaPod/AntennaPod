package de.danoeh.antennapod.playback.wear;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

class WearMediaSession {
    /**
     * Take a custom action builder and add no extras, because this is not the Play version of the app.
     */
    public static void addWearExtrasToAction(PlaybackStateCompat.CustomAction.Builder actionBuilder) {
        // no-op
    }

    public static void mediaSessionSetExtraForWear(MediaSessionCompat mediaSession) {
        // no-op
    }
}
