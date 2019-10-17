package de.danoeh.antennapod.core.preferences;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.util.playback.Playable;

import static de.danoeh.antennapod.core.feed.FeedPreferences.SPEED_USE_GLOBAL;

public class PlaybackSpeedHelper {

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
