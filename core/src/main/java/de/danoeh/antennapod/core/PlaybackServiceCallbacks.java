package de.danoeh.antennapod.core;

import android.content.Context;
import android.content.Intent;

import de.danoeh.antennapod.core.feed.MediaType;

/**
 * Callbacks for the PlaybackService of the core module
 */
public interface PlaybackServiceCallbacks {

    /**
     * Returns an intent which starts an audio- or videoplayer, depending on the
     * type of media that is being played.
     *
     * @param mediaType The type of media that is being played.
     * @return A non-null activity intent.
     */
    public Intent getPlayerActivityIntent(Context context, MediaType mediaType);

    /**
     * Returns true if the PlaybackService should load new episodes from the queue when playback ends
     * and false if the PlaybackService should ignore the queue and load no more episodes when playback
     * finishes.
     */
    public boolean useQueue();

    /**
     * Returns a drawable resource that is used for the notification of the playback service.
     */
    public int getNotificationIconResource(Context context);
}
