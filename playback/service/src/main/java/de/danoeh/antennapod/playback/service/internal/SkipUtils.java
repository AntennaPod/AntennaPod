package de.danoeh.antennapod.playback.service.internal;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;

public final class SkipUtils {
    private SkipUtils() {
    }

    /**
     * Returns the position to start playback at, taking into account skip intro.
     * If the intro should be skipped, returns the skip intro position.
     * Otherwise returns currentPosition unchanged.
     */
    public static long skipIntroPosition(FeedMedia media, long currentPosition) {
        if (media.getItem() == null || media.getItem().getFeed() == null
                || media.getItem().getFeed().getPreferences() == null) {
            return currentPosition;
        }
        int skipIntro = media.getItem().getFeed().getPreferences().getFeedSkipIntro();
        long duration = media.getDuration();
        if (skipIntro > 0 && currentPosition < skipIntro * 1000L
                && (skipIntro * 1000L < duration || duration <= 0)) {
            return skipIntro * 1000L;
        }
        return currentPosition;
    }

    /**
     * Returns the number of seconds to skip at the end of the episode, or 0 if no ending should
     * be skipped given the current position, duration and playback speed.
     */
    public static int skipEndingSeconds(FeedMedia media, long position, long duration, float speed) {
        if (media.getItem() == null || media.getItem().getFeed() == null
                || media.getItem().getFeed().getPreferences() == null) {
            return 0;
        }
        FeedPreferences preferences = media.getItem().getFeed().getPreferences();
        int skipEnd = preferences.getFeedSkipEnding();
        long remainingTime = duration - position;
        if (skipEnd > 0
                && skipEnd * 1000L < duration
                && (remainingTime - (skipEnd * 1000L) > 0)
                && ((remainingTime - skipEnd * 1000L) < (speed * 1000))) {
            return skipEnd;
        }
        return 0;
    }
}
