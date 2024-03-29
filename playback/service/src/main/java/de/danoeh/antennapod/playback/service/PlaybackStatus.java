package de.danoeh.antennapod.playback.service;

import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.model.feed.FeedMedia;

public abstract class PlaybackStatus {
    /**
     * Reads playback preferences to determine whether this FeedMedia object is
     * currently being played and the current player status is playing.
     */
    public static boolean isCurrentlyPlaying(FeedMedia media) {
        return isPlaying(media) && PlaybackService.isRunning
                && ((PlaybackPreferences.getCurrentPlayerStatus() == PlaybackPreferences.PLAYER_STATUS_PLAYING));
    }

    public static boolean isPlaying(FeedMedia media) {
        return PlaybackPreferences.getCurrentlyPlayingMediaType() == FeedMedia.PLAYABLE_TYPE_FEEDMEDIA
                && media != null
                && PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == media.getId();
    }
}
