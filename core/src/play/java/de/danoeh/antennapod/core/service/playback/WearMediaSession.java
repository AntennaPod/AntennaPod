package de.danoeh.antennapod.core.service.playback;

import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.wearable.media.MediaControlConstants;

public class WearMediaSession {
    static void sessionStateAddActionForWear(PlaybackStateCompat.Builder sessionState, String actionName,
              CharSequence name, int icon) {
        PlaybackStateCompat.CustomAction.Builder actionBuilder =
                new PlaybackStateCompat.CustomAction.Builder(actionName, name, icon);
        Bundle actionExtras = new Bundle();
        actionExtras.putBoolean(MediaControlConstants.EXTRA_CUSTOM_ACTION_SHOW_ON_WEAR, true);
        actionBuilder.setExtras(actionExtras);

        sessionState.addCustomAction(actionBuilder.build());
    }

    static void mediaSessionSetExtraForWear(MediaSessionCompat mediaSession) {
        Bundle sessionExtras = new Bundle();
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_PREVIOUS, true);
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_NEXT, true);
        mediaSession.setExtras(sessionExtras);
    }
}
