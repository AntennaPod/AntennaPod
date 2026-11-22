package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.content.Intent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.VideoPlayerActivityStarter;

public abstract class PlaybackServiceInterface {
    /**
     * Is true if playback is active or about to start.
     */
    public static boolean isPlaying = false;

    public static Intent getPlayerActivityIntent(Context context, FeedMedia media) {
        if (media.getMediaType() == MediaType.VIDEO) {
            return new VideoPlayerActivityStarter(context).getIntent();
        } else {
            return new MainActivityStarter(context).withClearBackStack().withOpenPlayer().getIntent();
        }
    }
}
