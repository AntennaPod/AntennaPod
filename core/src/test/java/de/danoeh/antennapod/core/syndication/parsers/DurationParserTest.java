package de.danoeh.antennapod.core.syndication.parsers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DurationParserTest {
    private int milliseconds = 1;
    private int seconds = 1000 * milliseconds;
    private int minutes = 60 * seconds;
    private int hours = 60 * minutes;

    @Test
    public void testSecondDurationInMillis() {
        long duration = DurationParser.inMillis("00:45");
        assertEquals(45 * seconds, duration);
    }

    @Test
    public void testSingleNumberDurationInMillis() {
        int twoHoursInSeconds = 2 * 60 * 60;
        long duration = DurationParser.inMillis(String.valueOf(twoHoursInSeconds));
        assertEquals(2 * hours, duration);
    }

    @Test
    public void testMinuteSecondDurationInMillis() {
        long duration = DurationParser.inMillis("05:10");
        assertEquals(5 * minutes + 10 * seconds, duration);
    }

    @Test
    public void testHourMinuteSecondDurationInMillis() {
        long duration = DurationParser.inMillis("02:15:45");
        assertEquals(2 * hours + 15 * minutes + 45 * seconds, duration);
    }

    @Test
    public void testSecondsWithMillisecondsInMillis() {
        long duration = DurationParser.inMillis("00:00:00.123");
        assertEquals(123, duration);
    }
}
