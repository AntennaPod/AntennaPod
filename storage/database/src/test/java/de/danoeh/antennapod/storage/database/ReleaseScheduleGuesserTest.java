package de.danoeh.antennapod.storage.database;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static de.danoeh.antennapod.storage.database.ReleaseScheduleGuesser.ONE_DAY;
import static de.danoeh.antennapod.storage.database.ReleaseScheduleGuesser.ONE_HOUR;
import static de.danoeh.antennapod.storage.database.ReleaseScheduleGuesser.ONE_MINUTE;
import static de.danoeh.antennapod.storage.database.ReleaseScheduleGuesser.performGuess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReleaseScheduleGuesserTest {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);

    private Date makeDate(String dateStr) {
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertClose(Date expected, Date actual, long tolerance) {
        assertTrue("Date should differ at most " + tolerance / 60000 + " minutes from "
                + DATE_FORMAT.format(expected) + ", but is " + DATE_FORMAT.format(actual),
                Math.abs(expected.getTime() - actual.getTime()) < tolerance);
    }

    @Test
    public void testEdgeCases() {
        ArrayList<Date> releaseDates = new ArrayList<>();
        assertEquals(ReleaseScheduleGuesser.Schedule.UNKNOWN, performGuess(releaseDates).schedule);
        releaseDates.add(makeDate("2024-01-01 16:30"));
        assertEquals(ReleaseScheduleGuesser.Schedule.UNKNOWN, performGuess(releaseDates).schedule);
    }

    @Test
    public void testDaily() {
        ArrayList<Date> releaseDates = new ArrayList<>();
        releaseDates.add(makeDate("2024-01-01 16:30")); // Monday
        releaseDates.add(makeDate("2024-01-02 16:25"));
        releaseDates.add(makeDate("2024-01-03 16:35"));
        releaseDates.add(makeDate("2024-01-04 16:40"));
        releaseDates.add(makeDate("2024-01-05 16:20"));
        releaseDates.add(makeDate("2024-01-06 16:10"));
        releaseDates.add(makeDate("2024-01-07 16:32")); // Sunday

        // Next day
        ReleaseScheduleGuesser.Guess guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.DAILY, guess.schedule);
        assertClose(makeDate("2024-01-08 16:30"), guess.nextExpectedDate, 10 * ONE_MINUTE);

        // One-off early release
        releaseDates.add(makeDate("2024-01-08 10:00"));
        guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.DAILY, guess.schedule);
        assertClose(makeDate("2024-01-09 16:30"), guess.nextExpectedDate, 10 * ONE_MINUTE);
    }

    @Test
    public void testWeekdays() {
        ArrayList<Date> releaseDates = new ArrayList<>();
        releaseDates.add(makeDate("2024-01-01 16:30")); // Monday
        releaseDates.add(makeDate("2024-01-02 16:25"));
        releaseDates.add(makeDate("2024-01-03 16:35"));
        releaseDates.add(makeDate("2024-01-04 16:40"));
        releaseDates.add(makeDate("2024-01-05 16:20")); // Friday
        releaseDates.add(makeDate("2024-01-08 16:20")); // Monday
        releaseDates.add(makeDate("2024-01-09 16:30"));
        releaseDates.add(makeDate("2024-01-10 16:40"));
        releaseDates.add(makeDate("2024-01-11 16:45")); // Thursday

        // Next day
        ReleaseScheduleGuesser.Guess guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.WEEKDAYS, guess.schedule);
        assertClose(makeDate("2024-01-12 16:30"), guess.nextExpectedDate, ONE_HOUR);

        // After weekend
        releaseDates.add(makeDate("2024-01-12 16:30")); // Friday
        guess = performGuess(releaseDates);
        assertClose(makeDate("2024-01-15 16:30"), guess.nextExpectedDate, ONE_HOUR);
    }

    @Test
    public void testWeekly() {
        ArrayList<Date> releaseDates = new ArrayList<>();
        releaseDates.add(makeDate("2024-01-07 16:30")); // Sunday
        releaseDates.add(makeDate("2024-01-14 16:25"));
        releaseDates.add(makeDate("2024-01-21 14:25"));
        releaseDates.add(makeDate("2024-01-28 16:15"));

        // Next week
        ReleaseScheduleGuesser.Guess guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.WEEKLY, guess.schedule);
        assertClose(makeDate("2024-02-04 16:30"), guess.nextExpectedDate, 2 * ONE_HOUR);

        // One-off early release
        releaseDates.add(makeDate("2024-02-02 16:35"));
        guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.WEEKLY, guess.schedule);
        assertClose(makeDate("2024-02-11 16:30"), guess.nextExpectedDate, 2 * ONE_HOUR);

        // One-off late release
        releaseDates.add(makeDate("2024-02-13 16:35"));
        guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.WEEKLY, guess.schedule);
        assertClose(makeDate("2024-02-18 16:30"), guess.nextExpectedDate, 2 * ONE_HOUR);
    }

    @Test
    public void testMonthly() {
        ArrayList<Date> releaseDates = new ArrayList<>();
        releaseDates.add(makeDate("2024-01-01 16:30"));
        releaseDates.add(makeDate("2024-02-01 16:30"));
        releaseDates.add(makeDate("2024-03-01 16:30"));
        releaseDates.add(makeDate("2024-04-01 16:30"));

        // Next month
        ReleaseScheduleGuesser.Guess guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.MONTHLY, guess.schedule);
        assertClose(makeDate("2024-05-01 16:30"), guess.nextExpectedDate, 10 * ONE_HOUR);

        // One-off early release
        releaseDates.add(makeDate("2024-04-30 16:30"));
        guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.MONTHLY, guess.schedule);
        assertClose(makeDate("2024-06-01 16:30"), guess.nextExpectedDate, 10 * ONE_HOUR);

        // One-off late release
        releaseDates.remove(releaseDates.size() - 1);
        releaseDates.add(makeDate("2024-05-13 16:30"));
        guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.MONTHLY, guess.schedule);
        assertClose(makeDate("2024-06-01 16:30"), guess.nextExpectedDate, 10 * ONE_HOUR);
    }

    @Test
    public void testFourweekly() {
        ArrayList<Date> releaseDates = new ArrayList<>();
        releaseDates.add(makeDate("2024-01-01 16:30"));
        releaseDates.add(makeDate("2024-01-29 16:30"));
        releaseDates.add(makeDate("2024-02-26 16:30"));
        releaseDates.add(makeDate("2024-03-25 16:30"));

        // 4 weeks later
        ReleaseScheduleGuesser.Guess guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.FOURWEEKLY, guess.schedule);
        assertClose(makeDate("2024-04-22 16:30"), guess.nextExpectedDate, 10 * ONE_HOUR);
    }

    @Test
    public void testUnknown() {
        ArrayList<Date> releaseDates = new ArrayList<>();
        releaseDates.add(makeDate("2024-01-01 16:30"));
        releaseDates.add(makeDate("2024-01-03 16:30"));
        releaseDates.add(makeDate("2024-01-03 16:31"));
        releaseDates.add(makeDate("2024-01-04 16:30"));
        releaseDates.add(makeDate("2024-01-04 16:31"));
        releaseDates.add(makeDate("2024-01-07 16:30"));
        releaseDates.add(makeDate("2024-01-07 16:31"));
        releaseDates.add(makeDate("2024-01-10 16:30"));
        ReleaseScheduleGuesser.Guess guess = performGuess(releaseDates);
        assertEquals(ReleaseScheduleGuesser.Schedule.UNKNOWN, guess.schedule);
        assertClose(makeDate("2024-01-12 16:30"), guess.nextExpectedDate, 2 * ONE_DAY);
    }
}