package de.danoeh.antennapod.ui.common;

import android.content.Context;

import android.content.res.Resources;
import java.util.Locale;

/** Provides methods for converting various units. */
public final class Converter {
    /** Class shall not be instantiated. */
    private Converter() {
    }

    private static final int HOURS_MIL = 3600000;
    private static final int MINUTES_MIL = 60000;
    private static final int SECONDS_MIL = 1000;

    /**
     * Converts milliseconds to a string containing hours, minutes and seconds.
     */
    public static String getDurationStringLong(int duration) {
        if (duration <= 0) {
            return "00:00:00";
        } else {
            int[] hms = millisecondsToHms(duration);
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hms[0], hms[1], hms[2]);
        }
    }

    private static int[] millisecondsToHms(long duration) {
        int h = (int) (duration / HOURS_MIL);
        long rest = duration - (long) h * HOURS_MIL;
        int m = (int) (rest / MINUTES_MIL);
        rest -= (long) m * MINUTES_MIL;
        int s = (int) (rest / SECONDS_MIL);
        return new int[] {h, m, s};
    }

    /**
     * Converts milliseconds to a string containing hours and minutes or minutes and seconds.
     */
    public static String getDurationStringShort(int duration, boolean durationIsInHours) {
        int firstPartBase = durationIsInHours ?  HOURS_MIL : MINUTES_MIL;
        int firstPart = duration / firstPartBase;
        int leftoverFromFirstPart = duration - firstPart * firstPartBase;
        int secondPart = leftoverFromFirstPart / (durationIsInHours ? MINUTES_MIL : SECONDS_MIL);

        return String.format(Locale.getDefault(), "%02d:%02d", firstPart, secondPart);
    }

    /**
     * Converts long duration string (HH:MM:SS) to milliseconds.
     */
    public static int durationStringLongToMs(String input) {
        String[] parts = input.split(":");
        if (parts.length != 3) {
            return 0;
        }
        return Integer.parseInt(parts[0]) * 3600 * 1000
                + Integer.parseInt(parts[1]) * 60 * 1000
                + Integer.parseInt(parts[2]) * 1000;
    }

    /**
     * Converts short duration string (XX:YY) to milliseconds. If durationIsInHours is true then the
     * format is HH:MM, otherwise it's MM:SS.
     */
    public static int durationStringShortToMs(String input, boolean durationIsInHours) {
        String[] parts = input.split(":");
        if (parts.length != 2) {
            return 0;
        }

        int modifier = durationIsInHours ? 60 : 1;

        return Integer.parseInt(parts[0]) * 60 * 1000 * modifier
                + Integer.parseInt(parts[1]) * 1000 * modifier;
    }

    /**
     * Converts milliseconds to a localized string containing hours and minutes.
     */
    public static String getDurationStringLocalized(Context context, long duration) {
        return getDurationStringLocalized(context.getResources(), duration, false);
    }

    public static String getDurationStringLocalized(Resources resources, long duration, boolean includeDays) {
        String result = "";
        int h = (int) (duration / HOURS_MIL);
        int d = h / 24;
        if (!includeDays) {
            d = 0;
        } else if (d > 0) {
            String days = resources.getQuantityString(R.plurals.time_days_quantified, d, d);
            result += days.replace(" ", "\u00A0") + " ";
            h -= d * 24;
        }
        int rest = (int) (duration - ((long) d * 24 + h) * HOURS_MIL);
        int m = rest / MINUTES_MIL;
        if (h > 0) {
            String hours = resources.getQuantityString(R.plurals.time_hours_quantified, h, h);
            result += hours.replace(" ", "\u00A0");
            if (d == 0) {
                result += " ";
            }
        }
        if (d == 0) {
            String minutes = resources.getQuantityString(R.plurals.time_minutes_quantified, m, m);
            result += minutes.replace(" ", "\u00A0");
        }
        return result;
    }

    /**
     * Converts seconds to a localized representation.
     * @param time The time in seconds
     * @return "HH:MM hours"
     */
    public static String shortLocalizedDuration(Context context, long time) {
        float hours = (float) time / 3600f;
        return String.format(Locale.getDefault(), "%.1f ", hours) + context.getString(R.string.time_hours);
    }
}
