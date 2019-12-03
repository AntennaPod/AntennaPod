package de.danoeh.antennapodSA.core.feed.util;

import de.danoeh.antennapodSA.core.feed.Feed;
import de.danoeh.antennapodSA.core.feed.FeedItem;
import de.danoeh.antennapodSA.core.feed.FeedMedia;
import de.danoeh.antennapodSA.core.feed.MediaType;
import de.danoeh.antennapodSA.core.preferences.PlaybackPreferences;
import de.danoeh.antennapodSA.core.preferences.UserPreferences;
import de.danoeh.antennapodSA.core.util.playback.Playable;

import static de.danoeh.antennapodSA.core.feed.FeedPreferences.SPEED_USE_GLOBAL;

/**
 * Utility class to use the appropriate playback speed based on {@link PlaybackPreferences}
 */
public final class PlaybackSpeedUtils {

    private PlaybackSpeedUtils() {
    }

    /**
     * Returns the currently configured playback speed for the specified media.
     */
    public static float getCurrentPlaybackSpeed(Playable media) {
        float playbackSpeed = SPEED_USE_GLOBAL;
        MediaType mediaType = null;

        if (media != null) {
            mediaType = media.getMediaType();
            playbackSpeed = PlaybackPreferences.getCurrentlyPlayingTemporaryPlaybackSpeed();

            if (playbackSpeed == SPEED_USE_GLOBAL && media instanceof FeedMedia) {
                FeedItem item = ((FeedMedia) media).getItem();
                if (item != null) {
                    Feed feed = item.getFeed();
                    if (feed != null) {
                        playbackSpeed = feed.getPreferences().getFeedPlaybackSpeed();
                    }
                }
            }
        }

        if (playbackSpeed == SPEED_USE_GLOBAL) {
            playbackSpeed = UserPreferences.getPlaybackSpeed(mediaType);
        }

        return playbackSpeed;
    }
}
