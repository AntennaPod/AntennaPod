package de.danoeh.antennapod.core.util;

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
    private static final int NUM_LENGTH = 1024;
    
    
    private static final int HOURS_MIL = 3600000;
	private static final int MINUTES_MIL = 60000;
	private static final int SECONDS_MIL = 1000;

    /** Takes a byte-value and converts it into a more readable
     *  String.
     *  @param input The value to convert
     *  @return The converted String with a unit
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
    
    /** Converts milliseconds to a string containing hours, minutes and seconds */
    public static String getDurationStringLong(int duration) {	
    	int h = duration / HOURS_MIL;
    	int rest = duration - h * HOURS_MIL;
    	int m = rest / MINUTES_MIL;
    	rest -= m * MINUTES_MIL;
    	int s = rest / SECONDS_MIL;
    	
    	return String.format("%02d:%02d:%02d", h, m, s);
    }
    
    /** Converts milliseconds to a string containing hours and minutes */
    public static String getDurationStringShort(int duration) {
    	int h = duration / HOURS_MIL;
    	int rest = duration - h * HOURS_MIL;
    	int m = rest / MINUTES_MIL;
    	
    	return String.format("%02d:%02d", h, m);
    }

    /** Converts long duration string (HH:MM:SS) to milliseconds. */
    public static int durationStringLongToMs(String input) {
        String[] parts = input.split(":");
        if (parts.length != 3) {
            return 0;
        }
        return Integer.valueOf(parts[0]) * 3600 * 1000 +
                Integer.valueOf(parts[1]) * 60 * 1000 +
                Integer.valueOf(parts[2]) * 1000;
    }

    /** Converts short duration string (HH:MM) to milliseconds. */
    public static int durationStringShortToMs(String input) {
        String[] parts = input.split(":");
        if (parts.length != 2) {
            return 0;
        }
        return Integer.valueOf(parts[0]) * 3600 * 1000 +
                Integer.valueOf(parts[1]) * 1000 * 60;
    }
    
}
