package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.util.Log;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.playback.service.R;
import org.greenrobot.eventbus.EventBus;

public final class SkipUtils {
    private static final String TAG = "SkipUtils";

    private SkipUtils() {
    }

    /**
     * Logs the skip event, posts an EventBus message, and returns the position to start playback at,
     * taking into account skip intro. If the intro should be skipped, returns the skip intro position.
     * Otherwise returns the media's saved position unchanged.
     */
    public static long skipIntroIfNecessary(Context context, FeedMedia media) {
        return skipIntroIfNecessary(context, media, 0);
    }

    /**
     * Logs the skip event, posts an EventBus message, and returns the position to start playback at,
     * taking into account skip intro. Uses media's saved position if > 0, otherwise falls back to
     * startPositionMs. If the intro should be skipped, returns the skip intro position.
     */
    public static long skipIntroIfNecessary(Context context, FeedMedia media, long startPositionMs) {
        long currentPosition = media.getPosition() > 0 ? media.getPosition()
                : (startPositionMs > 0 ? startPositionMs : 0);
        if (media.getItem() == null || media.getItem().getFeed() == null
                || media.getItem().getFeed().getPreferences() == null) {
            return currentPosition;
        }
        int skipIntro = media.getItem().getFeed().getPreferences().getFeedSkipIntro();
        long duration = media.getDuration();
        long startPosition = currentPosition;
        if (skipIntro > 0 && currentPosition < skipIntro * 1000L
                && (skipIntro * 1000L < duration || duration <= 0)) {
            startPosition = skipIntro * 1000L;
        }
        if (startPosition != currentPosition) {
            Log.d(TAG, "skipIntro " + media.getEpisodeTitle());
            EventBus.getDefault().post(
                    new MessageEvent(context.getString(R.string.pref_feed_skip_intro_toast, (int) (startPosition / 1000))));
        }
        return startPosition;
    }

    /**
     * Logs the skip event, posts an EventBus message, and returns true if the ending should be
     * skipped given the current position, duration and playback speed.
     */
    public static boolean skipEndingIfNecessary(Context context, FeedMedia media,
                                                long position, long duration, float speed) {
        if (media.getItem() == null || media.getItem().getFeed() == null
                || media.getItem().getFeed().getPreferences() == null) {
            return false;
        }
        FeedPreferences preferences = media.getItem().getFeed().getPreferences();
        int skipEnd = preferences.getFeedSkipEnding();
        long remainingTime = duration - position;
        if (skipEnd > 0
                && skipEnd * 1000L < duration
                && (remainingTime - (skipEnd * 1000L) > 0)
                && ((remainingTime - skipEnd * 1000L) < (speed * 1000))) {
            Log.d(TAG, "skipEndingIfNecessary: Skipping remaining " + (duration - position));
            EventBus.getDefault().post(
                    new MessageEvent(context.getString(R.string.pref_feed_skip_ending_toast, skipEnd)));
            return true;
        }
        return false;
    }
}
