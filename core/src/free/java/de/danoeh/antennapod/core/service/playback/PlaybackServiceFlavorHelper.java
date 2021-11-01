package de.danoeh.antennapod.core.service.playback;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

/**
 * Class intended to work along PlaybackService and provide support for different flavors.
 */
class PlaybackServiceFlavorHelper {
    void sessionStateAddActionForWear(PlaybackStateCompat.Builder sessionState, String actionName, CharSequence name, int icon) {
        // no-op
    }

    void mediaSessionSetExtraForWear(MediaSessionCompat mediaSession) {
        // no-op
    }
}
