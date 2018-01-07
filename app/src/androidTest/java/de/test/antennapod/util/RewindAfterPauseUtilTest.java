package de.test.antennapod.util;

import junit.framework.TestCase;

import de.danoeh.antennapod.core.util.RewindAfterPauseUtils;

/**
 * Tests for {@link RewindAfterPauseUtils}.
 */
public class RewindAfterPauseUtilTest extends TestCase {

    public void testCalculatePositionWithRewindNoRewind() {
        final int ORIGINAL_POSITION = 10000;
        long lastPlayed = System.currentTimeMillis();
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed);

        assertEquals(ORIGINAL_POSITION, position);
    }

    public void testCalculatePositionWithRewindSmallRewind() {
        final int ORIGINAL_POSITION = 10000;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_SHORT_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed);

        assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.SHORT_REWIND, position);
    }

    public void testCalculatePositionWithRewindMediumRewind() {
        final int ORIGINAL_POSITION = 10000;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_MEDIUM_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed);

        assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.MEDIUM_REWIND, position);
    }

    public void testCalculatePositionWithRewindLongRewind() {
        final int ORIGINAL_POSITION = 30000;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_LONG_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed);

        assertEquals(ORIGINAL_POSITION - RewindAfterPauseUtils.LONG_REWIND, position);
    }

    public void testCalculatePositionWithRewindNegativeNumber() {
        final int ORIGINAL_POSITION = 100;
        long lastPlayed = System.currentTimeMillis() - RewindAfterPauseUtils.ELAPSED_TIME_FOR_LONG_REWIND - 1000;
        int position = RewindAfterPauseUtils.calculatePositionWithRewind(ORIGINAL_POSITION, lastPlayed);

        assertEquals(0, position);
    }
}
