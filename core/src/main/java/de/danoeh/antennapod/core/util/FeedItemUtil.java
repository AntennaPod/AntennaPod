package de.danoeh.antennapod.core.util;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.preferences.PlaybackPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.model.feed.FeedItem;

public class FeedItemUtil {
    private FeedItemUtil(){}

    public static int indexOfItemWithId(List<FeedItem> items, long id) {
        for(int i=0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            if(item != null && item.getId() == id) {
                return i;
            }
        }
        return -1;
    }

    public static int indexOfItemWithMediaId(List<FeedItem> items, long mediaId) {
        for(int i=0; i < items.size(); i++) {
            FeedItem item = items.get(i);
            if(item != null && item.getMedia() != null && item.getMedia().getId() == mediaId) {
                return i;
            }
        }
        return -1;
    }

    public static long[] getIds(List<FeedItem> items) {
        if(items == null || items.size() == 0) {
            return new long[0];
        }
        long[] result = new long[items.size()];
        for(int i=0; i < items.size(); i++) {
            result[i] = items.get(i).getId();
        }
        return result;
    }

    @NonNull
    public static List<Long> getIdList(List<? extends FeedItem> items) {
        List<Long> result = new ArrayList<>();
        for (FeedItem item : items) {
            result.add(item.getId());
        }
        return result;
    }

    /**
     * Get the link for the feed item for the purpose of Share. It fallbacks to
     * use the feed's link if the named feed item has no link.
     */
    public static String getLinkWithFallback(FeedItem item) {
        if (item == null) {
            return null;
        } else if (StringUtils.isNotBlank(item.getLink())) {
            return item.getLink();
        } else if (StringUtils.isNotBlank(item.getFeed().getLink())) {
            return item.getFeed().getLink();
        }
        return null;
    }

    public static boolean hasAlmostEnded(FeedMedia media) {
        int smartMarkAsPlayedSecs = UserPreferences.getSmartMarkAsPlayedSecs();
        return media.getDuration() > 0 && media.getPosition() >= media.getDuration() - smartMarkAsPlayedSecs * 1000;
    }

    /**
     * Reads playback preferences to determine whether this FeedMedia object is
     * currently being played and the current player status is playing.
     */
    public static boolean isCurrentlyPlaying(FeedMedia media) {
        return isPlaying(media) && PlaybackService.isRunning
                && ((PlaybackPreferences.getCurrentPlayerStatus() == PlaybackPreferences.PLAYER_STATUS_PLAYING));
    }

    public static boolean isPlaying(FeedMedia media) {
        return PlaybackPreferences.getCurrentlyPlayingMediaType() == FeedMedia.PLAYABLE_TYPE_FEEDMEDIA
                && media != null
                && PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == media.getId();
    }
}
