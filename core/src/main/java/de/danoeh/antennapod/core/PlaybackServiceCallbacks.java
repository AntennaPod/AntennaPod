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
     * @param remotePlayback true if the media is played on a remote device.
     * @return A non-null activity intent.
     */
    Intent getPlayerActivityIntent(Context context, MediaType mediaType, boolean remotePlayback);

    /**
     * Returns true if the PlaybackService should load new episodes from the queue when playback ends
     * and false if the PlaybackService should ignore the queue and load no more episodes when playback
     * finishes.
     */
    boolean useQueue();

    /**
     * Returns a drawable resource that is used for the notification of the playback service.
     */
    int getNotificationIconResource(Context context);
}
