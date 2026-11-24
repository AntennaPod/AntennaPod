package de.danoeh.antennapod.playback.service.internal;

import java.util.Collections;
import java.util.Date;

import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

/**
 * Provides utility methods for FeedMedia objects.
 */
public abstract class PlayableUtils {
    /**
     * Saves the current position of this object.
     *
     * @param newPosition  new playback position in ms
     * @param timestamp  current time in ms
     */
    public static void saveCurrentPosition(FeedMedia media, int newPosition, long timestamp) {
        media.setPosition(newPosition);
        media.setLastPlayedTimeStatistics(timestamp);
        media.setLastPlayedTimeHistory(new Date(timestamp));
        FeedItem item = media.getItem();
        if (item != null && item.isNew()) {
            DBWriter.markItemsPlayed(FeedItem.UNPLAYED, false, Collections.singletonList(item));
        }
        DBWriter.setFeedMediaPlaybackInformation(media);
    }
}
