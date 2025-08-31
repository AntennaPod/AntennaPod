package de.danoeh.antennapod.storage.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Can be used to guess the release schedule of podcasts based on a sorted list of past release dates
 */
public class ReleaseScheduleGuesser {
    static final long ONE_MINUTE = 60 * 1000;
    static final long ONE_HOUR = ONE_MINUTE * 60;
    static final long ONE_DAY = ONE_HOUR * 24;
    static final long ONE_WEEK = ONE_DAY * 7;
    static final long ONE_MONTH = ONE_DAY * 30;
    private static final int MAX_UNIQUE_DATES = 20;
    private static final int MULTIPLE_PER_DAY_THRESHOLD = 2;

    public enum Schedule {
        DAILY, WEEKDAYS, SPECIFIC_DAYS,
        WEEKLY, BIWEEKLY, FOURWEEKLY,
        MONTHLY, UNKNOWN
    }

    public static class Guess {
        public final Schedule schedule;
        public final List<Integer> days;
        public final Date nextExpectedDate;
        public final boolean multipleReleasesPerDay;

        public Guess(Schedule schedule, List<Integer> days, Date nextExpectedDate, boolean multipleReleasesPerDay) {
            this.schedule = schedule;
            this.days = days;
            this.nextExpectedDate = nextExpectedDate;
            this.multipleReleasesPerDay = multipleReleasesPerDay;
        }
    }

    private static class Stats {
        final float medianHour;
        final float medianDistance;
        final float avgDeltaToMedianDistance;
        final int[] daysOfWeek;
        final int[] daysOfMonth;
        final int mostOftenDayOfWeek;
        final int mostOftenDayOfMonth;

        public Stats(float medianHour, float medianDistance, float avgDeltaToMedianDistance,
                     int[] daysOfWeek, int[] daysOfMonth, int mostOftenDayOfWeek, int mostOftenDayOfMonth) {
            this.medianHour = medianHour;
            this.medianDistance = medianDistance;
            this.avgDeltaToMedianDistance = avgDeltaToMedianDistance;
            this.daysOfWeek = daysOfWeek;
            this.daysOfMonth = daysOfMonth;
            this.mostOftenDayOfWeek = mostOftenDayOfWeek;
            this.mostOftenDayOfMonth = mostOftenDayOfMonth;
        }
    }

    private static void addTime(GregorianCalendar date, long time) {
        date.setTime(new Date(date.getTime().getTime() + time));
    }

    private static void addTimeUntilOnAllowedDay(GregorianCalendar date, long amount, List<Integer> allowedDays) {
        do {
            addTime(date, amount);
        } while (!allowedDays.contains(date.get(Calendar.DAY_OF_WEEK)));
    }

    private static <T> T getMedian(List<T> list) {
        return list.get(list.size() / 2);
    }

