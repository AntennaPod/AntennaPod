package de.danoeh.antennapod.storage.database;

import de.danoeh.antennapod.parser.feed.util.DateUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

public class ReleaseScheduleGuesserRealWorldTest {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);

    private void printHistogram(int[] histogram) {
        int max = 0;
        for (int x : histogram) {
            max = Math.max(x, max);
        }
        for (int row = 8; row >= 0; row--) {
            for (int x : histogram) {
                System.out.print((x > (double) row * (max / 9.0)) ? "#" : " ");
            }
            System.out.println();
        }
        for (int col = 0; col < histogram.length; col++) {
            System.out.print(((col % 5) == 0) ? "|" : " ");
        }
        System.out.println();
    }

    @Test
    public void testRealWorld() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResource("release_dates.csv").openStream();
        int numCorrectDay = 0;
        int numFoundSchedule = 0;
        int numFoundScheduleAndCorrectDay = 0;
        int num3hoursCorrect = 0;
        int numOffByMoreThan2days = 0;
        int[] histogram = new int[101];

        String csv = IOUtils.toString(inputStream, "UTF-8");
        String[] lines = csv.split("\n");
        int totalPodcasts = 0;
        int lineNr = 0;
        for (String line : lines) {
            lineNr++;
            String[] dates = line.split(";");
            List<Date> releaseDates = new ArrayList<>();
            for (String date : dates) {
                releaseDates.add(DateUtils.parse(date));
            }
            if (releaseDates.size() <= 3) {
                continue;
            }
            totalPodcasts++;
            Collections.sort(releaseDates, Comparator.comparingLong(Date::getTime));
            Date dateActual = releaseDates.get(releaseDates.size() - 1);
            // Remove most recent one and possible duplicates of episodes on the same day
            do {
                releaseDates = releaseDates.subList(0, releaseDates.size() - 1);
            } while (releaseDates.get(releaseDates.size() - 1).getTime()
                    > dateActual.getTime() - 30 * ReleaseScheduleGuesser.ONE_MINUTE);
            ReleaseScheduleGuesser.Guess guess = ReleaseScheduleGuesser.performGuess(releaseDates);

            final boolean is3hoursClose = Math.abs(dateActual.getTime() - guess.nextExpectedDate.getTime())
                    < 3 * ReleaseScheduleGuesser.ONE_HOUR;
            //noinspection ConstantValue
            if (false) {
                System.out.println(lineNr + " guessed: " + DATE_FORMAT.format(guess.nextExpectedDate)
                        + ", actual: " + DATE_FORMAT.format(dateActual)
                        + " " + guess.schedule.name() + (is3hoursClose ? " ✔" : ""));
            }
            long deltaTime = dateActual.getTime() - guess.nextExpectedDate.getTime();
            int histogramClass = (int) Math.max(0, Math.min(100, deltaTime / ReleaseScheduleGuesser.ONE_HOUR + 50));
            histogram[histogramClass]++;
            boolean foundSchedule = guess.schedule != ReleaseScheduleGuesser.Schedule.UNKNOWN;
            if (foundSchedule) {
                numFoundSchedule++;
            }
            Calendar calendarExpected = new GregorianCalendar();
            calendarExpected.setTime(dateActual);
            Calendar calendarGuessed = new GregorianCalendar();
            calendarGuessed.setTime(guess.nextExpectedDate);
            if (calendarExpected.get(Calendar.DAY_OF_YEAR) == calendarGuessed.get(Calendar.DAY_OF_YEAR)) {
                numCorrectDay++;
                if (foundSchedule) {
                    numFoundScheduleAndCorrectDay++;
                }
            }
            if (Math.abs(deltaTime) > 2 * ReleaseScheduleGuesser.ONE_DAY) {
                numOffByMoreThan2days++;
            }
            if (is3hoursClose) {
                num3hoursCorrect++;
            }
        }

        System.out.println("Podcasts tested: " + totalPodcasts);

        double schedulePercentage = 100.0 * numFoundSchedule / totalPodcasts;
        System.out.println("Found schedule: " + schedulePercentage);
        double offByLessThan3HoursPercentage = 100.0 * num3hoursCorrect / totalPodcasts;
        System.out.println("Off by less than 3 hours: " + offByLessThan3HoursPercentage);
        double scheduleAndCorrectDayPercentage = 100.0 * numFoundScheduleAndCorrectDay / numFoundSchedule;
        System.out.println("Correct day when schedule found: " + scheduleAndCorrectDayPercentage);
        double correctDayPercentage = 100.0 * numCorrectDay / totalPodcasts;
        System.out.println("Correct day: " + correctDayPercentage);
        double offByLessThan2daysPercentage = 100.0 * (totalPodcasts - numOffByMoreThan2days) / totalPodcasts;
        System.out.println("Off by less than 2 days: " + offByLessThan2daysPercentage);

        assertTrue(schedulePercentage > 80);
        assertTrue(offByLessThan3HoursPercentage > 55);
        assertTrue(scheduleAndCorrectDayPercentage > 75);
        assertTrue(correctDayPercentage > 60);
        assertTrue(offByLessThan2daysPercentage > 75);

        printHistogram(histogram);
    }
}