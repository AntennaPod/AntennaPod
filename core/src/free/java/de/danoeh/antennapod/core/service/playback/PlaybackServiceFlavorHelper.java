package de.danoeh.antennapod.core.service.playback;

import android.content.Context;
import androidx.annotation.StringRes;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

/**
 * Class intended to work along PlaybackService and provide support for different flavors.
 */
class PlaybackServiceFlavorHelper {

    private final PlaybackService.FlavorHelperCallback callback;

    PlaybackServiceFlavorHelper(Context context, PlaybackService.FlavorHelperCallback callback) {
        this.callback = callback;
    }

    void initializeMediaPlayer(Context context) {
        callback.setMediaPlayer(new LocalPSMP(context, callback.getMediaPlayerCallback()));
    }

    void removeCastConsumer() {
        // no-op
    }

    boolean castDisconnect(boolean castDisconnect) {
        return false;
    }

    boolean onMediaPlayerInfo(Context context, int code, @StringRes int resourceId) {
        return false;
    }

    void registerWifiBroadcastReceiver() {
        // no-op
    }

    void unregisterWifiBroadcastReceiver() {
        // no-op
    }

    boolean onSharedPreference(String key) {
        return false;
    }

    void sessionStateAddActionForWear(PlaybackStateCompat.Builder sessionState, String actionName, CharSequence name, int icon) {
        // no-op
    }

    void mediaSessionSetExtraForWear(MediaSessionCompat mediaSession) {
        // no-op
    }
}
