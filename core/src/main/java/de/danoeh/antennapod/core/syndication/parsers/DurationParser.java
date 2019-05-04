package de.danoeh.antennapod.core.syndication.parsers;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class DurationParser {
    public static long inMillis(String durationStr) throws NumberFormatException {
        String[] parts = durationStr.trim().split(":");

        if (parts.length == 1) {
            return toMillis(parts[0]);
        } else if (parts.length == 2) {
            return toMillis("0", parts[0], parts[1]);
        } else if (parts.length == 3) {
            return toMillis(parts[0], parts[1], parts[2]);
        } else {
            throw new NumberFormatException();
        }
    }

    private static long toMillis(String hours, String minutes, String seconds) {
        return HOURS.toMillis(Long.parseLong(hours))
                + MINUTES.toMillis(Long.parseLong(minutes))
                + toMillis(seconds);
    }

    private static long toMillis(String seconds) {
        if (seconds.contains(".")) {
            float value = Float.parseFloat(seconds);
            float millis = value % 1;
            return SECONDS.toMillis((long) value) + (long) (millis * 1000);
        } else {
            return SECONDS.toMillis(Long.parseLong(seconds));
        }
    }
}
