package de.podfetcher.syndication.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

	public static Date parseRFC822Date(final String date) {
		Date result = null;
		SimpleDateFormat format = new SimpleDateFormat(RFC822DAY);
		try {
			result = format.parse(date);
		} catch (ParseException e) {
			format = new SimpleDateFormat(RFC822);
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
		SimpleDateFormat format = null;
		if (date.endsWith("Z")) {
			format = new SimpleDateFormat(RFC3339UTC);
			try {
				result = format.parse(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			format = new SimpleDateFormat(RFC3339LOCAL);
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
