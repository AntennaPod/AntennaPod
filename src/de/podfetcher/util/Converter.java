package de.podfetcher.util;

import android.util.Log;

/** Provides methods for converting various units. */
public final class Converter {
	/** Class shall not be instantiated. */
	private Converter() {
	}

	/** Logging tag. */
	private static final String TAG = "Converter";


	/** Indicates that the value is in the Byte range.*/
	private static final int B_RANGE = 0;
	/** Indicates that the value is in the Kilobyte range.*/
	private static final int KB_RANGE = 1;
	/** Indicates that the value is in the Megabyte range.*/
	private static final int MB_RANGE = 2;
	/** Indicates that the value is in the Gigabyte range.*/
	private static final int GB_RANGE = 3;
	/** Determines the length of the number for best readability.*/
	private static final int NUM_LENGTH = 1000;

	/** Takes a byte-value and converts it into a more readable
	 *	String.
	 *	@param input The value to convert
	 *	@return The converted String with a unit
	 * */
	public static String byteToString(final long input) {
		int i = 0;
		int result = 0;

		for (i = 0; i < GB_RANGE + 1; i++) {
			result = (int) (input / Math.pow(1024, i));
			if (result < NUM_LENGTH) {
				break;
			}
		}

		switch (i) {
			case B_RANGE:
				return result + " B";
			case KB_RANGE:
				return result + " KB";
			case MB_RANGE:
				return result + " MB";
			case GB_RANGE:
				return result + " GB";
			default:
				Log.e(TAG, "Error happened in byteToString");
				return "ERROR";
		}
	}
}
