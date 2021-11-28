package de.danoeh.antennapod.playback.base;

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
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed);

        assertEquals(ORIGINAL_POSITION, position);
    }

    @Test
    public void testCalculatePositionWithRewindSmallRewind() {
        final int ORIGINAL_POSITION = 10000;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_SHORT_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed);

        assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.SHORT_REWIND, position);
    }

    @Test
    public void testCalculatePositionWithRewindMediumRewind() {
        final int ORIGINAL_POSITION = 10000;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_MEDIUM_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed);

        assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.MEDIUM_REWIND, position);
    }

    @Test
    public void testCalculatePositionWithRewindLongRewind() {
        final int ORIGINAL_POSITION = 30000;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_LONG_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed);

        assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.LONG_REWIND, position);
    }

    @Test
    public void testCalculatePositionWithRewindNegativeNumber() {
        final int ORIGINAL_POSITION = 100;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_LONG_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed);

        assertEquals(0, position);
    }
}
