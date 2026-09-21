package de.danoeh.antennapod.playback.service.internal;

import java.util.Collections;
import java.util.Date;

import de.danoeh.antennapod.storage.database.DBWriter;
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
        FeedItem item = playable instanceof FeedMedia ? ((FeedMedia) playable).getItem() : null;
        boolean itemWasNew = item != null && item.isNew();
        playable.setPosition(newPosition);
        playable.setLastPlayedTimeStatistics(timestamp);

        if (playable instanceof FeedMedia) {
            FeedMedia media = (FeedMedia) playable;
            media.setLastPlayedTimeHistory(new Date(timestamp));
            if (itemWasNew) {
                DBWriter.markItemsPlayed(FeedItem.UNPLAYED, false, Collections.singletonList(item));
            }
            if (media.getStartPosition() >= 0 && playable.getPosition() > media.getStartPosition()) {
                media.setPlayedDuration(media.getPlayedDurationWhenStarted()
                        + playable.getPosition() - media.getStartPosition());
            }
            DBWriter.setFeedMediaPlaybackInformation(media);
        }
    }
}
