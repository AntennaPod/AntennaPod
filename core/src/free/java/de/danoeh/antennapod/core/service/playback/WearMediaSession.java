package de.danoeh.antennapod.core.service.playback;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

class WearMediaSession {
    /**
     * Take a custom action builder and add no extras, because this is not the Play version of the app.
     */
    static void addWearExtrasToAction(PlaybackStateCompat.CustomAction.Builder actionBuilder) {
        // no-op
    }

    static void mediaSessionSetExtraForWear(MediaSessionCompat mediaSession) {
        // no-op
    }
}
