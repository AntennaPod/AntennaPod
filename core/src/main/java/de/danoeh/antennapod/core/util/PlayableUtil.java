package de.danoeh.antennapod.core.util;

import android.content.SharedPreferences;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackPosition;

public class PlayableUtil {

    public static void saveCurrentPosition(PlaybackPosition currentPosition, Playable playable, SharedPreferences sharedPreferences) {
        if (playable instanceof FeedMedia)
            saveForFeedMedia(sharedPreferences, currentPosition, (FeedMedia) playable);
        else
            playable.saveCurrentPosition(sharedPreferences, currentPosition);
    }

    public static void loadChapterMarks(Playable playable) {
        if (playable instanceof FeedMedia) loadForFeedMedia((FeedMedia) playable);
        else playable.loadChapterMarks();

    }

    public static void loadMetadata(Playable playable) throws Playable.PlayableException {
        if(playable instanceof FeedMedia) loadMetadataForFeedMedia(((FeedMedia) playable));
        else playable.loadMetadata();
    }

    private static void loadMetadataForFeedMedia(FeedMedia feedMedia) {
        loadItemIfNecessary(feedMedia);
    }

    private static void loadForFeedMedia(FeedMedia feedMedia) {
        loadItemIfNecessary(feedMedia);
        if (feedMedia.shouldStopLoadingChapterMarks()) {
            return;
        }
        // check if chapters are stored in db and not loaded yet.
        if (feedMedia.hasChapters()) {
            DBReader.loadChaptersOfFeedItem(feedMedia.getItem());
        } else {
            feedMedia.loadChapterMarks();
            if (feedMedia.getItem().getChapters() != null) {
                DBWriter.setFeedItem(feedMedia.getItem());
            }
        }

    }

    private static void loadItemIfNecessary(FeedMedia feedMedia) {
        if (feedMedia.shouldLoadItem()) {
            feedMedia.setItem(DBReader.getFeedItem(feedMedia.getItemID()));
        }
    }

    private static void saveForFeedMedia(SharedPreferences preferences, PlaybackPosition currentPosition, FeedMedia feedMedia) {
        if (feedMedia.shouldMarkItemAsPlayed()) {
            DBWriter.markItemPlayed(FeedItem.UNPLAYED, feedMedia.getItem().getId());
        }
        feedMedia.saveCurrentPosition(preferences, currentPosition);
        DBWriter.setFeedMediaPlaybackInformation(feedMedia);
    }


}

