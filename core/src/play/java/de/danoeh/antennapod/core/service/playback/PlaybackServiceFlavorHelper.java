package de.danoeh.antennapod.core.service.playback;

/**
 * Class intended to work along PlaybackService and provide support for different flavors.
 */
public class PlaybackServiceFlavorHelper {
    public static final String TAG = "PlaybackSrvFlavorHelper";

    /*
    void sessionStateAddActionForWear(PlaybackStateCompat.Builder sessionState, String actionName,
              CharSequence name, int icon) {
        if (!CastManager.isInitialized()) {
            return;
        }
        PlaybackStateCompat.CustomAction.Builder actionBuilder =
            new PlaybackStateCompat.CustomAction.Builder(actionName, name, icon);
        Bundle actionExtras = new Bundle();
        actionExtras.putBoolean(MediaControlConstants.EXTRA_CUSTOM_ACTION_SHOW_ON_WEAR, true);
        actionBuilder.setExtras(actionExtras);

        sessionState.addCustomAction(actionBuilder.build());
    }

    void mediaSessionSetExtraForWear(MediaSessionCompat mediaSession) {
        if (!CastManager.isInitialized()) {
            return;
        }
        Bundle sessionExtras = new Bundle();
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_PREVIOUS, true);
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_NEXT, true);
        mediaSession.setExtras(sessionExtras);
    }
    */
}
