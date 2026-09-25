package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class SkipUtilsTest {

    private static final long DURATION_MS = 30 * 60 * 1000L;
    private static final int SKIP_ENDING_SECONDS = 30;
    private static final float SPEED_NORMAL = 1.0f;

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
    }

    private FeedMedia createFeedMediaWithSkipEnding(int skipEndingSeconds) {
        FeedPreferences prefs = mock(FeedPreferences.class);
        when(prefs.getFeedSkipEnding()).thenReturn(skipEndingSeconds);

        Feed feed = mock(Feed.class);
        when(feed.getPreferences()).thenReturn(prefs);

        FeedItem item = mock(FeedItem.class);
        when(item.getFeed()).thenReturn(feed);

        FeedMedia media = mock(FeedMedia.class);
        when(media.getItem()).thenReturn(item);

        return media;
    }

    @Test
    public void testNormalPlayback_triggersSkipInWindow() {
        FeedMedia media = createFeedMediaWithSkipEnding(SKIP_ENDING_SECONDS);
        long skipPointMs = DURATION_MS - SKIP_ENDING_SECONDS * 1000L;
        long position = skipPointMs - 500;

        boolean result = SkipUtils.skipEndingIfNecessary(context, media, position, DURATION_MS, SPEED_NORMAL);
        assertTrue("Should skip when position is within the normal trigger window", result);
    }

    @Test
    public void testNormalPlayback_triggersSkipJustPastThreshold() {
        FeedMedia media = createFeedMediaWithSkipEnding(SKIP_ENDING_SECONDS);
        long skipPointMs = DURATION_MS - SKIP_ENDING_SECONDS * 1000L;
        long position = skipPointMs + 100;

        boolean result = SkipUtils.skipEndingIfNecessary(context, media, position, DURATION_MS, SPEED_NORMAL);
        assertTrue("Should skip when position just crosses past skip threshold", result);
    }

    @Test
    public void testNormalPlayback_doesNotTriggerBeforeWindow() {
        FeedMedia media = createFeedMediaWithSkipEnding(SKIP_ENDING_SECONDS);
        long skipPointMs = DURATION_MS - SKIP_ENDING_SECONDS * 1000L;
        long position = skipPointMs - 2000;

        boolean result = SkipUtils.skipEndingIfNecessary(context, media, position, DURATION_MS, SPEED_NORMAL);
        assertFalse("Should not skip when well before skip threshold", result);
    }

    @Test
    public void testSilenceSkip_jumpsIntoSkipZone() {
        FeedMedia media = createFeedMediaWithSkipEnding(SKIP_ENDING_SECONDS);
        long position = DURATION_MS - 15_000L;

        boolean result = SkipUtils.skipEndingIfNecessary(context, media, position, DURATION_MS, SPEED_NORMAL);
        assertTrue("Should skip when silence-skip jumps position into the skip zone", result);
    }

    @Test
    public void testNoSkipWhenDisabled() {
        FeedMedia media = createFeedMediaWithSkipEnding(0);
        long position = DURATION_MS - 15_000L;

        boolean result = SkipUtils.skipEndingIfNecessary(context, media, position, DURATION_MS, SPEED_NORMAL);
        assertFalse("Should not skip when skip ending is disabled", result);
    }

    @Test
    public void testNoSkipWhenSkipEndExceedsDuration() {
        FeedMedia media = createFeedMediaWithSkipEnding(60);
        long shortDuration = 30_000L;

        boolean result = SkipUtils.skipEndingIfNecessary(context, media, 20_000L, shortDuration, SPEED_NORMAL);
        assertFalse("Should not skip when skip ending exceeds episode duration", result);
    }

    @Test
    public void testNoSkipWhenAtExactEnd() {
        FeedMedia media = createFeedMediaWithSkipEnding(SKIP_ENDING_SECONDS);
        long position = DURATION_MS;

        boolean result = SkipUtils.skipEndingIfNecessary(context, media, position, DURATION_MS, SPEED_NORMAL);
        assertFalse("Should not skip when already at the exact end", result);
    }

    @Test
    public void testNoSkipWhenNullFeedInfo() {
        FeedMedia media = mock(FeedMedia.class);
        when(media.getItem()).thenReturn(null);

        boolean result = SkipUtils.skipEndingIfNecessary(context, media, DURATION_MS - 15_000L, DURATION_MS, SPEED_NORMAL);
        assertFalse("Should not skip when feed info is null", result);
    }
}
