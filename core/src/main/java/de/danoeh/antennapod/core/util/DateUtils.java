package de.danoeh.antennapod.core.util;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Parses several date formats.
 */
public class DateUtils {
    
	private static final String TAG = "DateUtils";

    public static Date parse(final String input) {
        if(input == null) {
            throw new IllegalArgumentException("Date most not be null");
        }
        String date = input.replace('/', '-');
        if(date.contains(".")) {
            int start = date.indexOf('.');
            int current = start+1;
            while(current < date.length() && Character.isDigit(date.charAt(current))) {
                current++;
            }
            if(current - start > 4) {
                if(current < date.length()-1) {
                    date = date.substring(0, start + 4) + date.substring(current);
                } else {
                    date = date.substring(0, start + 4);
                }
            } else if(current - start < 4) {
                if(current < date.length()-1) {
                    date = date.substring(0, current) + StringUtils.repeat("0", 4-(current-start)) + date.substring(current);
                } else {
                    date = date.substring(0, current) + StringUtils.repeat("0", 4-(current-start));
                }

            }
        }
        String[] patterns = {
                "dd MMM yy HH:mm:ss Z",
                "dd MMM yy HH:mm Z",
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, dd MMMM yyyy HH:mm:ss Z",
                "EEEE, dd MMM yy HH:mm:ss Z",
                "EEE MMM d HH:mm:ss yyyy",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSS Z",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-ddZ",
                "yyyy-MM-dd"
        };
        SimpleDateFormat parser = new SimpleDateFormat("", Locale.US);
        parser.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        for(String pattern : patterns) {
            parser.applyPattern(pattern);
            pos.setIndex(0);
            Date result = parser.parse(date, pos);
            if(result != null && pos.getIndex() == date.length()) {
                return result;
            }
        }

        Log.d(TAG, "Could not parse date string \"" + input + "\"");
        return null;
    }


    /**
     * Takes a string of the form [HH:]MM:SS[.mmm] and converts it to
     * milliseconds.
     *
     * @throws java.lang.NumberFormatException if the number segments contain invalid numbers.
     */
    public static long parseTimeString(final String time) {
        String[] parts = time.split(":");
        long result = 0;
        int idx = 0;
        if (parts.length == 3) {
            // string has hours
            result += Integer.valueOf(parts[idx]) * 3600000L;
            idx++;
        }
        if (parts.length >= 2) {
            result += Integer.valueOf(parts[idx]) * 60000L;
            idx++;
            result += (Float.valueOf(parts[idx])) * 1000L;
        }
        return result;
    }

    public static String formatRFC822Date(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yy HH:mm:ss Z", Locale.US);
        return format.format(date);
    }

    public static String formatRFC3339Local(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        return format.format(date);
    }

    public static String formatRFC3339UTC(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        return format.format(date);
    }
}
