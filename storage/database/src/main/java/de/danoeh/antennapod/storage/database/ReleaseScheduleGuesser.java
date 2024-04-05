package de.danoeh.antennapod.storage.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Can be used to guess the release schedule of podcasts based on a sorted list of past release dates
 */
public class ReleaseScheduleGuesser {
    static final long ONE_MINUTE = 60 * 1000;
    static final long ONE_HOUR = ONE_MINUTE * 60;
    static final long ONE_DAY =  ONE_HOUR * 24;
    static final long ONE_WEEK = ONE_DAY * 7;
    static final long ONE_MONTH = ONE_DAY * 30;
    private static final int MAX_DATA_POINTS = 20;

    public enum Schedule {
        DAILY, WEEKDAYS, SPECIFIC_DAYS,
        WEEKLY, BIWEEKLY, FOURWEEKLY,
        MONTHLY, UNKNOWN
    }

    public static class Guess {
        public final Schedule schedule;
        public final List<Integer> days;
        public final Date nextExpectedDate;

        public Guess(Schedule schedule, List<Integer> days, Date nextExpectedDate) {
            this.schedule = schedule;
            this.days = days;
            this.nextExpectedDate = nextExpectedDate;
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

    private static void addUntil(GregorianCalendar date, List<Integer> days) {
        do {
            addTime(date, ONE_DAY);
        } while (!days.contains(date.get(Calendar.DAY_OF_WEEK)));
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

    public static Guess performGuess(List<Date> releaseDates) {
        if (releaseDates.size() <= 1) {
            return new Guess(Schedule.UNKNOWN, null, null);
        } else if (releaseDates.size() > MAX_DATA_POINTS) {
            releaseDates = releaseDates.subList(releaseDates.size() - MAX_DATA_POINTS, releaseDates.size());
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

        if (Math.abs(stats.medianDistance - ONE_DAY) < 2 * ONE_HOUR
                && stats.avgDeltaToMedianDistance < 2 * ONE_HOUR) {
            addTime(last, ONE_DAY);
            return new Guess(Schedule.DAILY, Arrays.asList(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY), last.getTime());
        } else if (Math.abs(stats.medianDistance - ONE_WEEK) < ONE_DAY
                && stats.avgDeltaToMedianDistance < 2 * ONE_DAY) {
            // Just using last.set(Calendar.DAY_OF_WEEK) could skip a week
            // when the last release is delayed over week boundaries
            addTime(last, 3 * ONE_DAY);
            do {
                addTime(last, ONE_DAY);
            } while (last.get(Calendar.DAY_OF_WEEK) != stats.mostOftenDayOfWeek);
            return new Guess(Schedule.WEEKLY, List.of(stats.mostOftenDayOfWeek), last.getTime());
        } else if (Math.abs(stats.medianDistance - 2 * ONE_WEEK) < ONE_DAY
                && stats.avgDeltaToMedianDistance < 2 * ONE_DAY) {
            // Just using last.set(Calendar.DAY_OF_WEEK) could skip a week
            // when the last release is delayed over week boundaries
            addTime(last, 10 * ONE_DAY);
            do {
                addTime(last, ONE_DAY);
            } while (last.get(Calendar.DAY_OF_WEEK) != stats.mostOftenDayOfWeek);
            return new Guess(Schedule.BIWEEKLY, List.of(stats.mostOftenDayOfWeek), last.getTime());
        } else if (Math.abs(stats.medianDistance - ONE_MONTH) < 5 * ONE_DAY
                && stats.avgDeltaToMedianDistance < 5 * ONE_DAY) {
            if (stats.daysOfMonth[stats.mostOftenDayOfMonth] >= releaseDates.size() - maxTotalWrongDays) {
                // Just using last.set(Calendar.DAY_OF_MONTH) could skip a week
                // when the last release is delayed over week boundaries
                addTime(last, 2 * ONE_WEEK);
                do {
                    addTime(last, ONE_DAY);
                } while (last.get(Calendar.DAY_OF_MONTH) != stats.mostOftenDayOfMonth);
                return new Guess(Schedule.MONTHLY, null, last.getTime());
            }

            addTime(last, 3 * ONE_WEEK + 3 * ONE_DAY);
            do {
                addTime(last, ONE_DAY);
            } while (last.get(Calendar.DAY_OF_WEEK) != stats.mostOftenDayOfWeek);
            return new Guess(Schedule.FOURWEEKLY, List.of(stats.mostOftenDayOfWeek), last.getTime());
        }

        // Find release days
        List<Integer> largeDays = new ArrayList<>();
        for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            if (stats.daysOfWeek[i] > maxSingleDayOff) {
                largeDays.add(i);
            }
        }
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
            addUntil(last, largeDays);

            if (largeDays.size() == 5 && largeDays.containsAll(Arrays.asList(
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY))) {
                return new Guess(Schedule.WEEKDAYS, largeDays, last.getTime());
            }
            return new Guess(Schedule.SPECIFIC_DAYS, largeDays, last.getTime());
        } else if (largeDays.size() == 1) {
            // Probably still weekly with more exceptions than others
            addUntil(last, largeDays);
            return new Guess(Schedule.WEEKLY, largeDays, last.getTime());
        }

        addTime(last, (long) (0.6f * stats.medianDistance));
        return new Guess(Schedule.UNKNOWN, null, last.getTime());
    }
}
