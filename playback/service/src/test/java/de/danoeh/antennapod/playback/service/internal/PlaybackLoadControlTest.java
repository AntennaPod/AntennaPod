package de.danoeh.antennapod.playback.service.internal;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class PlaybackLoadControlTest {
    @Test
    public void usesSlidingWindowShorterThanOneHour() {
        assertTrue(PlaybackLoadControl.MAX_BUFFER_MS < TimeUnit.HOURS.toMillis(1));
        assertTrue(PlaybackLoadControl.MIN_BUFFER_MS < PlaybackLoadControl.MAX_BUFFER_MS);
        assertTrue(PlaybackLoadControl.BACK_BUFFER_MS < PlaybackLoadControl.MIN_BUFFER_MS);
    }
}
