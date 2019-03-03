package de.test.antennapod.util;

import android.test.AndroidTestCase;

import de.danoeh.antennapod.core.util.Converter;

/**
 * Test class for converter
 */
public class ConverterTest extends AndroidTestCase {

    public void testGetDurationStringLong() throws Exception {
        String expected = "13:05:10";
        int input = 47110000;
        assertEquals(expected, Converter.getDurationStringLong(input));
    }

    public void testGetDurationStringShort() throws Exception {
        String expected = "13:05";
        assertEquals(expected, Converter.getDurationStringShort(47110000, true));
        assertEquals(expected, Converter.getDurationStringShort(785000, false));
    }

    public void testDurationStringLongToMs() throws Exception {
        String input = "01:20:30";
        long expected = 4830000;
        assertEquals(expected, Converter.durationStringLongToMs(input));
    }

    public void testDurationStringShortToMs() throws Exception {
        String input = "8:30";
        assertEquals(30600000, Converter.durationStringShortToMs(input, true));
        assertEquals(510000, Converter.durationStringShortToMs(input, false));
    }
}
