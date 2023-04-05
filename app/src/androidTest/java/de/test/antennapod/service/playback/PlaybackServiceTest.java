package de.test.antennapod.service.playback;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.danoeh.antennapod.core.service.playback.PlaybackService;

public class PlaybackServiceTest {
    @Test
    public void testIsInTimeRange() {
        assertTrue(PlaybackService.isInTimeRange(0, 10, 8));
        assertTrue(PlaybackService.isInTimeRange(1, 10, 8));
        assertTrue(PlaybackService.isInTimeRange(1, 10, 1));
        assertTrue(PlaybackService.isInTimeRange(20, 10, 8));
        assertTrue(PlaybackService.isInTimeRange(20, 20, 8));
        assertFalse(PlaybackService.isInTimeRange(1, 6, 8));
        assertFalse(PlaybackService.isInTimeRange(1, 6, 6));
        assertFalse(PlaybackService.isInTimeRange(20, 6, 8));
    }
}
