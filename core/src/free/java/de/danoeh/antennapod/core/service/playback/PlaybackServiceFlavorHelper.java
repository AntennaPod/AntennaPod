package de.danoeh.antennapod.core.service.playback;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

class WearMediaSession {
    static void sessionStateAddActionForWear(PlaybackStateCompat.Builder sessionState, String actionName,
                                      CharSequence name, int icon) {
        // no-op
    }

    static void mediaSessionSetExtraForWear(MediaSessionCompat mediaSession) {
        // no-op
    }
}
