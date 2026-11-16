package de.danoeh.antennapod.playback.base;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link RewindAfterPauseUtils}.
 */
public class RewindAfterPauseUtilTest {

    @Test
    public void testCalculatePositionWithRewindNoRewind() {
        final int ORIGINAL_POSITION = 10000;
        long lastPlayed = System.currentTimeMillis();
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed, null);

        assertEquals(ORIGINAL_POSITION, position);
    }

    @Test
    public void testCalculatePositionWithRewindMinimalRewind() {
        final int ORIGINAL_POSITION = 10000;
        long lastPlayed = System.currentTimeMillis() - 500;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed, null);

        assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.MINIMUM_REWIND, position);
    }

    @Test
    public void testCalculatePositionWithRewindSmallRewind() {
        final int ORIGINAL_POSITION = 10000;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_SHORT_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed, null);

        assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.SHORT_REWIND, position);
    }

    @Test
    public void testCalculatePositionWithRewindMediumRewind() {
        final int ORIGINAL_POSITION = 10000;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_MEDIUM_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed, null);

        assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.MEDIUM_REWIND, position);
    }

    @Test
    public void testCalculatePositionWithRewindLongRewind() {
        final int ORIGINAL_POSITION = 30000;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_LONG_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed, null);

        assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.LONG_REWIND, position);
    }

    @Test
    public void testCalculatePositionWithRewindNegativeNumber() {
        final int ORIGINAL_POSITION = 100;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_LONG_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed, null);

        assertEquals(0, position);
    }

    @Test
    public void testCalculatePositionWithRewindDoesNotGoBelowSkipIntro() {
        final int ORIGINAL_POSITION = 30000;
        final int SKIP_INTRO_SECONDS = 30;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_LONG_REWIND - 1000;

        FeedPreferences preferences = new FeedPreferences(1, FeedPreferences.AutoDownloadSetting.GLOBAL,
                FeedPreferences.AutoDeleteAction.GLOBAL, VolumeAdaptionSetting.OFF,
                FeedPreferences.NewEpisodesAction.GLOBAL, null, null);
        preferences.setFeedSkipIntro(SKIP_INTRO_SECONDS);
        Feed feed = new Feed("url", null, "title");
        feed.setPreferences(preferences);
        FeedItem item = new FeedItem();
        item.setFeed(feed);
        FeedMedia media = new FeedMedia(0, item, 0, ORIGINAL_POSITION, 0, null, null, null, 0, null, 0, 0);

        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed, media);

        assertEquals(SKIP_INTRO_SECONDS * 1000, position);
    }
}
