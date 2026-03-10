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
     * Otherwise returns currentPosition unchanged.
     */
    public static long skipIntroIfNecessary(Context context, FeedMedia media, long currentPosition) {
        long startPosition = skipIntroPosition(media, currentPosition);
        if (startPosition != currentPosition) {
            Log.d(TAG, "skipIntro " + media.getEpisodeTitle());
            EventBus.getDefault().post(
                    new MessageEvent(context.getString(R.string.pref_feed_skip_intro_toast, (int) (startPosition / 1000))));
        }
        return startPosition;
    }

    private static long skipIntroPosition(FeedMedia media, long currentPosition) {
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
     * Logs the skip event, posts an EventBus message, and returns true if the ending should be
     * skipped given the current position, duration and playback speed.
     */
    public static boolean skipEndingIfNecessary(Context context, FeedMedia media,
                                                long position, long duration, float speed) {
        int skipEnd = skipEndingSeconds(media, position, duration, speed);
        if (skipEnd > 0) {
            Log.d(TAG, "skipEndingIfNecessary: Skipping remaining " + (duration - position));
            EventBus.getDefault().post(
                    new MessageEvent(context.getString(R.string.pref_feed_skip_ending_toast, skipEnd)));
            return true;
        }
        return false;
    }

    /**
     * Returns the number of seconds to skip at the end of the episode, or 0 if no ending should
     * be skipped given the current position, duration and playback speed.
     */
    private static int skipEndingSeconds(FeedMedia media, long position, long duration, float speed) {
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
