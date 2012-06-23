package de.podfetcher.syndication.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

/** Parses several date formats. */
public class SyndDateUtils {
	private static final String TAG = "DateUtils";
	public static final String RFC822 = "dd MMM yyyy HH:mm:ss Z";
	/** RFC 822 date format with day of the week. */
	public static final String RFC822DAY = "EEE, " + RFC822;

	/** RFC 3339 date format for UTC dates. */
	public static final String RFC3339UTC = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	/** RFC 3339 date format for localtime dates with offset. */
	public static final String RFC3339LOCAL = "yyyy-MM-dd'T'HH:mm:ssZ";
	
	private static ThreadLocal<SimpleDateFormat> RFC822Formatter = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {			
			return new SimpleDateFormat(RFC822DAY, Locale.US);
		}
		
	};
	
	private static ThreadLocal<SimpleDateFormat> RFC3339Formatter = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {			
			return new SimpleDateFormat(RFC3339UTC, Locale.US);
		}
		
	};

	public static Date parseRFC822Date(final String date) {
		Date result = null;
		SimpleDateFormat format = RFC822Formatter.get();
		try {
			result = format.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
			format.applyPattern(RFC822);
			try {
				result = format.parse(date);
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
		}

		return result;
	}

	public static Date parseRFC3339Date(final String date) {
		Date result = null;
		SimpleDateFormat format = RFC3339Formatter.get();
		if (date.endsWith("Z")) {
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
			}

		}

		return result;

	}
}
