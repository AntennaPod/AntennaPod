package de.danoeh.antennapod.ui.episodes;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Utility class to use the appropriate playback speed based on {@link PlaybackPreferences}
 */
public abstract class PlaybackSpeedUtils {
    /**
     * Returns the currently configured playback speed for the specified media.
     */
    public static float getCurrentPlaybackSpeed(FeedMedia media) {
        float playbackSpeed = FeedPreferences.SPEED_USE_GLOBAL;
        if (media != null) {
            if (PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == media.getId()) {
                playbackSpeed = PlaybackPreferences.getCurrentlyPlayingTemporaryPlaybackSpeed();
            }
            if (playbackSpeed == FeedPreferences.SPEED_USE_GLOBAL && media.getItem() != null) {
                Feed feed = media.getItem().getFeed();
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
    public static FeedPreferences.SkipSilence getCurrentSkipSilencePreference(FeedMedia media) {
        FeedPreferences.SkipSilence skipSilence = FeedPreferences.SkipSilence.GLOBAL;
        if (media != null) {
            if (PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == media.getId()) {
                skipSilence = PlaybackPreferences.getCurrentlyPlayingTemporarySkipSilence();
            }
            if (skipSilence == FeedPreferences.SkipSilence.GLOBAL && media.getItem() != null) {
                Feed feed = media.getItem().getFeed();
                if (feed != null && feed.getPreferences() != null) {
                    skipSilence = feed.getPreferences().getFeedSkipSilence();
                }
            }
        }
        if (skipSilence == FeedPreferences.SkipSilence.GLOBAL) {
            skipSilence = UserPreferences.isSkipSilence()
                    ? FeedPreferences.SkipSilence.AGGRESSIVE : FeedPreferences.SkipSilence.OFF;
        }
        return skipSilence;
    }
}
