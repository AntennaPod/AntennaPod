package de.danoeh.antennapod.core.util.playback;

import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;

/**
 * Provides utility methods for Playable objects.
 */
public abstract class PlayableUtils {
    /**
     * Saves the current position of this object.
     *
     * @param newPosition  new playback position in ms
     * @param timestamp  current time in ms
     */
    public static void saveCurrentPosition(Playable playable, int newPosition, long timestamp) {
        playable.setPosition(newPosition);
        playable.setLastPlayedTime(timestamp);

        if (playable instanceof FeedMedia) {
            FeedMedia media = (FeedMedia) playable;
            FeedItem item = media.getItem();
            if (item != null && item.isNew()) {
                DBWriter.markItemPlayed(FeedItem.UNPLAYED, item.getId());
            }
            if (media.getStartPosition() >= 0 && playable.getPosition() > media.getStartPosition()) {
                media.setPlayedDuration(media.getPlayedDurationWhenStarted()
                        + playable.getPosition() - media.getStartPosition());
            }
            DBWriter.setFeedMediaPlaybackInformation(media);
        }
    }
}
