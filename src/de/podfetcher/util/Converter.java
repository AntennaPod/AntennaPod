package de.podfetcher.util;

import android.util.Log;

/** Provides methods for converting various units */
public class Converter {
	private static final String TAG = "Converter";

	public static String byteToString(long input) {
		int i = 0;
		int result = 0;

		for(i = 0; i < 4; i++) {
			result = (int) (input / Math.pow(1024, i));
			if(result < 1000) {
				break;
			}
		}

		switch(i) {
			case 0:
				return result + " B";
			case 1:
				return result + " KB";
			case 2:
				return result + " MB";
			case 3:
				return result + " GB";
			default:
				Log.e(TAG, "Error happened in byteToString");
				return "ERROR";
		}
	}
}
