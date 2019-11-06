package de.danoeh.antennapod.core.util;

/**
 * Utility functions for handling Arrays.
 */
public class ArrayUtils {

    private ArrayUtils() {

    }

    public static <T> boolean contains(T[] array, T searchItem) {
        return indexOf(array, searchItem) != -1;
    }

    public static <T> int indexOf(T[] array, T searchItem) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(searchItem)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean contains(long[] array, long searchItem) {
        return indexOf(array, searchItem) != -1;
    }

    public static int indexOf(long[] array, long searchItem) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == searchItem) {
                return i;
            }
        }
        return -1;
    }
}
