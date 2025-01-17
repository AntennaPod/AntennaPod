package de.danoeh.antennapod.ui.episodes;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.model.playback.Playable;

/**
 * Utility class to use the appropriate playback speed based on {@link PlaybackPreferences}
 */
public abstract class PlaybackSpeedUtils {
    /**
     * Returns the currently configured playback speed for the specified media.
     */
    public static float getCurrentPlaybackSpeed(Playable media) {
        float playbackSpeed = FeedPreferences.SPEED_USE_GLOBAL;
        if (media instanceof FeedMedia) {
            FeedMedia feedMedia = (FeedMedia) media;
            if (PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == feedMedia.getId()) {
                playbackSpeed = PlaybackPreferences.getCurrentlyPlayingTemporaryPlaybackSpeed();
            }
            if (playbackSpeed == FeedPreferences.SPEED_USE_GLOBAL && feedMedia.getItem() != null) {
                Feed feed = feedMedia.getItem().getFeed();
                if (feed != null && feed.getPreferences() != null) {
                    playbackSpeed = feed.getPreferences().getFeedPlaybackSpeed();
                }
            }
        }
        if (playbackSpeed == FeedPreferences.SPEED_USE_GLOBAL) {
            playbackSpeed = UserPreferences.getPlaybackSpeed();
        }
        return playbackSpeed;
    }

    /**
     * Returns the currently configured skip silence for the specified media.
     */
    public static FeedPreferences.SkipSilence getCurrentSkipSilencePreference(Playable media) {
        FeedPreferences.SkipSilence skipSilence = FeedPreferences.SkipSilence.GLOBAL;
        if (media instanceof FeedMedia) {
            FeedMedia feedMedia = (FeedMedia) media;
            if (PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == feedMedia.getId()) {
                skipSilence = PlaybackPreferences.getCurrentlyPlayingTemporarySkipSilence();
            }
            if (skipSilence == FeedPreferences.SkipSilence.GLOBAL && feedMedia.getItem() != null) {
                Feed feed = feedMedia.getItem().getFeed();
                if (feed != null && feed.getPreferences() != null) {
                    skipSilence = feed.getPreferences().getFeedSkipSilence();
                }
            }
        }
        if (skipSilence == FeedPreferences.SkipSilence.GLOBAL) {
            skipSilence = UserPreferences.getSkipSilence();
        }
        return skipSilence;
    }
}
