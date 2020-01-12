package de.danoeh.antennapod.core.util.comparator;

/**
 * Some compare() methods are not available before API 19.
 * This class provides fallbacks
 */
public class CompareCompat {

    private CompareCompat() {
        // Must not be instantiated
    }

    /**
     * Compares two {@code long} values. Long.compare() is not available before API 19
     *
     * @return 0 if long1 = long2, less than 0 if long1 &lt; long2,
     * and greater than 0 if long1 &gt; long2.
     */
    public static int compareLong(long long1, long long2) {
        //noinspection UseCompareMethod
        if (long1 > long2) {
            return -1;
        } else if (long1 < long2) {
            return 1;
        } else {
            return 0;
        }
    }
}
