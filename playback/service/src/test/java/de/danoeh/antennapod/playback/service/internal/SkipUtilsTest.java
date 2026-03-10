package de.danoeh.antennapod.playback.service.internal;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SkipUtilsTest {

    @Test
    public void skipIntroPosition_noPreferences_returnsCurrentPosition() {
        FeedMedia media = mock(FeedMedia.class);
        when(media.getItem()).thenReturn(null);

        assertEquals(0, SkipUtils.skipIntroPosition(media, 0));
    }

    @Test
    public void skipIntroPosition_skipIntroZero_returnsCurrentPosition() {
        FeedMedia media = mockFeedMedia(0, 0, 600000);

        assertEquals(5000, SkipUtils.skipIntroPosition(media, 5000));
    }

    @Test
    public void skipIntroPosition_currentPositionBeforeIntro_returnsIntroPosition() {
        FeedMedia media = mockFeedMedia(30, 0, 600000);

        assertEquals(30000, SkipUtils.skipIntroPosition(media, 0));
    }

    @Test
    public void skipIntroPosition_currentPositionAfterIntro_returnsCurrentPosition() {
        FeedMedia media = mockFeedMedia(30, 0, 600000);

        assertEquals(60000, SkipUtils.skipIntroPosition(media, 60000));
    }

    @Test
    public void skipIntroPosition_introLongerThanDuration_returnsCurrentPosition() {
        FeedMedia media = mockFeedMedia(30, 0, 20000);

        assertEquals(0, SkipUtils.skipIntroPosition(media, 0));
    }

    @Test
    public void skipIntroPosition_unknownDuration_appliesSkipIntro() {
        FeedMedia media = mockFeedMedia(30, 0, -1);

        assertEquals(30000, SkipUtils.skipIntroPosition(media, 0));
    }

    @Test
    public void skipEndingSeconds_noPreferences_returnsZero() {
        FeedMedia media = mock(FeedMedia.class);
        when(media.getItem()).thenReturn(null);

        assertEquals(0, SkipUtils.skipEndingSeconds(media, 50000, 100000, 1.0f));
    }

    @Test
    public void skipEndingSeconds_skipEndZero_returnsZero() {
        FeedMedia media = mockFeedMedia(0, 0, 100000);

        assertEquals(0, SkipUtils.skipEndingSeconds(media, 95000, 100000, 1.0f));
    }

    @Test
    public void skipEndingSeconds_withinSkipEndWindow_returnsSeconds() {
        FeedMedia media = mockFeedMedia(0, 5, 100000);

        assertEquals(5, SkipUtils.skipEndingSeconds(media, 94500, 100000, 1.0f));
    }

    @Test
    public void skipEndingSeconds_outsideSkipEndWindow_returnsZero() {
        FeedMedia media = mockFeedMedia(0, 5, 100000);

        assertEquals(0, SkipUtils.skipEndingSeconds(media, 80000, 100000, 1.0f));
    }

    @Test
    public void skipEndingSeconds_skipEndLongerThanDuration_returnsZero() {
        FeedMedia media = mockFeedMedia(0, 200, 100000);

        assertEquals(0, SkipUtils.skipEndingSeconds(media, 190000, 100000, 1.0f));
    }

    private FeedMedia mockFeedMedia(int skipIntroSecs, int skipEndingSecs, int durationMs) {
        FeedMedia media = mock(FeedMedia.class);
        FeedItem feedItem = mock(FeedItem.class);
        Feed feed = mock(Feed.class);
        FeedPreferences feedPreferences = mock(FeedPreferences.class);

        when(media.getItem()).thenReturn(feedItem);
        when(feedItem.getFeed()).thenReturn(feed);
        when(feed.getPreferences()).thenReturn(feedPreferences);
        when(feedPreferences.getFeedSkipIntro()).thenReturn(skipIntroSecs);
        when(feedPreferences.getFeedSkipEnding()).thenReturn(skipEndingSecs);
        when(media.getDuration()).thenReturn(durationMs);
        return media;
    }
}
