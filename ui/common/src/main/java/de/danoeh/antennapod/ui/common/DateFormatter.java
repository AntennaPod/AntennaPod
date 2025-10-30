package de.danoeh.antennapod.ui.common;

import android.content.Context;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Formats dates.
 */
public class DateFormatter {
    private DateFormatter() {

    }

    public static String formatAbbrev(final Context context, final Date date) {
        if (date == null) {
            return "";
        }
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        boolean withinLastYear = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR);
        int format = android.text.format.DateUtils.FORMAT_ABBREV_ALL;
        if (withinLastYear) {
            format |= android.text.format.DateUtils.FORMAT_NO_YEAR;
        }
        return android.text.format.DateUtils.formatDateTime(context, date.getTime(), format);
    }

    public static String formatForAccessibility(final Date date) {
        if (date == null) {
            return "";
        }
        return DateFormat.getDateInstance(DateFormat.LONG).format(date);
    }
}