    private static Stats getStats(List<Date> releaseDates) {
        ArrayList<Float> hours = new ArrayList<>();
        ArrayList<Long> distances = new ArrayList<>();
        int[] daysOfWeek = new int[8];
        int[] daysOfMonth = new int[32];
        for (int i = 0; i < releaseDates.size(); i++) {
            Date d = releaseDates.get(i);
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(d);
            hours.add(calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60f);
            if (i > 0) {
                distances.add(d.getTime() - releaseDates.get(i - 1).getTime());
            }
            daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK)]++;
            daysOfMonth[calendar.get(Calendar.DAY_OF_MONTH)]++;
        }

        int mostOftenDayOfWeek = 1;
        int mostOftenDayOfWeekNum = 0;
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            if (daysOfWeek[i] > mostOftenDayOfWeekNum) {
                mostOftenDayOfWeekNum = daysOfWeek[i];
                mostOftenDayOfWeek = i;
            }
        }

        int mostOftenDayOfMonth = 1;
        int mostOftenDayOfMonthNum = 0;
        for (int i = 1; i < 31; i++) {
            if (daysOfMonth[i] > mostOftenDayOfMonthNum) {
                mostOftenDayOfMonthNum = daysOfMonth[i];
                mostOftenDayOfMonth = i;
            }
        }

        Collections.sort(hours, Float::compareTo);
        final float medianHour = getMedian(hours);
        Collections.sort(distances, Long::compareTo);
        final float medianDistance = getMedian(distances);

        float avgDeltaToMedianDistance = 0;
        for (long distance : distances) {
            avgDeltaToMedianDistance += Math.abs(distance - medianDistance);
        }
        avgDeltaToMedianDistance /= distances.size();

        return new Stats(medianHour, medianDistance, avgDeltaToMedianDistance,
                daysOfWeek, daysOfMonth, mostOftenDayOfWeek, mostOftenDayOfMonth);
    }

    private static List<Integer> getLargeDays(ReleaseScheduleGuesser.Stats stats, int maxDaysOff) {
        List<Integer> largeDays = new ArrayList<>();
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            if (stats.daysOfWeek[i] > maxDaysOff) {
                largeDays.add(i);
            }
        }
        return largeDays;
    }

    private static Date getNormalizedDate(Date date) {
        GregorianCalendar current = new GregorianCalendar();
        current.setTime(date);
        current.set(Calendar.HOUR_OF_DAY, 0);
        current.set(Calendar.MINUTE, 0);
        current.set(Calendar.SECOND, 0);
        current.set(Calendar.MILLISECOND, 0);
        return current.getTime();
    }

    public static Guess performGuess(List<Date> releaseDates) {
        Collections.sort(releaseDates);
        Set<Date> uniqueDates = new HashSet<>();
        int releaseDatesLowerIndex = releaseDates.size();
        while (releaseDatesLowerIndex > 0 && uniqueDates.size() < MAX_UNIQUE_DATES) {
            Date normalizedDate = getNormalizedDate(releaseDates.get(--releaseDatesLowerIndex));
            uniqueDates.add(normalizedDate);
        }

        if (releaseDates.size() <= 1) {
            return new Guess(Schedule.UNKNOWN, null, null, false);
        } else if (releaseDates.size() > MAX_UNIQUE_DATES) {
            releaseDates = releaseDates.subList(releaseDatesLowerIndex, releaseDates.size());
        }

        Stats stats = getStats(releaseDates);
        final int maxTotalWrongDays = Math.max(1, releaseDates.size() / 5);
        final int maxSingleDayOff = releaseDates.size() / 10;

        GregorianCalendar last = new GregorianCalendar();
        last.setTime(releaseDates.get(releaseDates.size() - 1));
        last.set(Calendar.HOUR_OF_DAY, (int) stats.medianHour);
        last.set(Calendar.MINUTE, (int) ((stats.medianHour - Math.floor(stats.medianHour)) * 60));
        last.set(Calendar.SECOND, 0);
        last.set(Calendar.MILLISECOND, 0);

        boolean multipleReleasesPerDay = (releaseDates.size() - uniqueDates.size()) >= MULTIPLE_PER_DAY_THRESHOLD;
        if (multipleReleasesPerDay) {
            float averagePerDayAmount = (float) releaseDates.size() / uniqueDates.size();
            Date date = getNormalizedDate(last.getTime());
            int releasesToday = 0;
            for (Date releaseDate : releaseDates) {
                if (date.equals(getNormalizedDate(releaseDate))) {
                    releasesToday++;
                }
            }

            long distance;
            if (releasesToday <= averagePerDayAmount) {
                distance = (long) stats.medianDistance;
            } else {
                distance = ONE_DAY;
            }

            List<Integer> largeDays = getLargeDays(stats, 0);
            addTimeUntilOnAllowedDay(last, distance, largeDays);
            Schedule schedule = Schedule.SPECIFIC_DAYS;
            if (largeDays.size() == 7) {
                schedule = Schedule.DAILY;
            } else  if (largeDays.size() == 5 && largeDays.containsAll(Arrays.asList(
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY))) {
                schedule = Schedule.WEEKDAYS;
            }

            return new Guess(schedule, largeDays, last.getTime(), true);
        } else if (Math.abs(stats.medianDistance - ONE_DAY) < 2 * ONE_HOUR
                && stats.avgDeltaToMedianDistance < 2 * ONE_HOUR) {
            addTime(last, ONE_DAY);
            return new Guess(Schedule.DAILY, Arrays.asList(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY), last.getTime(),
                    false);
        } else if (Math.abs(stats.medianDistance - ONE_WEEK) < ONE_DAY
                && stats.avgDeltaToMedianDistance < 2 * ONE_DAY) {
            // Just using last.set(Calendar.DAY_OF_WEEK) could skip a week
            // when the last release is delayed over week boundaries
            addTime(last, 3 * ONE_DAY);
            do {
                addTime(last, ONE_DAY);
            } while (last.get(Calendar.DAY_OF_WEEK) != stats.mostOftenDayOfWeek);
            return new Guess(Schedule.WEEKLY, List.of(stats.mostOftenDayOfWeek), last.getTime(), false);
        } else if (Math.abs(stats.medianDistance - 2 * ONE_WEEK) < ONE_DAY
                && stats.avgDeltaToMedianDistance < 2 * ONE_DAY) {
            // Just using last.set(Calendar.DAY_OF_WEEK) could skip a week
            // when the last release is delayed over week boundaries
            addTime(last, 10 * ONE_DAY);
            do {
                addTime(last, ONE_DAY);
            } while (last.get(Calendar.DAY_OF_WEEK) != stats.mostOftenDayOfWeek);
            return new Guess(Schedule.BIWEEKLY, List.of(stats.mostOftenDayOfWeek), last.getTime(), false);
        } else if (Math.abs(stats.medianDistance - ONE_MONTH) < 5 * ONE_DAY
                && stats.avgDeltaToMedianDistance < 5 * ONE_DAY) {
            if (stats.daysOfMonth[stats.mostOftenDayOfMonth] >= releaseDates.size() - maxTotalWrongDays) {
                // Just using last.set(Calendar.DAY_OF_MONTH) could skip a week
                // when the last release is delayed over week boundaries
                addTime(last, 2 * ONE_WEEK);
                do {
                    addTime(last, ONE_DAY);
                } while (last.get(Calendar.DAY_OF_MONTH) != stats.mostOftenDayOfMonth);
                return new Guess(Schedule.MONTHLY, null, last.getTime(), false);
            }

            addTime(last, 3 * ONE_WEEK + 3 * ONE_DAY);
            do {
                addTime(last, ONE_DAY);
            } while (last.get(Calendar.DAY_OF_WEEK) != stats.mostOftenDayOfWeek);
            return new Guess(Schedule.FOURWEEKLY, List.of(stats.mostOftenDayOfWeek), last.getTime(), false);
        }

        // Find release days
        List<Integer> largeDays = getLargeDays(stats, maxSingleDayOff);

        // Ensure that all release days are used similarly often
        int averageDays = releaseDates.size() / largeDays.size();
        boolean matchesAverageDays = true;
        for (int day : largeDays) {
            if (stats.daysOfWeek[day] < averageDays - maxSingleDayOff) {
                matchesAverageDays = false;
                break;
            }
        }

        if (matchesAverageDays && stats.medianDistance < ONE_WEEK) {
            // Fixed daily release schedule (eg Mo, Thu, Fri)
            addTimeUntilOnAllowedDay(last, ONE_DAY, largeDays);

            if (largeDays.size() == 5 && largeDays.containsAll(Arrays.asList(
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY))) {
                return new Guess(Schedule.WEEKDAYS, largeDays, last.getTime(), false);
            }
            return new Guess(Schedule.SPECIFIC_DAYS, largeDays, last.getTime(), false);
        } else if (largeDays.size() == 1) {
            // Probably still weekly with more exceptions than others
            addTimeUntilOnAllowedDay(last, ONE_DAY, largeDays);
            return new Guess(Schedule.WEEKLY, largeDays, last.getTime(), false);
        }
        addTime(last, (long) (0.6f * stats.medianDistance));
        return new Guess(Schedule.UNKNOWN, null, last.getTime(), false);
    }
}