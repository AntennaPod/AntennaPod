package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.content.Intent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;

public abstract class PlaybackServiceInterface {
    /**
     * Is true if playback is active or about to start.
     */
    public static boolean isPlaying = false;

    public static Intent getPlayerActivityIntent(Context context, FeedMedia media) {
        return new MainActivityStarter(context).withClearBackStack().withOpenPlayer().getIntent();
    }
}
