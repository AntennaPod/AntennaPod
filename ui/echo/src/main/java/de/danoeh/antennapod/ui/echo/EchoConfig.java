package de.danoeh.antennapod.ui.echo;

import java.util.Calendar;

public class EchoConfig {
    public static final int RELEASE_YEAR = 2024;

    public static long jan1() {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.set(Calendar.DAY_OF_MONTH, 1);
        date.set(Calendar.MONTH, 0);
        date.set(Calendar.YEAR, RELEASE_YEAR);
        return date.getTimeInMillis();
    }

    public static boolean isCurrentlyVisible() {
        return Calendar.getInstance().get(Calendar.YEAR) == RELEASE_YEAR
                && Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER
                && Calendar.getInstance().get(Calendar.DAY_OF_MONTH) >= 10;
    }
}
