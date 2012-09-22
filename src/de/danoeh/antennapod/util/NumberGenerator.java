package de.danoeh.antennapod.util;


/**Utility class for creating numbers.*/
public final class NumberGenerator {
    /** Class shall not be instantiated.*/
    private NumberGenerator() {
    }

    /**Logging tag.*/
    private static final String TAG = "NumberGenerator";

    /** Takes a string and generates a random value out of
     * the hash-value of that string.
     *  @param strSeed The string to take for the return value
     *  @return The generated random value
     * */
    public static long generateLong(final String strSeed) {
        return strSeed.hashCode();
    }
}
