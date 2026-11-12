package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.content.Intent;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.VideoPlayerActivityStarter;

public abstract class PlaybackServiceInterface {
    public static final String EXTRA_PLAYABLE = "PlaybackService.PlayableExtra";
    public static final String EXTRA_ALLOW_STREAM_THIS_TIME = "extra.de.danoeh.antennapod.core.service.allowStream";
    public static final String EXTRA_ALLOW_STREAM_ALWAYS = "extra.de.danoeh.antennapod.core.service.allowStreamAlways";

    public static final String ACTION_PLAYER_NOTIFICATION
            = "action.de.danoeh.antennapod.core.service.playerNotification";
    public static final String EXTRA_NOTIFICATION_CODE = "extra.de.danoeh.antennapod.core.service.notificationCode";
    public static final String EXTRA_NOTIFICATION_TYPE = "extra.de.danoeh.antennapod.core.service.notificationType";
    public static final int NOTIFICATION_TYPE_PLAYBACK_END = 7;
    public static final int NOTIFICATION_TYPE_RELOAD = 3;
    public static final int EXTRA_CODE_AUDIO = 1; // Used in NOTIFICATION_TYPE_RELOAD
    public static final int EXTRA_CODE_VIDEO = 2;
    public static final int EXTRA_CODE_CAST = 3;

    public static final String ACTION_SHUTDOWN_PLAYBACK_SERVICE
            = "action.de.danoeh.antennapod.core.service.actionShutdownPlaybackService";

    /**
     * Is true if playback is active or about to start.
     */
    public static boolean isPlaying = false;

    public static Intent getPlayerActivityIntent(Context context, Playable media) {
        if (media.getMediaType() == MediaType.VIDEO) {
            return new VideoPlayerActivityStarter(context).getIntent();
        } else {
            return new MainActivityStarter(context).withClearBackStack().withOpenPlayer().getIntent();
        }
    }
}
