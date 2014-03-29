package de.danoeh.antennapod.syndication.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Parses several date formats. */
public class SyndDateUtils {
	private static final String TAG = "DateUtils";

	private static final String[] RFC822DATES = { "dd MMM yy HH:mm:ss Z", };

	/** RFC 3339 date format for UTC dates. */
	public static final String RFC3339UTC = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	/** RFC 3339 date format for localtime dates with offset. */
	public static final String RFC3339LOCAL = "yyyy-MM-dd'T'HH:mm:ssZ";

	private static ThreadLocal<SimpleDateFormat> RFC822Formatter = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat(RFC822DATES[0], Locale.US);
		}

	};

	private static ThreadLocal<SimpleDateFormat> RFC3339Formatter = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat(RFC3339UTC, Locale.US);
		}

	};

	public static Date parseRFC822Date(String date) {
		Date result = null;
		if (date.contains("PDT")) {
			date = date.replace("PDT", "PST8PDT");
		}
		if (date.contains(",")) {
			// Remove day of the week
			date = date.substring(date.indexOf(",") + 1).trim();
		}
		SimpleDateFormat format = RFC822Formatter.get();
		for (int i = 0; i < RFC822DATES.length; i++) {
			try {
				format.applyPattern(RFC822DATES[i]);
				result = format.parse(date);
				break;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (result == null) {
			Log.e(TAG, "Unable to parse feed date correctly");
		}

		return result;
	}

	public static Date parseRFC3339Date(String date) {
		Date result = null;
		SimpleDateFormat format = RFC3339Formatter.get();
		boolean isLocal = date.endsWith("Z");
		if (date.contains(".")) {
			// remove secfrac
			int fracIndex = date.indexOf(".");
			String first = date.substring(0, fracIndex);
			String second = null;
			if (isLocal) {
				second = date.substring(date.length() - 1);
			} else {
				if (date.contains("+")) {
					second = date.substring(date.indexOf("+"));
				} else {
					second = date.substring(date.indexOf("-"));
				}
			}
			
			date = first + second;
		}
		if (isLocal) {
			try {
				result = format.parse(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			format.applyPattern(RFC3339LOCAL);
			// remove last colon
			StringBuffer buf = new StringBuffer(date.length() - 1);
			int colonIdx = date.lastIndexOf(':');
			for (int x = 0; x < date.length(); x++) {
				if (x != colonIdx)
					buf.append(date.charAt(x));
			}
			String bufStr = buf.toString();
			try {
				result = format.parse(bufStr);
			} catch (ParseException e) {
				e.printStackTrace();
				Log.e(TAG, "Unable to parse date");
			} finally {
				format.applyPattern(RFC3339UTC);
			}

		}

		return result;

	}

	/**
	 * Takes a string of the form [HH:]MM:SS[.mmm] and converts it to
	 * milliseconds.
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
		result += Integer.valueOf(parts[idx]) * 60000L;
		idx++;
		result += (Float.valueOf(parts[idx])) * 1000L;
		return result;
	}
}
